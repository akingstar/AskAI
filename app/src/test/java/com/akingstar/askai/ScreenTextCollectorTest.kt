package com.akingstar.askai

import org.junit.Assert.*
import org.junit.Test

class ScreenTextCollectorTest {
    private data class FakeNode(
        override val text: CharSequence? = null,
        override val contentDescription: CharSequence? = null,
        override val isPassword: Boolean = false,
        override val isEditable: Boolean = false,
        override val isVisibleToUser: Boolean = true,
        private val children: List<ScreenTextNode> = emptyList()
    ) : ScreenTextNode {
        override val childCount: Int
            get() = children.size

        override fun childAt(index: Int): ScreenTextNode? =
            children.getOrNull(index)
    }

    @Test
    fun skipsPasswordAndEditableNodesAndTheirDescendants() {
        val root = FakeNode(
            children = listOf(
                FakeNode(text = "Welcome"),
                FakeNode(
                    text = "secret-password",
                    isPassword = true,
                    children = listOf(FakeNode(text = "nested-secret"))
                ),
                FakeNode(
                    text = "typed-private-value",
                    isEditable = true,
                    children = listOf(FakeNode(text = "editable-child-secret"))
                ),
                FakeNode(text = "Continue")
            )
        )

        val result = ScreenTextCollector.collect(root)
        assertTrue(result.contains("Welcome"))
        assertTrue(result.contains("Continue"))
        assertFalse(result.contains("secret"))
        assertFalse(result.contains("typed-private"))
    }

    @Test
    fun skipsParentTextThatMayAggregateEditableChildValues() {
        val root = FakeNode(
            text = "Account form typed-private-password",
            contentDescription = "Form containing secret input",
            children = listOf(
                FakeNode(
                    text = "typed-private-password",
                    isEditable = true
                ),
                FakeNode(text = "Continue")
            )
        )

        val result = ScreenTextCollector.collect(root)

        assertFalse(result.contains("typed-private-password"))
        assertFalse(result.contains("Account form"))
        assertTrue(result.contains("Continue"))
    }

    @Test
    fun masksSensitiveTextButKeepsSafeScreenText() {
        val root = FakeNode(
            text = "Contact test@example.com or call 9876543210"
        )

        val result = ScreenTextCollector.collect(root)
        assertTrue(result.contains("Contact"))
        assertTrue(result.contains("छिपाई गई"))
        assertFalse(result.contains("test@example.com"))
        assertFalse(result.contains("9876543210"))
    }

    @Test
    fun ignoresInvisibleNodes() {
        val root = FakeNode(
            children = listOf(
                FakeNode(text = "Visible"),
                FakeNode(text = "Hidden", isVisibleToUser = false)
            )
        )

        val result = ScreenTextCollector.collect(root)
        assertTrue(result.contains("Visible"))
        assertFalse(result.contains("Hidden"))
    }

    @Test
    fun respectsNodeAndOutputLimits() {
        val children = (1..350).map {
            FakeNode(text = "node-$it")
        }
        val result = ScreenTextCollector.collect(
            FakeNode(children = children)
        )

        assertTrue(result.length <= ScreenTextCollector.MAX_OUTPUT_CHARS)
        assertFalse(result.contains("node-350"))
    }

    @Test
    fun nodeAccessFailureDiscardsAllPartiallyCollectedText() {
        val root = FakeNode(
            text = "Safe text",
            children = listOf(ThrowingNode())
        )

        assertEquals("", ScreenTextCollector.collect(root))
    }

    private class ThrowingNode : ScreenTextNode {
        override val text: CharSequence?
            get() = error("simulated accessibility failure")
        override val contentDescription: CharSequence? = null
        override val isPassword = false
        override val isEditable = false
        override val isVisibleToUser = true
        override val childCount = 0
        override fun childAt(index: Int): ScreenTextNode? = null
    }

    @Test
    fun nullRootReturnsEmptyText() {
        assertEquals("", ScreenTextCollector.collect(null))
    }
}
