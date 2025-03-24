package com.wissotsky.screentranslator.domain

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Provider for screen content information used in text processing and translation.
 */
class ScreenInfoProvider(private val context: Context) {
    
    companion object {
        private const val TAG = "ScreenInfoProvider"
    }
    
    /**
     * Extract text content from an AccessibilityNodeInfo.
     * 
     * @param nodeInfo The accessibility node to extract text from
     * @return The extracted text or null if no text is available
     */
    fun extractTextFromNode(nodeInfo: AccessibilityNodeInfo?): String? {
        if (nodeInfo == null) return null
        
        // Try to get text directly from the node
        val nodeText = nodeInfo.text?.toString()
        if (!nodeText.isNullOrBlank()) {
            return nodeText
        }
        
        // If no text is available, try to get a description
        val contentDescription = nodeInfo.contentDescription?.toString()
        if (!contentDescription.isNullOrBlank()) {
            return contentDescription
        }
        
        return null
    }
    
    /**
     * Gets the screen bounds for a node.
     * 
     * @param nodeInfo The accessibility node
     * @return The bounds of the node on screen or null if bounds cannot be determined
     */
    fun getNodeBounds(nodeInfo: AccessibilityNodeInfo?): Rect? {
        if (nodeInfo == null) return null
        
        val rect = Rect()
        try {
            nodeInfo.getBoundsInScreen(rect)
            if (rect.isEmpty) return null
            return rect
        } catch (e: Exception) {
            Log.e(TAG, "Error getting node bounds", e)
            return null
        }
    }
    
    /**
     * Determines if a node is visible on screen.
     * 
     * @param nodeInfo The accessibility node to check
     * @return True if the node is visible, false otherwise
     */
    fun isNodeVisible(nodeInfo: AccessibilityNodeInfo?): Boolean {
        if (nodeInfo == null) return false
        
        // Check if the node is visible to the user
        if (!nodeInfo.isVisibleToUser) return false
        
        // Get bounds and check if they're valid
        val bounds = getNodeBounds(nodeInfo)
        if (bounds == null || bounds.isEmpty) return false
        
        return true
    }
}