package com.usgate.client.ui

import androidx.annotation.StringRes
import com.usgate.client.R

/**
 * Home-screen connection chrome (status label + primary button).
 * Pure mapping — no VpnService / TUN — so JVM unit tests can cover
 * disconnected → connecting → connected / error without VPN permission.
 */
enum class ConnectionUiState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR;

    @StringRes
    fun statusLabelRes(): Int = when (this) {
        DISCONNECTED -> R.string.status_disconnected
        CONNECTING -> R.string.status_connecting
        CONNECTED -> R.string.status_connected
        ERROR -> R.string.status_error
    }

    @StringRes
    fun primaryButtonRes(): Int = when (this) {
        DISCONNECTED, ERROR -> R.string.btn_connect
        CONNECTING, CONNECTED -> R.string.btn_disconnect
    }

    companion object {
        /** Map fragment flags to a single state (connecting wins over connected). */
        fun fromFlags(connected: Boolean, connecting: Boolean, error: Boolean = false): ConnectionUiState =
            when {
                error -> ERROR
                connecting -> CONNECTING
                connected -> CONNECTED
                else -> DISCONNECTED
            }
    }
}
