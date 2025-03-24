package com.wissotsky.screentranslator.domain

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager

/**
 * Provider for system-level information needed by the application.
 */
class SystemInfoProvider(private val context: Context) {
    
    private var statusBarHeight = 0
    private var navigationBarHeight = 0
    
    init {
        updateInsets()
    }
    
    /**
     * Updates system insets information.
     */
    fun updateInsets() {
        // Get status bar height
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        
        // Get navigation bar height if available
        val navResourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (navResourceId > 0) {
            navigationBarHeight = context.resources.getDimensionPixelSize(navResourceId)
        }
        
        // For Android R and above, we can also use WindowInsets
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars()
            )
            statusBarHeight = insets.top
            navigationBarHeight = insets.bottom
        }
    }
    
    /**
     * Gets the status bar height in pixels.
     */
    fun getStatusBarHeight(): Int {
        return statusBarHeight
    }
    
    /**
     * Gets the navigation bar height in pixels.
     */
    fun getNavigationBarHeight(): Int {
        return navigationBarHeight
    }
    
    /**
     * Gets the display metrics for the current display.
     */
    fun getDisplayMetrics(): DisplayMetrics {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        
        @Suppress("DEPRECATION")
        val display = windowManager.defaultDisplay
        @Suppress("DEPRECATION")
        display.getRealMetrics(displayMetrics)
        
        return displayMetrics
    }
    
    /**
     * Gets the screen width in pixels.
     */
    fun getScreenWidth(): Int {
        return getDisplayMetrics().widthPixels
    }
    
    /**
     * Gets the screen height in pixels.
     */
    fun getScreenHeight(): Int {
        return getDisplayMetrics().heightPixels
    }
    
    /**
     * Gets the screen density.
     */
    fun getScreenDensity(): Float {
        return getDisplayMetrics().density
    }
    
    /**
     * Convert dp to pixels.
     */
    fun dpToPx(dp: Float): Int {
        return (dp * getScreenDensity() + 0.5f).toInt()
    }
    
    /**
     * Convert pixels to dp.
     */
    fun pxToDp(px: Int): Float {
        return px / getScreenDensity()
    }
}