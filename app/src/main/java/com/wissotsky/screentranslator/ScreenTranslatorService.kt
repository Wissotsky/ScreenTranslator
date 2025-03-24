package com.wissotsky.screentranslator

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.view.WindowManager
import android.content.Context
import android.graphics.text.LineBreaker
import android.view.Gravity
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import android.widget.FrameLayout
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.os.SystemClock
import android.text.Layout
import java.util.Timer
import java.util.TimerTask
import androidx.core.util.Pools
import com.wissotsky.screentranslator.data.PreferencesRepository
import com.wissotsky.screentranslator.di.ServiceLocator
import com.wissotsky.screentranslator.domain.TextProcessor
import com.wissotsky.screentranslator.domain.TranslationEngine
import com.wissotsky.screentranslator.ui.overlay.OverlayManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow

class ScreenTranslatorService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var overlayManager: OverlayManager
    private lateinit var textProcessor: TextProcessor
    private lateinit var translationEngine: TranslationEngine
    private lateinit var preferencesRepository: PreferencesRepository
    
    private val TAG = "ScreenTranslatorService"

    override fun onCreate() {
        super.onCreate()
        
        // Get dependencies from service locator
        val serviceLocator = (application as ScreenTranslatorApplication).serviceLocator
        translationEngine = serviceLocator.provideTranslationEngine()
        textProcessor = serviceLocator.provideTextProcessor()
        overlayManager = serviceLocator.provideOverlayManager(this,textProcessor)
        preferencesRepository = serviceLocator.providePreferencesRepository()
        
        // Observe configuration changes
        serviceScope.launch {
            preferencesRepository.translationConfig.collect { config ->
                translationEngine.updateConfiguration(config)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
        overlayManager.initialize()
        
        // Start periodic refresh
        serviceScope.launch {
            textProcessor.periodicRefreshFlow().collect {
                refreshAllTranslations()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    serviceScope.launch {
                        processScreenContent(rootNode)
                    }
                }
            }
        }
    }

    private fun processScreenContent(rootNode: AccessibilityNodeInfo) {
        val textNodes = textProcessor.extractTextNodes(rootNode)
        
        textNodes.forEach { nodeInfo ->
            if (textProcessor.shouldProcessNode(nodeInfo)) {
                val nodeId = nodeInfo.node.hashCode()
                val text = nodeInfo.text
                
                // Show preview immediately
                overlayManager.showPreview(nodeId, text, nodeInfo.bounds)
                
                // Translate if needed
                if (textProcessor.shouldTranslate(nodeId, text)) {
                    translateText(nodeId, text, nodeInfo.bounds)
                }
            }
        }
    }

    private fun translateText(nodeId: Int, text: String, bounds: Rect) {
        serviceScope.launch {
            try {
                val translatedText = translationEngine.translate(text)
                overlayManager.showTranslation(nodeId, translatedText, bounds)
            } catch (e: Exception) {
                Log.e(TAG, "Translation failed", e)
            }
        }
    }

    private fun refreshAllTranslations() {
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            serviceScope.launch {
                val visibleNodes = textProcessor.findVisibleNodes(rootNode)
                overlayManager.removeInvisibleOverlays(visibleNodes)
                processScreenContent(rootNode)
            }
        }
    }

    override fun onInterrupt() {
        // Service was interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager.cleanup()
        serviceScope.cancel()
    }
}