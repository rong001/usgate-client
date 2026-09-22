package com.usgate.client.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

object VpnController {
    fun prepareIntent(context: Context): Intent? = VpnService.prepare(context)

    fun start(context: Context) {
        val intent = Intent(context, UsGateVpnService::class.java).apply {
            action = UsGateVpnService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, UsGateVpnService::class.java).apply {
            action = UsGateVpnService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun ensurePermissionThenStart(
        activity: Activity,
        permissionLauncher: ActivityResultLauncher<Intent>,
        onAlreadyGranted: () -> Unit
    ) {
        val prepare = prepareIntent(activity)
        if (prepare != null) {
            permissionLauncher.launch(prepare)
        } else {
            onAlreadyGranted()
            start(activity)
        }
    }
}
