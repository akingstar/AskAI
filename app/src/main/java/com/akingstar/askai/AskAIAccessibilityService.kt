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

    @Volatile private var latestControlInventory: String = "[]"
    private var lastDiscoveryEventTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return
        if (event.eventTime - lastDiscoveryEventTime < 350L) return
        lastDiscoveryEventTime = event.eventTime
        latestControlInventory = discoverControls()
    }
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

            "discover_controls" ->
                discoverControls()

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
        if (text.isBlank() || text.length > 100 ||
            SafetyGate.isRiskyTapTarget(text)
        ) return false

        val root = rootInActiveWindow ?: return false
        val node = findUniqueExactText(root, text) ?: return false
        val clickable = clickableParent(node) ?: return false

        if (!clickable.isEnabled || !clickable.isVisibleToUser) return false
        return clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun longPressText(text: String): Boolean {
        if (text.isBlank() || text.length > 100 ||
            SafetyGate.isRiskyTapTarget(text)
        ) return false

        val root = rootInActiveWindow ?: return false
        val node = findUniqueExactText(root, text) ?: return false
        val target = longClickableParent(node) ?: return false

        if (!target.isEnabled || !target.isVisibleToUser) return false
        return target.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    fun setText(text: String, fieldHint: String): Boolean {
        if (text.isBlank() || text.length > 500 ||
            fieldHint.isBlank() || fieldHint.length > 100 ||
            SafetyGate.isSensitiveInputTarget(fieldHint) ||
            SensitiveDataFilter.containsSensitive(text)
        ) return false

        val root = rootInActiveWindow ?: return false
        val matches = findEditables(root).filter { node ->
            if (!node.isEnabled || !node.isVisibleToUser || node.isPassword) {
                false
            } else {
                val labels = listOfNotNull(
                    node.hintText?.toString(),
                    node.contentDescription?.toString()
                )
                labels.any { ControlTargetMatcher.matches(it, fieldHint) } &&
                    labels.none { SafetyGate.isSensitiveInputTarget(it) } &&
                    labels.none { SensitiveDataFilter.containsSensitive(it) }
            }
        }

        // Zero or multiple matches: refuse to guess.
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

    fun discoverControls(): String {
        val root = rootInActiveWindow ?: return "[]"
        val controls = mutableListOf<DiscoveredControl>()
        var visited = 0

        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 20 || visited >= 300 || controls.size >= 100) return
            visited++

            val password = node.isPassword
            val editable = node.isEditable
            val rawLabel = when {
                password -> "Password field"
                editable -> listOfNotNull(
                    node.hintText?.toString(),
                    node.contentDescription?.toString()
                ).firstOrNull()
                else -> listOfNotNull(
                    node.text?.toString(),
                    node.contentDescription?.toString()
                ).firstOrNull()
            }.orEmpty()

            val label = SensitiveDataFilter.mask(rawLabel)
                .replace(Regex("[\\\\r\\\\n\\\\t]+"), " ")
                .take(120)

            val role = when {
                password -> "password_field"
                editable -> "text_field"
                node.isScrollable -> "scroll_area"
                node.isCheckable -> "checkable"
                node.isClickable -> "clickable"
                else -> "text"
            }

            if (role != "text" || label.isNotBlank()) {
                controls.add(
                    DiscoveredControl(
                        index = controls.size,
                        role = role,
                        label = label,
                        viewId = if (password) "" else
                            (node.viewIdResourceName ?: "").take(160),
                        clickable = node.isClickable,
                        editable = editable,
                        scrollable = node.isScrollable,
                        enabled = node.isEnabled
                    )
                )
            }

            for (i in 0 until node.childCount) {
                if (visited >= 300 || controls.size >= 100) break
                node.getChild(i)?.let { visit(it, depth + 1) }
            }
        }

        visit(root, 0)
        return ControlDiscovery.encode(controls).also {
            latestControlInventory = it
        }
    }

    fun readScreen(): String {
        val root = rootInActiveWindow ?: return ""
        return runCatching {
            ScreenTextCollector.collect(AccessibilityScreenTextNode(root))
        }.getOrDefault("")
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

    private fun findUniqueExactText(
        root: AccessibilityNodeInfo,
        target: String
    ): AccessibilityNodeInfo? {
        val matches = mutableListOf<AccessibilityNodeInfo>()
        var visited = 0

        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 20 || visited >= 300 || matches.size > 1) return
            visited++

            if (node.isVisibleToUser && node.isEnabled) {
                val labels = listOfNotNull(
                    node.text?.toString(),
                    node.contentDescription?.toString()
                )
                if (labels.any { ControlTargetMatcher.matches(it, target) }) {
                    matches.add(node)
                    if (matches.size > 1) return
                }
            }

            for (i in 0 until node.childCount) {
                if (visited >= 300 || matches.size > 1) break
                node.getChild(i)?.let { visit(it, depth + 1) }
            }
        }

        visit(root, 0)
        return matches.singleOrNull()
    }

    private fun findEditables(
        node: AccessibilityNodeInfo
    ): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        var visited = 0

        fun collectEditable(current: AccessibilityNodeInfo, depth: Int) {
            if (depth > 20 || visited >= 300) return
            visited++
            if (current.isEditable) result.add(current)

            for (i in 0 until current.childCount) {
                if (visited >= 300) break
                current.getChild(i)?.let { collectEditable(it, depth + 1) }
            }
        }

        collectEditable(node, 0)
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
        var depth = 0
        while (current != null && depth < 20) {
            if (current.isClickable && current.isEnabled &&
                current.isVisibleToUser
            ) return current
            current = current.parent
            depth++
        }
        return null
    }

    private fun longClickableParent(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 20) {
            if (current.isLongClickable && current.isEnabled &&
                current.isVisibleToUser
            ) return current
            current = current.parent
            depth++
        }
        return null
    }

    private class AccessibilityScreenTextNode(
        private val node: AccessibilityNodeInfo
    ) : ScreenTextNode {
        override val text: CharSequence?
            get() = node.text

        override val contentDescription: CharSequence?
            get() = node.contentDescription

        override val isPassword: Boolean
            get() = node.isPassword

        override val isEditable: Boolean
            get() = node.isEditable

        override val isVisibleToUser: Boolean
            get() = node.isVisibleToUser

        override val childCount: Int
            get() = node.childCount

        override fun childAt(index: Int): ScreenTextNode? =
            node.getChild(index)?.let(::AccessibilityScreenTextNode)
    }
}
