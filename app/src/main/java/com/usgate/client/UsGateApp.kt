package com.usgate.client

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.usgate.client.data.PrefsStore
import go.Seq
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.SetupOptions
import java.io.File

class UsGateApp : Application() {
    lateinit var prefs: PrefsStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = PrefsStore(this)
        createVpnChannel()
        setupLibbox()
    }

    private fun setupLibbox() {
        try {
            Seq.setContext(this)
            val baseDir = filesDir.apply { mkdirs() }
            val workingDir = (getExternalFilesDir(null) ?: File(filesDir, "sing-box")).apply { mkdirs() }
            val tempDir = cacheDir.apply { mkdirs() }
            Libbox.setup(SetupOptions().also {
                it.basePath = baseDir.path
                it.workingPath = workingDir.path
                it.tempPath = tempDir.path
                it.fixAndroidStack = true
                it.logMaxLines = 3000
                it.debug = true
            })
            Log.i(TAG, "libbox ready: ${Libbox.version()}")
        } catch (e: Throwable) {
            Log.w(TAG, "libbox setup skipped/failed: ${e.message}")
        }
    }

    private fun createVpnChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            VPN_CHANNEL_ID,
            getString(R.string.vpn_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "UsGateApp"
        const val VPN_CHANNEL_ID = "usgate_vpn"
        lateinit var instance: UsGateApp
            private set
    }
}
