package com.usgate.client.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.usgate.client.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.app.Application
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ConnectionUiStateTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun disconnectedToConnectingMock() {
        val start = ConnectionUiState.fromFlags(connected = false, connecting = false)
        assertEquals(ConnectionUiState.DISCONNECTED, start)
        assertEquals(R.string.status_disconnected, start.statusLabelRes())
        assertEquals(R.string.btn_connect, start.primaryButtonRes())

        val mid = ConnectionUiState.fromFlags(connected = false, connecting = true)
        assertEquals(ConnectionUiState.CONNECTING, mid)
        assertEquals(R.string.status_connecting, mid.statusLabelRes())
        assertEquals(R.string.btn_disconnect, mid.primaryButtonRes())

        // Connecting wins even if connected flag accidentally true
        val preferConnecting = ConnectionUiState.fromFlags(connected = true, connecting = true)
        assertEquals(ConnectionUiState.CONNECTING, preferConnecting)
    }

    @Test
    fun statusAndErrorStringsMatchResources() {
        assertEquals("未连接", context.getString(ConnectionUiState.DISCONNECTED.statusLabelRes()))
        assertEquals("连接中…", context.getString(ConnectionUiState.CONNECTING.statusLabelRes()))
        assertEquals("已连接", context.getString(ConnectionUiState.CONNECTED.statusLabelRes()))
        assertEquals("出错", context.getString(ConnectionUiState.ERROR.statusLabelRes()))

        assertEquals("需要 VPN 授权才能连接", context.getString(R.string.vpn_permission_required))
        assertEquals("请先选择节点", context.getString(R.string.need_node))
        assertEquals("请先填写订阅地址或粘贴分享链接", context.getString(R.string.subscription_required))
        assertEquals("导入失败：%1\$s", context.getString(R.string.import_fail))
    }

    @Test
    fun errorStateUsesConnectButton() {
        val err = ConnectionUiState.fromFlags(connected = false, connecting = false, error = true)
        assertEquals(ConnectionUiState.ERROR, err)
        assertEquals(R.string.btn_connect, err.primaryButtonRes())
    }
}
