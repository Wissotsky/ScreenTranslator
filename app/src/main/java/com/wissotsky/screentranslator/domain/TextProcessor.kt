package com.wissotsky.screentranslator.domain

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.wissotsky.screentranslator.data.NodeInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap
import android.os.SystemClock

class TextProcessor(
    private val systemInfoProvider: SystemInfoProvider,
    private val debounceInterval: Long = 500L,
    private val largeNodeThreshold: Double = 0.5,
    private val refreshInterval: Long = 2000L
) {
    private val lastUpdateTimes = ConcurrentHashMap<Int, Long>()
    private val processedTexts = ConcurrentHashMap<Int, String>()

    fun extractTextNodes(rootNode: AccessibilityNodeInfo): List<NodeInfo> {
        val result = mutableListOf<NodeInfo>()
        extractTextNodesRecursive(rootNode, result)
        return result
    }
    
    private fun extractTextNodesRecursive(node: AccessibilityNodeInfo, result: MutableList<NodeInfo>) {
        if (!node.isVisibleToUser) {
            return
        }
        
        // Check if this node has text
        val text = node.text?.toString()
        if (!text.isNullOrBlank()) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            
            // Skip nodes that are too large
            if (!isNodeTooLarge(bounds)) {
                result.add(NodeInfo(
                    node = node,
                    text = text,
                    bounds = bounds
                ))
            }
        }
        
        // Process children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            extractTextNodesRecursive(child, result)
            child.recycle()
        }
    }
    
    fun shouldProcessNode(nodeInfo: NodeInfo): Boolean {
        return nodeInfo.node.isVisibleToUser && !isNodeTooLarge(nodeInfo.bounds)
    }
    
    fun shouldTranslate(nodeId: Int, text: String): Boolean {
        val lastUpdate = lastUpdateTimes[nodeId] ?: 0L
        val now = SystemClock.elapsedRealtime()
        
        // Check if time to update based on debounce
        if (now - lastUpdate > debounceInterval) {
            // Check if text has changed
            val previousText = processedTexts[nodeId]
            if (previousText != text) {
                lastUpdateTimes[nodeId] = now
                processedTexts[nodeId] = text
                return true
            }
        }
        
        return false
    }

    fun resetNode(nodeId: Int) {
        lastUpdateTimes.remove(nodeId)
        processedTexts.remove(nodeId)
    }
    
    fun findVisibleNodes(rootNode: AccessibilityNodeInfo): Set<Int> {
        val result = mutableSetOf<Int>()
        findVisibleNodesRecursive(rootNode, result)
        return result
    }
    
    private fun findVisibleNodesRecursive(node: AccessibilityNodeInfo, result: MutableSet<Int>) {
        if (node.isVisibleToUser) {
            result.add(node.hashCode())
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findVisibleNodesRecursive(child, result)
            child.recycle()
        }
    }
    
    fun periodicRefreshFlow(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(refreshInterval)
        }
    }
    
    private fun isNodeTooLarge(bounds: Rect): Boolean {
        val displayMetrics = systemInfoProvider.getDisplayMetrics()
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        val nodeArea = bounds.width() * bounds.height()
        val screenArea = screenWidth * screenHeight
        
        val ratio = nodeArea.toDouble() / screenArea
        return ratio > largeNodeThreshold
    }
}
