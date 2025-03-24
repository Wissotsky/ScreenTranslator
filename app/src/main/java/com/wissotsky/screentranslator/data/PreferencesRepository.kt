package com.wissotsky.screentranslator.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing application preferences.
 */
class PreferencesRepository(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "screen_translator_preferences"
        private const val KEY_SOURCE_LANGUAGE = "source_language"
        private const val KEY_TARGET_LANGUAGE = "target_language"
        private const val KEY_ENABLE_AUTO_DETECT = "enable_auto_detect"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_OVERLAY_OPACITY = "overlay_opacity"
        
        // Default values
        private const val DEFAULT_SOURCE_LANGUAGE = "he"
        private const val DEFAULT_TARGET_LANGUAGE = "en"
        private const val DEFAULT_ENABLE_AUTO_DETECT = true
        private const val DEFAULT_TEXT_SIZE = 16f
        private const val DEFAULT_OVERLAY_OPACITY = 0.8f
    }
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    // StateFlow to emit configuration changes
    private val _translationConfig = MutableStateFlow(getCurrentConfig())
    val translationConfig: StateFlow<TranslationConfig> = _translationConfig.asStateFlow()
    
    // Preference change listener
    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _translationConfig.value = getCurrentConfig()
    }
    
    init {
        // Register listener for preference changes
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
    
    /**
     * Gets the current translation configuration.
     */
    fun getCurrentConfig(): TranslationConfig {
        return TranslationConfig(
            sourceLanguage = preferences.getString(KEY_SOURCE_LANGUAGE, DEFAULT_SOURCE_LANGUAGE)!!,
            targetLanguage = preferences.getString(KEY_TARGET_LANGUAGE, DEFAULT_TARGET_LANGUAGE)!!,
            enableAutoDetect = preferences.getBoolean(KEY_ENABLE_AUTO_DETECT, DEFAULT_ENABLE_AUTO_DETECT),
            textSize = preferences.getFloat(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE),
            overlayOpacity = preferences.getFloat(KEY_OVERLAY_OPACITY, DEFAULT_OVERLAY_OPACITY)
        )
    }
    
    /**
     * Updates the source language preference.
     */
    fun setSourceLanguage(languageCode: String) {
        preferences.edit {
            putString(KEY_SOURCE_LANGUAGE, languageCode)
        }
    }
    
    /**
     * Updates the target language preference.
     */
    fun setTargetLanguage(languageCode: String) {
        preferences.edit {
            putString(KEY_TARGET_LANGUAGE, languageCode)
        }
    }
    
    /**
     * Updates the auto-detect language preference.
     */
    fun setEnableAutoDetect(enabled: Boolean) {
        preferences.edit {
            putBoolean(KEY_ENABLE_AUTO_DETECT, enabled)
        }
    }
    
    /**
     * Updates the text size preference.
     */
    fun setTextSize(size: Float) {
        preferences.edit {
            putFloat(KEY_TEXT_SIZE, size)
        }
    }
    
    /**
     * Updates the overlay opacity preference.
     */
    fun setOverlayOpacity(opacity: Float) {
        preferences.edit {
            putFloat(KEY_OVERLAY_OPACITY, opacity)
        }
    }
}

/**
 * Data class representing the translation configuration.
 */
data class TranslationConfig(
    val sourceLanguage: String,
    val targetLanguage: String,
    val enableAutoDetect: Boolean,
    val textSize: Float,
    val overlayOpacity: Float
)