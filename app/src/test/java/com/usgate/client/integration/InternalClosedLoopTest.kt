package com.usgate.client.integration

import com.usgate.client.subscription.SubscriptionParser
import com.usgate.client.vpn.SingBoxConfigBuilder
import java.util.Base64
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * INTERNAL_CLOSED_LOOP (Android JVM): synthetic subscription fixture → parser →
 * SingBoxConfigBuilder. Placeholders only — no live hosts/UUIDs/keys.
 *
 * Portal revoke/disable is covered by usgate-demo portal/tests/e2e_internal_closed_loop.py.
 * Physical device exit-IP / reconnect / admin-seen-device = BLOCKED (no device).
 */
class InternalClosedLoopTest {

    // Shape mirrors typical 3X-UI base64 sub body; all values are fixtures.
    private val synthPlain =
        """
        vless://11111111-1111-4111-8111-111111111111@example.com:443?encryption=none&security=tls&type=ws&path=%2Ficl-test#ICL-Synthetic
        """.trimIndent()

    @Test
    fun internalClosedLoop_parserAcceptsSyntheticSubscription() {
        val b64 = Base64.getEncoder().encodeToString(synthPlain.toByteArray())
        val nodes = SubscriptionParser.parse(b64)
        assertEquals(1, nodes.size)
        assertEquals("vless", nodes[0].protocol)
        assertEquals("example.com", nodes[0].host)
        assertEquals(443, nodes[0].port)
        assertEquals("ICL-Synthetic", nodes[0].name)
        assertTrue(nodes[0].uuidOrPassword.isNotBlank())
    }

    @Test
    fun internalClosedLoop_configBuilderAcceptsParsedNode() {
        val node = SubscriptionParser.parse(synthPlain).single()
        val json = JSONObject(SingBoxConfigBuilder.build(node))
        assertTrue(json.has("inbounds"))
        assertTrue(json.has("outbounds"))
        val proxy = json.getJSONArray("outbounds").getJSONObject(0)
        assertEquals("vless", proxy.getString("type"))
        assertEquals("example.com", proxy.getString("server"))
        assertEquals(443, proxy.getInt("server_port"))
    }

    @Test
    fun internalClosedLoop_afterRevokeEmptySubRejectedByParser() {
        // After admin revoke, refreshed sub body is empty / garbage — client must not build nodes.
        assertTrue(SubscriptionParser.parse("").isEmpty())
        assertTrue(SubscriptionParser.parse("   ").isEmpty())
        assertTrue(SubscriptionParser.parse("not-a-subscription").isEmpty())
        val garbageB64 = Base64.getEncoder().encodeToString("revoked".toByteArray())
        val nodes = SubscriptionParser.parse(garbageB64)
        assertTrue(nodes.isEmpty())
    }

    @Test
    fun internalClosedLoop_noLiveSecretMarkersInFixture() {
        // Guard: fixture must stay placeholder-shaped (example.com / ICL / 1111-… UUID).
        assertTrue(synthPlain.contains("example.com"))
        assertTrue(synthPlain.contains("ICL-Synthetic"))
        assertFalse(synthPlain.contains("117."))
        assertFalse(synthPlain.contains("VPS_IP"))
    }
}
