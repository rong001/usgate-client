package com.usgate.client.vpn

import com.usgate.client.subscription.ProxyNode
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds a minimal sing-box JSON config from a [ProxyNode].
 * Focus: vless:// (incl. Reality / Vision); also vmess:// and ss:// best-effort.
 */
object SingBoxConfigBuilder {

    fun build(node: ProxyNode): String {
        val root = JSONObject()
        root.put("log", JSONObject().put("level", "info").put("timestamp", true))
        root.put("dns", buildDns())
        root.put("inbounds", JSONArray().put(buildTunInbound()))
        root.put("outbounds", JSONArray().apply {
            put(buildProxyOutbound(node))
            put(JSONObject().put("type", "direct").put("tag", "direct"))
            put(JSONObject().put("type", "block").put("tag", "block"))
            put(JSONObject().put("type", "dns").put("tag", "dns-out"))
        })
        root.put("route", buildRoute())
        return root.toString(2)
    }

    private fun buildDns(): JSONObject {
        // sing-box 1.12+ style; works with libbox 1.13.x
        val servers = JSONArray()
            .put(
                JSONObject()
                    .put("type", "udp")
                    .put("tag", "dns-remote")
                    .put("server", "1.1.1.1")
                    .put("detour", "proxy")
            )
            .put(
                JSONObject()
                    .put("type", "local")
                    .put("tag", "dns-local")
                    .put("detour", "direct")
            )
        return JSONObject()
            .put("servers", servers)
            .put("final", "dns-remote")
            .put("strategy", "ipv4_only")
    }

    private fun buildTunInbound(): JSONObject {
        return JSONObject()
            .put("type", "tun")
            .put("tag", "tun-in")
            .put("inet4_address", "172.19.0.1/30")
            .put("mtu", 1500)
            .put("auto_route", true)
            .put("strict_route", true)
            .put("stack", "mixed")
            .put("sniff", true)
            .put("sniff_override_destination", true)
    }

    private fun buildRoute(): JSONObject {
        val rules = JSONArray()
            .put(JSONObject().put("protocol", "dns").put("outbound", "dns-out"))
            .put(
                JSONObject()
                    .put("ip_is_private", true)
                    .put("outbound", "direct")
            )
        return JSONObject()
            .put("auto_detect_interface", true)
            .put("rules", rules)
            .put("final", "proxy")
    }

    private fun buildProxyOutbound(node: ProxyNode): JSONObject {
        return when (node.protocol.lowercase()) {
            "vless" -> buildVless(node)
            "vmess" -> buildVmess(node)
            "ss", "shadowsocks" -> buildShadowsocks(node)
            else -> buildVless(node)
        }
    }

    private fun buildVless(node: ProxyNode): JSONObject {
        val extras = node.extras
        val out = JSONObject()
            .put("type", "vless")
            .put("tag", "proxy")
            .put("server", node.host)
            .put("server_port", node.port)
            .put("uuid", node.uuidOrPassword)
        val flow = extras["flow"].orEmpty()
        if (flow.isNotBlank()) out.put("flow", flow)

        val network = extras["type"] ?: extras["network"] ?: "tcp"
        when (network) {
            "ws" -> {
                val ws = JSONObject().put("path", extras["path"] ?: "/")
                val hostHeader = extras["host"] ?: extras["sni"]
                if (!hostHeader.isNullOrBlank()) {
                    ws.put("headers", JSONObject().put("Host", hostHeader))
                }
                out.put("transport", JSONObject().put("type", "ws").put("path", ws.optString("path", "/")).also {
                    if (ws.has("headers")) it.put("headers", ws.getJSONObject("headers"))
                })
            }
            "grpc" -> {
                out.put(
                    "transport",
                    JSONObject()
                        .put("type", "grpc")
                        .put("service_name", extras["serviceName"] ?: extras["service_name"] ?: "")
                )
            }
            // tcp / raw — no transport object
        }

        val security = (extras["security"] ?: "").lowercase()
        if (security == "tls" || security == "reality") {
            out.put("tls", buildTls(extras, security == "reality"))
        }
        return out
    }

    private fun buildTls(extras: Map<String, String>, reality: Boolean): JSONObject {
        val tls = JSONObject().put("enabled", true)
        val sni = extras["sni"] ?: extras["host"]
        if (!sni.isNullOrBlank()) tls.put("server_name", sni)
        val insecure = extras["allowInsecure"] == "1" || extras["insecure"] == "1"
        if (insecure) tls.put("insecure", true)

        val fp = extras["fp"] ?: extras["fingerprint"]
        if (!fp.isNullOrBlank()) {
            tls.put(
                "utls",
                JSONObject().put("enabled", true).put("fingerprint", fp)
            )
        }

        if (reality) {
            val realityObj = JSONObject().put("enabled", true)
            val pbk = extras["pbk"] ?: extras["publicKey"] ?: extras["public_key"]
            val sid = extras["sid"] ?: extras["shortId"] ?: extras["short_id"]
            if (!pbk.isNullOrBlank()) realityObj.put("public_key", pbk)
            if (!sid.isNullOrBlank()) realityObj.put("short_id", sid)
            tls.put("reality", realityObj)
        }
        return tls
    }

    private fun buildVmess(node: ProxyNode): JSONObject {
        val extras = node.extras
        val out = JSONObject()
            .put("type", "vmess")
            .put("tag", "proxy")
            .put("server", node.host)
            .put("server_port", node.port)
            .put("uuid", node.uuidOrPassword)
            .put("security", extras["scy"] ?: extras["security"] ?: "auto")
            .put("alter_id", (extras["aid"] ?: "0").toIntOrNull() ?: 0)

        val network = extras["net"] ?: extras["type"] ?: "tcp"
        when (network) {
            "ws" -> {
                out.put(
                    "transport",
                    JSONObject()
                        .put("type", "ws")
                        .put("path", extras["path"] ?: "/")
                        .put(
                            "headers",
                            JSONObject().put("Host", extras["host"] ?: node.host)
                        )
                )
            }
            "grpc" -> {
                out.put(
                    "transport",
                    JSONObject()
                        .put("type", "grpc")
                        .put("service_name", extras["path"] ?: extras["serviceName"] ?: "")
                )
            }
        }
        val tlsFlag = extras["tls"]
        if (tlsFlag == "tls" || !extras["sni"].isNullOrBlank()) {
            out.put("tls", buildTls(extras, reality = false))
        }
        return out
    }

    private fun buildShadowsocks(node: ProxyNode): JSONObject {
        // uuidOrPassword may be "method:password"
        val user = node.uuidOrPassword
        val method: String
        val password: String
        val idx = user.indexOf(':')
        if (idx > 0) {
            method = user.substring(0, idx)
            password = user.substring(idx + 1)
        } else {
            method = node.extras["method"] ?: "aes-256-gcm"
            password = user
        }
        return JSONObject()
            .put("type", "shadowsocks")
            .put("tag", "proxy")
            .put("server", node.host)
            .put("server_port", node.port)
            .put("method", method)
            .put("password", password)
    }
}
