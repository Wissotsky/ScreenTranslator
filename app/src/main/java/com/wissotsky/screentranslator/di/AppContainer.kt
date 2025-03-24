package com.wissotsky.screentranslator.di

import android.content.Context
import com.google.mlkit.common.model.RemoteModelManager
import com.wissotsky.screentranslator.data.PreferencesRepository
import com.wissotsky.screentranslator.domain.TranslationModelRepository
import com.wissotsky.screentranslator.permissions.PermissionsManager

/**
 * Container for dependencies used by the UI layer
 */
class AppContainer(private val context: Context) {
    
    // ML Kit model manager
    private val modelManager by lazy {
        RemoteModelManager.getInstance()
    }
    
    // Repositories
    val translationModelRepository: TranslationModelRepository by lazy {
        TranslationModelRepository(modelManager)
    }
    
    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(context)
    }
    
    // Utility classes
    val permissionsManager: PermissionsManager by lazy {
        PermissionsManager(context)
    }
}
