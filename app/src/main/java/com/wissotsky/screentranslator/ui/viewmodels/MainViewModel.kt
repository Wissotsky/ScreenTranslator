package com.wissotsky.screentranslator.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wissotsky.screentranslator.data.PreferencesRepository
import com.wissotsky.screentranslator.permissions.PermissionsManager
import com.wissotsky.screentranslator.domain.TranslationModelRepository

/**
 * ViewModel for the main screen of the application.
 * Handles permission states and translation model management.
 */
class MainViewModel(
    private val permissionsManager: PermissionsManager,
    private val translationModelRepository: TranslationModelRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    
    // Permission states
    var isAccessibilityServiceInstalled by mutableStateOf(permissionsManager.isAccessibilityServiceInstalled())
    var isOverlayPermissionGranted by mutableStateOf(permissionsManager.isOverlayPermissionGranted())
    
    // Model states
    var downloadedModels by mutableStateOf<Set<String>>(emptySet())
    
    // Language states
    private var _sourceLanguage by mutableStateOf(preferencesRepository.getCurrentConfig().sourceLanguage)
    val sourceLanguage: String get() = _sourceLanguage

    private var _targetLanguage by mutableStateOf(preferencesRepository.getCurrentConfig().targetLanguage)
    val targetLanguage: String get() = _targetLanguage
    
    init {
        refreshPermissions()
        refreshDownloadedModels()
    }
    
    fun refreshPermissions() {
        isAccessibilityServiceInstalled = permissionsManager.isAccessibilityServiceInstalled()
        isOverlayPermissionGranted = permissionsManager.isOverlayPermissionGranted()
    }
    
    fun refreshDownloadedModels() {
        translationModelRepository.getDownloadedModels { models -> 
            downloadedModels = models
        }
    }
    
    fun downloadModel(languageCode: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        translationModelRepository.downloadModel(languageCode, onSuccess, onFailure)
    }
    
    fun deleteModel(languageCode: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        translationModelRepository.deleteModel(languageCode, onSuccess, onFailure)
    }
    
    fun updateSourceLanguage(languageCode: String) {
        preferencesRepository.setSourceLanguage(languageCode)
        _sourceLanguage = languageCode
    }
    
    fun updateTargetLanguage(languageCode: String) {
        preferencesRepository.setTargetLanguage(languageCode)
        _targetLanguage = languageCode
    }
}