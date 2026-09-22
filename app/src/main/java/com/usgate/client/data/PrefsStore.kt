package com.usgate.client.data

import android.content.Context
import com.usgate.client.subscription.ProxyNode
import com.usgate.client.subscription.SubscriptionParser
import org.json.JSONArray
import org.json.JSONObject

class PrefsStore(context: Context) {
    private val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var subscriptionUrl: String
        get() = sp.getString(KEY_SUB_URL, "") ?: ""
        set(value) = sp.edit().putString(KEY_SUB_URL, value).apply()

    var selectedNodeId: String?
        get() = sp.getString(KEY_SELECTED_ID, null)
        set(value) = sp.edit().putString(KEY_SELECTED_ID, value).apply()

    fun saveNodes(nodes: List<ProxyNode>) {
        val arr = JSONArray()
        nodes.forEach { n ->
            arr.put(
                JSONObject()
                    .put("id", n.id)
                    .put("name", n.name)
                    .put("protocol", n.protocol)
                    .put("host", n.host)
                    .put("port", n.port)
                    .put("rawLink", n.rawLink)
                    .put("uuidOrPassword", n.uuidOrPassword)
            )
        }
        sp.edit().putString(KEY_NODES, arr.toString()).apply()
    }

    fun loadNodes(): List<ProxyNode> {
        val raw = sp.getString(KEY_NODES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        ProxyNode(
                            id = o.getString("id"),
                            name = o.getString("name"),
                            protocol = o.getString("protocol"),
                            host = o.getString("host"),
                            port = o.getInt("port"),
                            rawLink = o.getString("rawLink"),
                            uuidOrPassword = o.optString("uuidOrPassword", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun selectedNode(): ProxyNode? {
        val id = selectedNodeId ?: return null
        return loadNodes().firstOrNull { it.id == id }
    }

    /** Persist pasted raw body as well (optional debugging aid, not a secret store). */
    fun saveLastImportBody(body: String) {
        sp.edit().putString(KEY_LAST_BODY, body.take(200_000)).apply()
    }

    fun reparseFromLinks(linksBody: String): List<ProxyNode> {
        val nodes = SubscriptionParser.parse(linksBody)
        saveNodes(nodes)
        saveLastImportBody(linksBody)
        return nodes
    }

    companion object {
        private const val PREFS = "usgate_prefs"
        private const val KEY_SUB_URL = "subscription_url"
        private const val KEY_NODES = "nodes_json"
        private const val KEY_SELECTED_ID = "selected_node_id"
        private const val KEY_LAST_BODY = "last_import_body"
    }
}
