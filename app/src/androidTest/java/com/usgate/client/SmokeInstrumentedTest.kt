package com.usgate.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.usgate.client.subscription.SubscriptionParser
import com.usgate.client.ui.ConnectionUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lightweight instrumentation smoke.
 * Does NOT attempt VpnService.connect / exit-IP / traffic — those stay BLOCKED on CI.
 */
@RunWith(AndroidJUnit4::class)
class SmokeInstrumentedTest {

    @Test
    fun packageNameAndDisconnectedLabel() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.usgate.client", ctx.packageName)
        assertEquals(
            "未连接",
            ctx.getString(ConnectionUiState.DISCONNECTED.statusLabelRes())
        )
    }

    @Test
    fun placeholderSubscriptionParsesOnDevice() {
        val link =
            "vless://00000000-0000-0000-0000-000000000000@example.com:443" +
                "?encryption=none&security=tls&type=ws&path=%2Fplaceholder#US-Placeholder-VLESS"
        val nodes = SubscriptionParser.parse(link)
        assertEquals(1, nodes.size)
        assertTrue(nodes[0].host == "example.com")
    }
}
