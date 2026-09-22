package com.usgate.client.vpn

import android.os.ParcelFileDescriptor
import android.util.Log
import com.usgate.client.subscription.ProxyNode
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.SystemProxyStatus
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Bridge between [UsGateVpnService] and the proxy core (libbox / scaffold).
 */
interface VpnCoreBridge {
    /**
     * Start the core. Libbox path creates TUN via [PlatformInterface.openTun];
     * scaffold path expects the service to have already established a TUN (see [ScaffoldVpnCore]).
     */
    fun startCore(service: UsGateVpnService, node: ProxyNode)
    fun stopCore()
    fun isRunning(): Boolean
}

object VpnCoreFactory {
    fun create(): VpnCoreBridge {
        return try {
            Class.forName("io.nekohasekai.libbox.Libbox")
            LibboxVpnCore()
        } catch (_: ClassNotFoundException) {
            Log.w("UsGateCore", "libbox AAR not on classpath — using scaffold")
            ScaffoldVpnCore()
        }
    }
}

/**
 * Real core: sing-box via official gomobile libbox AAR (downloaded at build time).
 */
class LibboxVpnCore : VpnCoreBridge, CommandServerHandler {
    @Volatile private var running = false
    private var commandServer: CommandServer? = null
    private var platform: LibboxPlatformInterface? = null
    private var serviceRef: UsGateVpnService? = null
    private val started = AtomicBoolean(false)

    override fun startCore(service: UsGateVpnService, node: ProxyNode) {
        if (!started.compareAndSet(false, true)) {
            Log.w(TAG, "already started")
            return
        }
        serviceRef = service
        val config = SingBoxConfigBuilder.build(node)
        Log.i(TAG, "libbox ${Libbox.version()} starting for ${node.protocol} ${node.host}:${node.port}")
        // Persist config for debugging (no secrets committed to git; local only)
        runCatching {
            File(service.filesDir, "sing-box-last.json").writeText(config)
        }

        DefaultNetworkMonitor.start(service)
        val platformIface = LibboxPlatformInterface(service)
        platform = platformIface

        try {
            Libbox.checkConfig(config)
        } catch (e: Exception) {
            Log.e(TAG, "config check failed: ${e.message}")
            // Still attempt start — some drafts warn only
        }

        val server = CommandServer(this, platformIface)
        commandServer = server
        server.start()
        server.startOrReloadService(config, OverrideOptions().apply {
            // Exclude our own package from TUN so control channel / notification traffic is fine;
            // tunnel sockets are still protect()'d via PlatformInterface.
            excludePackage = StringArray(listOf(service.packageName))
        })
        running = true
        Log.i(TAG, "libbox service started")
    }

    override fun stopCore() {
        running = false
        try {
            commandServer?.closeService()
        } catch (e: Exception) {
            Log.w(TAG, "closeService: ${e.message}")
        }
        try {
            commandServer?.close()
        } catch (e: Exception) {
            Log.w(TAG, "commandServer.close: ${e.message}")
        }
        commandServer = null
        platform = null
        serviceRef?.let { DefaultNetworkMonitor.stop(it) }
        serviceRef = null
        started.set(false)
        Log.i(TAG, "libbox stopped")
    }

    override fun isRunning(): Boolean = running

    // --- CommandServerHandler ---

    override fun serviceStop() {
        Log.i(TAG, "serviceStop from libbox")
        serviceRef?.requestStopFromCore()
    }

    override fun serviceReload() {
        Log.i(TAG, "serviceReload (no-op)")
    }

    override fun getSystemProxyStatus(): SystemProxyStatus {
        return SystemProxyStatus().apply {
            available = false
            enabled = false
        }
    }

    override fun setSystemProxyEnabled(isEnabled: Boolean) {}

    override fun writeDebugMessage(message: String?) {
        Log.d(TAG, message ?: "")
    }

    companion object {
        private const val TAG = "UsGateLibbox"
    }
}

/**
 * Placeholder core: holds a pre-established TUN open so system VPN status flips to connected.
 * Used only when libbox AAR is absent.
 */
class ScaffoldVpnCore : VpnCoreBridge {
    @Volatile private var running = false
    private var tunFd: ParcelFileDescriptor? = null

    override fun startCore(service: UsGateVpnService, node: ProxyNode) {
        Log.i(TAG, "Scaffold core start for ${node.protocol} ${node.host}:${node.port} (no real tunnel)")
        // Service establishes TUN for scaffold mode before calling us — grab it.
        tunFd = service.detachScaffoldTun()
        running = true
    }

    override fun stopCore() {
        running = false
        try {
            tunFd?.close()
        } catch (_: Exception) {
        }
        tunFd = null
        Log.i(TAG, "Scaffold core stopped")
    }

    override fun isRunning(): Boolean = running

    companion object {
        private const val TAG = "UsGateCore"
    }
}
