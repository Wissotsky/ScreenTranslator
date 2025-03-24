package com.wissotsky.screentranslator.ui.overlay

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.util.Pools
import com.wissotsky.screentranslator.domain.SystemInfoProvider
import com.wissotsky.screentranslator.domain.TextProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class OverlayManager(
    private val context: Context,
    private val systemInfoProvider: SystemInfoProvider,
    private val textProcessor: TextProcessor
) {
    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private lateinit var overlayContainer: FrameLayout
    
    // View management
    private val textViewPool = Pools.SimplePool<TextView>(50)
    private val previewViews = ConcurrentHashMap<Int, TextView>()
    private val translatedViews = ConcurrentHashMap<Int, TextView>()
    
    // Constants
    private val TEXT_COLOR_PENDING = 0xFFF6F193.toInt()
    private val TEXT_COLOR_TRANSLATED = 0xFFC5EBAA.toInt()
    private val BACKGROUND_COLOR = 0xFF2E2E2E.toInt()

    fun initialize() {
        createOverlayContainer()
        systemInfoProvider.updateInsets()
    }

    fun showPreview(nodeId: Int, text: String, bounds: Rect) {
        //Log.d("OverlayManager", "Showing preview for $nodeId with text $text at $bounds")

        uiScope.launch {
            val existingView = previewViews[nodeId]
            if (existingView != null) {
                //Log.d("OverlayManager", "Updating existing preview for $nodeId with text $text at $bounds")
                updateTextView(existingView, text, bounds)
            } else {
                // Check if the view is already translated
                val translatedView = translatedViews[nodeId]
                if (translatedView != null) {
                    // Update the position of the translated view
                    updateTextView(translatedView, translatedView.text.toString(), bounds)
                } else {
                    val newView = obtainTextView().apply {
                        this.text = text
                        this.setTextColor(TEXT_COLOR_PENDING)
                        //Log.d("OverlayManager", "Creating new preview for $nodeId with text $text at $bounds")
                    }
                    updateTextView(newView, text, bounds)
                    previewViews[nodeId] = newView
                    overlayContainer.addView(newView)
                }
            }
        }
    }
    
    fun showTranslation(nodeId: Int, translatedText: String, bounds: Rect) {
        uiScope.launch {
            // Get existing view or create new one
            val existingPreview = previewViews.remove(nodeId)
            val translationView = if (existingPreview != null) {
                existingPreview
            } else {
                obtainTextView()
            }
            
            // Update the view
            translationView.text = translatedText
            translationView.setTextColor(TEXT_COLOR_TRANSLATED)
            updateTextView(translationView, translatedText, bounds)
            
            // Manage collections
            translatedViews[nodeId] = translationView
            
            // Ensure it's in view hierarchy
            if (existingPreview == null) {
                overlayContainer.addView(translationView)
            }
        }
    }
    
    fun removeInvisibleOverlays(visibleNodeIds: Set<Int>) {
        uiScope.launch {
            // Remove previews for invisible nodes
            previewViews.keys
                .filter { it !in visibleNodeIds }
                .forEach { removeOverlay(it) }
            
            // Remove translations for invisible nodes
            translatedViews.keys
                .filter { it !in visibleNodeIds }
                .forEach { removeOverlay(it) }
        }
    }
    
    fun removeOverlay(nodeId: Int) {
        uiScope.launch {
            previewViews.remove(nodeId)?.let {
                overlayContainer.removeView(it)
                recycleTextView(it)
            }
            
            translatedViews.remove(nodeId)?.let {
                overlayContainer.removeView(it)
                recycleTextView(it)
            }
            // Reset node state
            textProcessor.resetNode(nodeId)
        }
    }
    
    fun cleanup() {
        uiScope.launch {
            // Remove all views
            previewViews.values.forEach { recycleTextView(it) }
            translatedViews.values.forEach { recycleTextView(it) }
            
            // Clear collections
            previewViews.clear()
            translatedViews.clear()
            
            // Remove container
            windowManager.removeView(overlayContainer)
        }
    }
    
    private fun createOverlayContainer() {
        overlayContainer = FrameLayout(context)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            -3
        )
        windowManager.addView(overlayContainer, params)
    }
    
    private fun obtainTextView(): TextView {
        return textViewPool.acquire() ?: createTextView()
    }
    
    private fun recycleTextView(textView: TextView) {
        textView.text = ""
        textView.setTextColor(TEXT_COLOR_PENDING)
        textView.visibility = View.GONE
        textViewPool.release(textView)
    }
    
    private fun createTextView(): TextView {
        return TextView(context).apply {
            setShadowLayer(2.5f, 0f, 0f, 0xFF000000.toInt())
            setTextColor(TEXT_COLOR_PENDING)
            setBackgroundColor(BACKGROUND_COLOR)
        }
    }
    
    private fun updateTextView(view: TextView, text: String, bounds: Rect) {
        //Log.d("OverlayManager", "Updating text view with text $text at $bounds")
        view.text = text
        view.visibility = View.VISIBLE
        
        val layoutParams = view.layoutParams as? FrameLayout.LayoutParams 
            ?: FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            
        layoutParams.width = bounds.width()
        layoutParams.height = bounds.height()
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.leftMargin = bounds.left
        layoutParams.topMargin = bounds.top - systemInfoProvider.getStatusBarHeight()
        
        view.layoutParams = layoutParams
    }
}
