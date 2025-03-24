package com.wissotsky.screentranslator.data

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Data class that represents information about a node containing text on the screen.
 */
data class NodeInfo(
    val node: AccessibilityNodeInfo,
    val text: String,
    val bounds: Rect
) {
    /**
     * A unique identifier for this node.
     */
    val id: Int
        get() = node.hashCode()
        
    /**
     * Creates a copy with the same data but a new text value.
     */
    fun copyWithText(newText: String): NodeInfo = NodeInfo(
        node = node,
        text = newText,
        bounds = bounds
    )
}