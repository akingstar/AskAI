package com.akingstar.askai

import org.json.JSONArray
import org.json.JSONObject

data class DiscoveredControl(
    val index: Int,
    val role: String,
    val label: String,
    val viewId: String,
    val clickable: Boolean,
    val editable: Boolean,
    val scrollable: Boolean,
    val enabled: Boolean
)

object ControlDiscovery {
    fun encode(items: List<DiscoveredControl>): String {
        val array = JSONArray()
        items.take(100).forEach { item ->
            array.put(
                JSONObject()
                    .put("index", item.index)
                    .put("role", item.role)
                    .put("label", item.label.take(120))
                    .put("viewId", item.viewId.take(160))
                    .put("clickable", item.clickable)
                    .put("editable", item.editable)
                    .put("scrollable", item.scrollable)
                    .put("enabled", item.enabled)
            )
        }
        return array.toString()
    }
}
