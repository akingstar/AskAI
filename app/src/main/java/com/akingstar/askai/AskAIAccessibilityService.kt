package com.akingstar.askai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AskAIAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (activeService === this) activeService = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (activeService === this) activeService = null
        super.onDestroy()
    }

    private fun performCommand(
        action: String,
        value: String,
        target: String
    ): String? {
        return when (action) {
            "tap_text" ->
                if (target.isNotBlank() && tapText(target)) "टैप कर दिया।" else null

            "long_press_text" ->
                if (target.isNotBlank() && longPressText(target)) "लॉन्ग प्रेस किया।" else null

            "type_text" ->
                if (target.isNotBlank() && setText(value, target)) "टेक्स्ट लिख दिया।" else null

            "scroll_up" ->
                if (scroll("up")) "ऊपर स्क्रॉल किया।" else null

            "scroll_down" ->
                if (scroll("down")) "नीचे स्क्रॉल किया।" else null

            "home" ->
                if (global("home")) "होम स्क्रीन खोल दी।" else null

            "back" ->
                if (global("back")) "पीछे आ गया।" else null

            "recents" ->
                if (global("recents")) "हाल के ऐप खोल दिए।" else null

            "notifications" ->
                if (global("notifications")) "नोटिफिकेशन खोल दिए।" else null

            "quick_settings" ->
                if (global("quick_settings")) "क्विक सेटिंग खोल दी।" else null

            "read_screen" ->
                readScreen().takeIf { it.isNotBlank() }

            else -> null
        }
    }

    companion object {
        @Volatile
        private var activeService: AskAIAccessibilityService? = null

        fun perform(
            action: String,
            value: String = "",
            target: String = ""
        ): String? {
            if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
                return null
            }
            val service = activeService ?: return null
            return runCatching {
                service.performCommand(action, value, target)
            }.getOrNull()
        }
    }

    fun global(action: String): Boolean {
        val code = when (action) {
            "home" -> GLOBAL_ACTION_HOME
            "back" -> GLOBAL_ACTION_BACK
            "recents" -> GLOBAL_ACTION_RECENTS
            "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
            "quick_settings" -> GLOBAL_ACTION_QUICK_SETTINGS
            "power" -> GLOBAL_ACTION_POWER_DIALOG
            "lock" -> GLOBAL_ACTION_LOCK_SCREEN
            "screenshot" -> if (Build.VERSION.SDK_INT >= 28) GLOBAL_ACTION_TAKE_SCREENSHOT else return false
            else -> return false
        }
        return performGlobalAction(code)
    }

    fun tapText(text: String): Boolean {
        if (text.isBlank() || text.length > 100) return false
        val root = rootInActiveWindow ?: return false
        val node = findText(root, text) ?: return false
        val clickable = clickableParent(node) ?: return false
        return clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun longPressText(text: String): Boolean {
        if (text.isBlank() || text.length > 100) return false
        val root = rootInActiveWindow ?: return false
        val node = findText(root, text) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    fun setText(text: String, fieldHint: String): Boolean {
        if (text.isBlank() || text.length > 500 ||
            fieldHint.isBlank() || fieldHint.length > 100
        ) return false

        val root = rootInActiveWindow ?: return false
        val matches = findEditables(root).filter { node ->
            listOfNotNull(
                node.text?.toString(),
                node.hintText?.toString(),
                node.contentDescription?.toString()
            ).any { it.contains(fieldHint, ignoreCase = true) }
        }

        // Never guess when the field hint matches zero or multiple fields.
        val node = matches.singleOrNull() ?: return false

        return node.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            android.os.Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }
        )
    }

    fun scroll(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findScrollable(root) ?: return false
        val action = when (direction) {
            "up" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            "down" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else -> return false
        }
        return node.performAction(action)
    }

    fun readScreen(): String {
        val root = rootInActiveWindow ?: return ""
        val out = StringBuilder()
        collect(root, out)
        return SensitiveDataFilter.mask(out.toString()).take(4000)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 350): Boolean {
        if (Build.VERSION.SDK_INT < 24) return false
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun tap(x: Float, y: Float): Boolean = swipe(x, y, x, y, 80)

    private fun findText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val label = listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).joinToString(" ")
        if (label.contains(text, true)) return node
        for (i in 0 until node.childCount) {
            val found = node.getChild(i)?.let { findText(it, text) }
            if (found != null) return found
        }
        return null
    }

    private fun findEditables(
        node: AccessibilityNodeInfo
    ): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()

        fun collectEditable(current: AccessibilityNodeInfo) {
            if (current.isEditable) result.add(current)
            for (i in 0 until current.childCount) {
                current.getChild(i)?.let(::collectEditable)
            }
        }

        collectEditable(node)
        return result
    }

    private fun findScrollable(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val found = node.getChild(i)?.let(::findScrollable)
            if (found != null) return found
        }
        return null
    }

    private fun clickableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    private fun collect(node: AccessibilityNodeInfo, out: StringBuilder) {
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        if (!text.isNullOrBlank()) out.append(text).append(". ")
        for (i in 0 until node.childCount) node.getChild(i)?.let { collect(it, out) }
    }
}
