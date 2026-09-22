package com.usgate.client.vpn

import com.usgate.client.subscription.ProxyNode
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SingBoxConfigBuilderTest {

    @Test
    fun buildsVlessRealityPlaceholderConfig() {
        val node = ProxyNode(
            id = "placeholder-id",
            name = "Reality-Placeholder",
            protocol = "vless",
            host = "example.com",
            port = 443,
            rawLink = "vless://placeholder@example.com:443",
            uuidOrPassword = "00000000-0000-0000-0000-000000000000",
            extras = mapOf(
                "security" to "reality",
                "pbk" to "PLACEHOLDER_PBK",
                "sid" to "abcd1234",
                "sni" to "www.example.com",
                "flow" to "xtls-rprx-vision",
                "fp" to "chrome",
                "type" to "tcp",
                "encryption" to "none"
            )
        )
        val json = JSONObject(SingBoxConfigBuilder.build(node))
        assertTrue(json.has("inbounds"))
        assertTrue(json.has("outbounds"))
        val proxy = json.getJSONArray("outbounds").getJSONObject(0)
        assertEquals("vless", proxy.getString("type"))
        assertEquals("example.com", proxy.getString("server"))
        assertEquals(443, proxy.getInt("server_port"))
        assertEquals("00000000-0000-0000-0000-000000000000", proxy.getString("uuid"))
        val tls = proxy.getJSONObject("tls")
        assertTrue(tls.optBoolean("enabled", false) || tls.has("reality"))
    }
}
