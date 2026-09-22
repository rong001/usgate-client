package com.usgate.client.subscription

/**
 * Normalized proxy node parsed from share links / subscription body.
 * Host/port are placeholders-safe; never commit real production endpoints.
 */
data class ProxyNode(
    val id: String,
    val name: String,
    val protocol: String,
    val host: String,
    val port: Int,
    val rawLink: String,
    val uuidOrPassword: String = "",
    val extras: Map<String, String> = emptyMap()
) {
    fun displayMeta(): String = "$protocol · $host:$port"
}
