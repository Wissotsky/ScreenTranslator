package com.wissotsky.screentranslator.permissions

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import com.wissotsky.screentranslator.ScreenTranslatorService

/**
 * A manager class to handle permission checking and requesting functionality.
 */
class PermissionsManager(private val context: Context) {
    
    companion object {
        private const val TAG = "PermissionsManager"
    }
    
    /**
     * Checks if the accessibility service is installed.
     */
    fun isAccessibilityServiceInstalled(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val installedServices = am.getInstalledAccessibilityServiceList()
        val myServiceComponentName = ComponentName(context, ScreenTranslatorService::class.java)
        
        for (installedService in installedServices) {
            val installedServiceComponentName = ComponentName(
                installedService.resolveInfo.serviceInfo.packageName,
                installedService.resolveInfo.serviceInfo.name
            )
            Log.d(TAG,"accessibility checked: ${installedServiceComponentName.flattenToString()} installed: ${myServiceComponentName.flattenToString()}")
            
            if (installedServiceComponentName == myServiceComponentName) {
                Log.d(TAG, "Accessibility service is installed: ${myServiceComponentName.flattenToString()}")
                return true
            }
        }
        
        return false
    }
    
    /**
     * Checks if overlay permission is granted.
     */
    fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(context)
    }
    
    /**
     * Checks if all required permissions are granted.
     */
    fun areAllPermissionsGranted(): Boolean {
        return isAccessibilityServiceInstalled() && isOverlayPermissionGranted()
    }
}