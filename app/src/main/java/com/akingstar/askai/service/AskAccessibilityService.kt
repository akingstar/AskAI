package com.akingstar.askai.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AskAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* Screen changes observed only while service is enabled. */ }
    override fun onInterrupt() {}
    fun visibleText(): String {
        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return "इस स्क्रीन का पाठ उपलब्ध नहीं है।"
        val out = StringBuilder()
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 12 || out.length > 5000) return
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let { out.append(it).append('\n') }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { out.append(it).append('\n') }
            for (i in 0 until node.childCount) node.getChild(i)?.let { child -> walk(child, depth + 1); child.recycle() }
        }
        walk(root, 0); root.recycle()
        return out.toString().ifBlank { "इस स्क्रीन पर पढ़ने योग्य पाठ नहीं मिला।" }
    }
}
