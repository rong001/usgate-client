package com.usgate.client.subscription

import java.util.Base64
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import org.json.JSONObject

/**
 * Parses common 3X-UI / v2rayN style subscriptions:
 * - base64-encoded multi-line share links
 * - plain multi-line vless:// vmess:// ss:// links
 */
object SubscriptionParser {

    fun parse(content: String): List<ProxyNode> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return emptyList()

        val decoded = decodeMaybeBase64(trimmed)
        val lines = decoded
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        return lines.mapNotNull { parseShareLink(it) }
    }

    private fun decodeMaybeBase64(raw: String): String {
        // Heuristic: if it looks like share links already, keep as-is
        val lower = raw.lowercase()
        if (lower.contains("vless://") || lower.contains("vmess://") || lower.contains("ss://")) {
            return raw
        }
        return try {
            val cleaned = raw.replace("\\s".toRegex(), "")
            val bytes = Base64.getDecoder().decode(cleaned)
            String(bytes, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            raw
        }
    }

    fun parseShareLink(link: String): ProxyNode? {
        val lower = link.lowercase()
        return when {
            lower.startsWith("vless://") -> parseVless(link)
            lower.startsWith("vmess://") -> parseVmess(link)
            lower.startsWith("ss://") -> parseShadowsocks(link)
            else -> null
        }
    }

    private fun parseVless(link: String): ProxyNode? {
        // vless://uuid@host:port?params#name
        return try {
            val withoutScheme = link.removePrefix("vless://").removePrefix("VLESS://")
            val hashIdx = withoutScheme.indexOf('#')
            val name = if (hashIdx >= 0) {
                URLDecoder.decode(withoutScheme.substring(hashIdx + 1), "UTF-8")
            } else {
                "VLESS"
            }
            val main = if (hashIdx >= 0) withoutScheme.substring(0, hashIdx) else withoutScheme
            val at = main.indexOf('@')
            if (at < 0) return null
            val uuid = main.substring(0, at)
            val hostPortQuery = main.substring(at + 1)
            val qIdx = hostPortQuery.indexOf('?')
            val hostPort = if (qIdx >= 0) hostPortQuery.substring(0, qIdx) else hostPortQuery
            val query = if (qIdx >= 0) hostPortQuery.substring(qIdx + 1) else ""
            val (host, port) = splitHostPort(hostPort)
            val extras = parseQuery(query)
            ProxyNode(
                id = UUID.nameUUIDFromBytes(link.toByteArray()).toString(),
                name = name.ifBlank { "VLESS-$host" },
                protocol = "vless",
                host = host,
                port = port,
                rawLink = link,
                uuidOrPassword = uuid,
                extras = extras
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseVmess(link: String): ProxyNode? {
        // vmess://base64(json)
        return try {
            val b64 = link.substringAfter("://")
            val jsonStr = String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8)
            val obj = JSONObject(jsonStr)
            val host = obj.optString("add", obj.optString("host", ""))
            val port = obj.optInt("port", 0)
            val name = obj.optString("ps", "VMess-$host")
            val uuid = obj.optString("id", "")
            val extras = mutableMapOf<String, String>()
            obj.keys().forEach { key ->
                if (key !in setOf("add", "host", "port", "ps", "id")) {
                    extras[key] = obj.optString(key)
                }
            }
            if (host.isBlank() || port == 0) return null
            ProxyNode(
                id = UUID.nameUUIDFromBytes(link.toByteArray()).toString(),
                name = name,
                protocol = "vmess",
                host = host,
                port = port,
                rawLink = link,
                uuidOrPassword = uuid,
                extras = extras
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseShadowsocks(link: String): ProxyNode? {
        // ss://method:password@host:port#name  OR  ss://base64(method:password)@host:port#name
        return try {
            val withoutScheme = link.removePrefix("ss://").removePrefix("SS://")
            val hashIdx = withoutScheme.indexOf('#')
            val name = if (hashIdx >= 0) {
                URLDecoder.decode(withoutScheme.substring(hashIdx + 1), "UTF-8")
            } else {
                "SS"
            }
            val main = if (hashIdx >= 0) withoutScheme.substring(0, hashIdx) else withoutScheme
            val at = main.lastIndexOf('@')
            if (at < 0) return null
            val userInfo = main.substring(0, at)
            val hostPort = main.substring(at + 1)
            val (host, port) = splitHostPort(hostPort)
            val decodedUser = try {
                String(Base64.getDecoder().decode(userInfo), StandardCharsets.UTF_8)
            } catch (_: Exception) {
                userInfo
            }
            ProxyNode(
                id = UUID.nameUUIDFromBytes(link.toByteArray()).toString(),
                name = name.ifBlank { "SS-$host" },
                protocol = "ss",
                host = host,
                port = port,
                rawLink = link,
                uuidOrPassword = decodedUser,
                extras = emptyMap()
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun splitHostPort(hostPort: String): Pair<String, Int> {
        return if (hostPort.startsWith("[")) {
            val end = hostPort.indexOf(']')
            val host = hostPort.substring(1, end)
            val port = hostPort.substring(end + 2).toInt()
            host to port
        } else {
            val colon = hostPort.lastIndexOf(':')
            hostPort.substring(0, colon) to hostPort.substring(colon + 1).toInt()
        }
    }

    private fun parseQuery(q: String): Map<String, String> {
        if (q.isBlank()) return emptyMap()
        return q.split("&").mapNotNull { part ->
            val i = part.indexOf('=')
            if (i <= 0) null
            else {
                val k = URLDecoder.decode(part.substring(0, i), "UTF-8")
                val v = URLDecoder.decode(part.substring(i + 1), "UTF-8")
                k to v
            }
        }.toMap()
    }
}
