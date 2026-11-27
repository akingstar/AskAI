package com.akingstar.askai

/**
 * Small adapter contract so screen-text privacy rules can be unit-tested
 * without constructing Android AccessibilityNodeInfo objects.
 */
internal interface ScreenTextNode {
    val text: CharSequence?
    val contentDescription: CharSequence?
    val isPassword: Boolean
    val isEditable: Boolean
    val isVisibleToUser: Boolean
    val childCount: Int
    fun childAt(index: Int): ScreenTextNode?
}

internal object ScreenTextCollector {
    const val MAX_DEPTH = 20
    const val MAX_NODES = 300
    const val MAX_OUTPUT_CHARS = 4000

    fun collect(root: ScreenTextNode?): String {
        if (root == null) return ""

        val output = StringBuilder()
        var visited = 0

        fun visit(node: ScreenTextNode, depth: Int) {
            if (depth > MAX_DEPTH ||
                visited >= MAX_NODES ||
                output.length >= MAX_OUTPUT_CHARS
            ) return

            visited++

            // Do not inspect descendants of password or editable fields.
            if (!node.isVisibleToUser || node.isPassword || node.isEditable) return

            // Parent nodes may aggregate text from editable descendants.
            // Only read text from leaf nodes to avoid exposing typed values.
            val childCount = node.childCount

            if (childCount == 0) {
                val raw = node.text?.toString()
                    ?: node.contentDescription?.toString()

                if (!raw.isNullOrBlank()) {
                    val safe = SensitiveDataFilter.mask(raw)
                        .replace(Regex("\\s+"), " ")
                        .trim()

                    if (safe.isNotBlank()) {
                        if (output.isNotEmpty() && output.length < MAX_OUTPUT_CHARS) {
                            output.append(". ")
                        }
                        val remaining = MAX_OUTPUT_CHARS - output.length
                        if (remaining > 0) output.append(safe.take(remaining))
                    }
                }
            }

            for (index in 0 until childCount) {
                if (visited >= MAX_NODES ||
                    output.length >= MAX_OUTPUT_CHARS
                ) break

                val child = node.childAt(index)
                if (child != null) visit(child, depth + 1)
            }
        }

        // Fail closed: never return a partial screen if any node access fails.
        return runCatching {
            visit(root, 0)
            output.toString().take(MAX_OUTPUT_CHARS)
        }.getOrDefault("")
    }
}
