package com.wissotsky.screentranslator.di

import android.content.Context
import android.service.autofill.TextValueSanitizer
import com.wissotsky.screentranslator.data.PreferencesRepository
import com.wissotsky.screentranslator.domain.ScreenInfoProvider
import com.wissotsky.screentranslator.domain.SystemInfoProvider
import com.wissotsky.screentranslator.domain.TextProcessor
import com.wissotsky.screentranslator.domain.TranslationEngine
import com.wissotsky.screentranslator.ui.overlay.OverlayManager

/**
 * Provides dependencies for the accessibility service
 */
class ServiceLocator(private val context: Context) {
    
    // Shared repositories
    private val preferencesRepository by lazy {
        PreferencesRepository(context)
    }
    
    // System information providers
    private val systemInfoProvider by lazy {
        SystemInfoProvider(context)
    }
    
    private val screenInfoProvider by lazy {
        ScreenInfoProvider(context)
    }
    
    // Service components
    fun provideTranslationEngine(): TranslationEngine {
        return TranslationEngine(preferencesRepository.getCurrentConfig())
    }
    
    fun provideTextProcessor(): TextProcessor {
        return TextProcessor(systemInfoProvider)
    }
    
    fun provideOverlayManager(context: Context, textProcessor: TextProcessor): OverlayManager {
        return OverlayManager(context, systemInfoProvider, textProcessor)
    }
    
    fun providePreferencesRepository(): PreferencesRepository {
        return preferencesRepository
    }
}
