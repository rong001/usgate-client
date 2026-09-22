package com.usgate.client.vpn

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.system.OsConstants
import android.util.Log
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.Notification
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import io.nekohasekai.libbox.NetworkInterface as LibboxNetworkInterface
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.net.NetworkInterface as JavaNetworkInterface

/** Tiny StringIterator for libbox. */
class StringArray(private val values: List<String>) : StringIterator {
    private val it = values.iterator()
    override fun len(): Int = values.size
    override fun hasNext(): Boolean = it.hasNext()
    override fun next(): String = it.next()
}

class NetworkInterfaceArray(private val values: List<LibboxNetworkInterface>) : NetworkInterfaceIterator {
    private val it = values.iterator()
    override fun hasNext(): Boolean = it.hasNext()
    override fun next(): LibboxNetworkInterface = it.next()
}

/**
 * Minimal DefaultNetworkMonitor so libbox can learn the underlying interface
 * for [auto_detect_interface] / protect().
 */
object DefaultNetworkMonitor {
    private const val TAG = "UsGateNetMon"
    @Volatile var defaultNetwork: Network? = null
    private var listener: InterfaceUpdateListener? = null
    private var registered = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            defaultNetwork = network
            notifyListener(network)
        }

        override fun onLost(network: Network) {
            if (defaultNetwork == network) {
                defaultNetwork = null
                notifyListener(null)
            }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            defaultNetwork = network
            notifyListener(network)
        }
    }

    fun start(context: Context) {
        if (registered) return
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            cm.registerNetworkCallback(req, callback)
            registered = true
            defaultNetwork = cm.activeNetwork
            notifyListener(defaultNetwork)
        } catch (e: Exception) {
            Log.w(TAG, "registerNetworkCallback failed", e)
        }
    }

    fun stop(context: Context) {
        if (!registered) return
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        try {
            cm.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
        registered = false
        listener = null
        defaultNetwork = null
    }

    fun setListener(l: InterfaceUpdateListener?) {
        listener = l
        notifyListener(defaultNetwork)
    }

    private fun notifyListener(network: Network?) {
        val l = listener ?: return
        mainHandler.post {
            try {
                if (network == null) {
                    l.updateDefaultInterface("", -1, false, false)
                    return@post
                }
                // ConnectivityManager needs a context — use UsGateApp
                val app = com.usgate.client.UsGateApp.instance
                val cm = app.getSystemService(ConnectivityManager::class.java) ?: return@post
                val lp = cm.getLinkProperties(network) ?: return@post
                val name = lp.interfaceName ?: return@post
                val index = try {
                    JavaNetworkInterface.getByName(name)?.index ?: -1
                } catch (_: Exception) {
                    -1
                }
                l.updateDefaultInterface(name, index, false, false)
            } catch (e: Exception) {
                Log.w(TAG, "notifyListener", e)
            }
        }
    }
}

/**
 * PlatformInterface for libbox 1.13.x — protect() + openTun via [UsGateVpnService].
 */
class LibboxPlatformInterface(
    private val service: UsGateVpnService
) : PlatformInterface {

    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true

    override fun autoDetectInterfaceControl(fd: Int) {
        service.protect(fd)
    }

    override fun openTun(options: TunOptions): Int {
        return service.openTunFromLibbox(options)
    }

    override fun useProcFS(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String?,
        sourcePort: Int,
        destinationAddress: String?,
        destinationPort: Int
    ): ConnectionOwner {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            error("android: findConnectionOwner requires API 29+")
        }
        val cm = service.getSystemService(ConnectivityManager::class.java)
            ?: error("no ConnectivityManager")
        val uid = cm.getConnectionOwnerUid(
            ipProtocol,
            InetSocketAddress(sourceAddress, sourcePort),
            InetSocketAddress(destinationAddress, destinationPort)
        )
        if (uid == Process.INVALID_UID) error("android: connection owner not found")
        val packages = service.packageManager.getPackagesForUid(uid)
        return ConnectionOwner().apply {
            userId = uid
            userName = packages?.firstOrNull().orEmpty()
            setAndroidPackageNames(StringArray(packages?.toList().orEmpty()))
        }
    }

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {
        DefaultNetworkMonitor.setListener(listener)
    }

    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener?) {
        DefaultNetworkMonitor.setListener(null)
    }

    override fun getInterfaces(): NetworkInterfaceIterator {
        val result = mutableListOf<LibboxNetworkInterface>()
        val cm = service.getSystemService(ConnectivityManager::class.java)
        val javaIfaces = JavaNetworkInterface.getNetworkInterfaces()?.toList().orEmpty()

        if (cm != null) {
            for (network in cm.allNetworks) {
                val lp = cm.getLinkProperties(network) ?: continue
                val caps = cm.getNetworkCapabilities(network) ?: continue
                val name = lp.interfaceName ?: continue
                val jni = javaIfaces.find { it.name == name } ?: continue
                val box = LibboxNetworkInterface()
                box.name = name
                box.index = jni.index
                runCatching { box.mtu = jni.mtu }
                box.dnsServer = StringArray(lp.dnsServers.mapNotNull { it.hostAddress })
                box.type = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Libbox.InterfaceTypeWIFI
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Libbox.InterfaceTypeCellular
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Libbox.InterfaceTypeEthernet
                    else -> Libbox.InterfaceTypeOther
                }
                box.addresses = StringArray(
                    jni.interfaceAddresses.map { ia ->
                        val host = ia.address.hostAddress ?: return@map null
                        val cleaned = if (ia.address is Inet6Address) host.substringBefore('%') else host
                        "$cleaned/${ia.networkPrefixLength}"
                    }.filterNotNull()
                )
                var flags = 0
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    flags = OsConstants.IFF_UP or OsConstants.IFF_RUNNING
                }
                if (jni.isLoopback) flags = flags or OsConstants.IFF_LOOPBACK
                if (jni.isPointToPoint) flags = flags or OsConstants.IFF_POINTOPOINT
                if (jni.supportsMulticast()) flags = flags or OsConstants.IFF_MULTICAST
                box.flags = flags
                box.metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                result.add(box)
            }
        }

        if (result.isEmpty()) {
            for (jni in javaIfaces) {
                val box = LibboxNetworkInterface()
                box.name = jni.name
                box.index = jni.index
                runCatching { box.mtu = jni.mtu }
                box.type = Libbox.InterfaceTypeOther
                box.addresses = StringArray(
                    jni.interfaceAddresses.mapNotNull { ia ->
                        val host = ia.address.hostAddress ?: return@mapNotNull null
                        val cleaned = if (ia.address is Inet6Address) host.substringBefore('%') else host
                        "$cleaned/${ia.networkPrefixLength}"
                    }
                )
                result.add(box)
            }
        }
        return NetworkInterfaceArray(result)
    }

    override fun underNetworkExtension(): Boolean = false
    override fun includeAllNetworks(): Boolean = false
    override fun clearDNSCache() {}
    override fun readWIFIState(): WIFIState? = null
    override fun localDNSTransport(): LocalDNSTransport? = null
    override fun systemCertificates(): StringIterator = StringArray(emptyList())
    override fun sendNotification(notification: Notification?) {}
}
