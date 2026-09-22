package com.usgate.client.subscription

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit tests for subscription parsing. Uses placeholders only — never real hosts/UUIDs from production.
 */
class SubscriptionParserTest {

    @Test
    fun parsePlainVlessShareLink() {
        val link =
            "vless://00000000-0000-0000-0000-000000000000@example.com:443" +
                "?encryption=none&security=tls&type=ws&path=%2Fplaceholder#US-Placeholder-VLESS"
        val nodes = SubscriptionParser.parse(link)
        assertEquals(1, nodes.size)
        val n = nodes[0]
        assertEquals("vless", n.protocol)
        assertEquals("example.com", n.host)
        assertEquals(443, n.port)
        assertEquals("US-Placeholder-VLESS", n.name)
        assertEquals("00000000-0000-0000-0000-000000000000", n.uuidOrPassword)
        assertEquals("ws", n.extras["type"])
        assertEquals("/placeholder", n.extras["path"])
    }

    @Test
    fun parseBase64MultilineSubscription() {
        val body =
            """
            vless://00000000-0000-0000-0000-000000000001@example.com:443?encryption=none&security=tls&type=tcp#Node-A
            ss://YWVzLTI1Ni1nY206cGFzc3dvcmQ@example.com:8388#Node-B
            """.trimIndent()
        val b64 = Base64.getEncoder().encodeToString(body.toByteArray())
        val nodes = SubscriptionParser.parse(b64)
        assertEquals(2, nodes.size)
        assertEquals("vless", nodes[0].protocol)
        assertEquals("ss", nodes[1].protocol)
        assertEquals("Node-B", nodes[1].name)
    }

    @Test
    fun parseVmessJsonShareLink() {
        val json =
            """{"v":"2","ps":"VMess-Placeholder","add":"example.com","port":"443","id":"00000000-0000-0000-0000-000000000002","aid":"0","scy":"auto","net":"ws","type":"none","host":"example.com","path":"/vmess","tls":"tls"}"""
        val link = "vmess://" + Base64.getEncoder().encodeToString(json.toByteArray())
        val node = SubscriptionParser.parseShareLink(link)
        assertNotNull(node)
        assertEquals("vmess", node!!.protocol)
        assertEquals("example.com", node.host)
        assertEquals(443, node.port)
        assertEquals("VMess-Placeholder", node.name)
    }

    @Test
    fun emptyAndGarbageReturnEmptyOrNull() {
        assertTrue(SubscriptionParser.parse("").isEmpty())
        assertTrue(SubscriptionParser.parse("   ").isEmpty())
        assertNull(SubscriptionParser.parseShareLink("https://example.com/not-a-share-link"))
        assertNull(SubscriptionParser.parseShareLink("ftp://nope"))
    }

    @Test
    fun commentsAndBlankLinesSkipped() {
        val content =
            """
            # comment
            vless://00000000-0000-0000-0000-000000000003@example.com:8443?encryption=none&security=reality&pbk=PLACEHOLDER_PBK&sid=abcd1234&sni=www.example.com&flow=xtls-rprx-vision&fp=chrome&type=tcp#Reality-Placeholder

            """.trimIndent()
        val nodes = SubscriptionParser.parse(content)
        assertEquals(1, nodes.size)
        assertEquals("reality", nodes[0].extras["security"])
        assertEquals("xtls-rprx-vision", nodes[0].extras["flow"])
    }
}
