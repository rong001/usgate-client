package com.usgate.client.subscription

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Fetches subscription body from URL (3X-UI subscription endpoint) or parses pasted text.
 */
object SubscriptionImporter {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun importFromUrl(url: String): List<ProxyNode> = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            "订阅地址需以 http:// 或 https:// 开头"
        }
        // Placeholders only in docs; runtime may use user-pasted URL.
        val req = Request.Builder()
            .url(trimmed)
            .header("User-Agent", "USGate/0.1")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body?.string().orEmpty()
            SubscriptionParser.parse(body)
        }
    }

    fun importFromText(text: String): List<ProxyNode> = SubscriptionParser.parse(text)
}
