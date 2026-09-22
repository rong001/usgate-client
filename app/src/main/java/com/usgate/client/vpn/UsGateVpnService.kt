package com.usgate.client.vpn

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.usgate.client.R
import com.usgate.client.UsGateApp
import com.usgate.client.ui.MainActivity
import io.nekohasekai.libbox.TunOptions
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android [VpnService] entry. Delegates packet forwarding to [VpnCoreBridge]
 * (libbox when AAR present, otherwise scaffold).
 */
class UsGateVpnService : VpnService() {

    private var tunInterface: ParcelFileDescriptor? = null
    private val core: VpnCoreBridge = VpnCoreFactory.create()
    private val started = AtomicBoolean(false)
    private var scaffoldTun: ParcelFileDescriptor? = null
    private val usingLibbox: Boolean = core is LibboxVpnCore

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                return START_NOT_STICKY
            }
            ACTION_START, null -> startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (!started.compareAndSet(false, true)) return

        val node = UsGateApp.instance.prefs.selectedNode()
        if (node == null) {
            Log.w(TAG, "No node selected")
            started.set(false)
            stopSelf()
            return
        }

        startForegroundNotification()
        broadcastState(STATE_CONNECTING)

        Thread({
            try {
                if (usingLibbox) {
                    // TUN is created later inside PlatformInterface.openTun()
                    core.startCore(this, node)
                } else {
                    val tun = establishScaffoldTun()
                    if (tun == null) {
                        Log.e(TAG, "User rejected VPN or establish returned null")
                        started.set(false)
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@Thread
                    }
                    scaffoldTun = tun
                    tunInterface = tun
                    core.startCore(this, node)
                }
                broadcastState(STATE_CONNECTED)
                Log.i(TAG, "VPN up for ${node.name} (libbox=$usingLibbox)")
            } catch (e: Exception) {
                Log.e(TAG, "startVpn failed", e)
                stopVpn()
            }
        }, "usgate-core-start").start()
    }

    private fun establishScaffoldTun(): ParcelFileDescriptor? {
        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = Builder()
            .setSession("USGate")
            .setMtu(1500)
            .addAddress(TUN_ADDRESS, 32)
            .addDnsServer(DNS_PLACEHOLDER)
            .addRoute("0.0.0.0", 0)
            .setConfigureIntent(pi)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }
        return try {
            builder.establish()
        } catch (e: Exception) {
            Log.e(TAG, "establish failed", e)
            null
        }
    }

    /** Called by [ScaffoldVpnCore] to take ownership of the pre-established TUN. */
    fun detachScaffoldTun(): ParcelFileDescriptor? {
        val fd = scaffoldTun
        scaffoldTun = null
        return fd
    }

    /**
     * Called by libbox via [LibboxPlatformInterface.openTun].
     * Builds the system TUN from [TunOptions] and returns the raw fd.
     */
    fun openTunFromLibbox(options: TunOptions): Int {
        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = Builder()
            .setSession("USGate")
            .setMtu(options.mtu)
            .setConfigureIntent(pi)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        val inet4 = options.inet4Address
        while (inet4.hasNext()) {
            val prefix = inet4.next()
            builder.addAddress(prefix.address(), prefix.prefix())
        }
        val inet6 = options.inet6Address
        while (inet6.hasNext()) {
            val prefix = inet6.next()
            builder.addAddress(prefix.address(), prefix.prefix())
        }

        if (options.autoRoute) {
            try {
                val dns = options.dnsServerAddress
                val value = dns?.value
                if (!value.isNullOrBlank()) {
                    builder.addDnsServer(value)
                } else {
                    builder.addDnsServer(DNS_PLACEHOLDER)
                }
            } catch (_: Exception) {
                builder.addDnsServer(DNS_PLACEHOLDER)
            }

            // Prefer explicit route ranges when provided; otherwise full tunnel.
            var addedRoute = false
            val v4range = options.inet4RouteRange
            while (v4range.hasNext()) {
                val p = v4range.next()
                builder.addRoute(p.address(), p.prefix())
                addedRoute = true
            }
            val v6range = options.inet6RouteRange
            while (v6range.hasNext()) {
                val p = v6range.next()
                builder.addRoute(p.address(), p.prefix())
                addedRoute = true
            }
            if (!addedRoute) {
                builder.addRoute("0.0.0.0", 0)
                // IPv6 full route only if we have an inet6 address
            }
        }

        // Always exclude ourselves when libbox didn't already set exclude list
        try {
            builder.addDisallowedApplication(packageName)
        } catch (_: Exception) {
        }

        val exclude = options.excludePackage
        while (exclude.hasNext()) {
            try {
                builder.addDisallowedApplication(exclude.next())
            } catch (_: Exception) {
            }
        }
        val include = options.includePackage
        var hasInclude = false
        while (include.hasNext()) {
            hasInclude = true
            try {
                builder.addAllowedApplication(include.next())
            } catch (_: Exception) {
            }
        }
        // If include list is used, disallowedApplication is ignored by Android — OK.

        val pfd = builder.establish()
            ?: error("android: VPN establish returned null (permission revoked?)")
        tunInterface = pfd
        Log.i(TAG, "openTunFromLibbox fd=${pfd.fd} include=$hasInclude")
        return pfd.fd
    }

    fun requestStopFromCore() {
        stopVpn()
    }

    private fun stopVpn() {
        try {
            core.stopCore()
        } catch (e: Exception) {
            Log.w(TAG, "core.stopCore", e)
        }
        try {
            tunInterface?.close()
        } catch (_: Exception) {
        }
        tunInterface = null
        scaffoldTun = null
        started.set(false)
        broadcastState(STATE_DISCONNECTED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundNotification() {
        val open = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, UsGateApp.VPN_CHANNEL_ID)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text))
            .setSmallIcon(R.drawable.ic_stat_vpn)
            .setColor(getColor(R.color.usgate_primary))
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)
    }

    private fun broadcastState(state: String) {
        sendBroadcast(Intent(ACTION_STATE).setPackage(packageName).putExtra(EXTRA_STATE, state))
    }

    override fun onDestroy() {
        if (started.get()) stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    companion object {
        private const val TAG = "UsGateVpn"
        private const val NOTIF_ID = 1001

        private const val TUN_ADDRESS = "10.8.0.2"
        private const val DNS_PLACEHOLDER = "1.1.1.1"

        const val ACTION_START = "com.usgate.client.vpn.START"
        const val ACTION_STOP = "com.usgate.client.vpn.STOP"
        const val ACTION_STATE = "com.usgate.client.vpn.STATE"
        const val EXTRA_STATE = "state"
        const val STATE_CONNECTED = "connected"
        const val STATE_DISCONNECTED = "disconnected"
        const val STATE_CONNECTING = "connecting"
    }
}
