package com.wissotsky.screentranslator.domain

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel

/**
 * Repository class that handles operations related to translation models.
 */
class TranslationModelRepository(
    private val modelManager: RemoteModelManager = RemoteModelManager.getInstance()
) {
    companion object {
        private const val TAG = "TranslationModelRepo"
    }
    
    /**
     * Retrieves the list of currently downloaded translation models.
     * 
     * @param onResult Callback that receives the set of language codes for downloaded models
     */
    fun getDownloadedModels(onResult: (Set<String>) -> Unit) {
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                val languageCodes = models.map { it.language }.toSet()
                Log.d(TAG, "Downloaded models: $languageCodes")
                onResult(languageCodes)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting downloaded models", exception)
                onResult(emptySet())
            }
    }
    
    /**
     * Downloads a translation model for the specified language.
     * 
     * @param languageCode The language code to download
     * @param onSuccess Callback to be invoked when download completes successfully
     * @param onFailure Callback to be invoked when download fails
     */
    fun downloadModel(
        languageCode: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val model = TranslateRemoteModel.Builder(languageCode).build()
        val conditions = DownloadConditions.Builder().build()
        
        modelManager.download(model, conditions)
            .addOnSuccessListener {
                Log.d(TAG, "Model $languageCode downloaded successfully")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error downloading model $languageCode", exception)
                onFailure(exception)
            }
    }
    
    /**
     * Deletes a translation model for the specified language.
     * 
     * @param languageCode The language code of the model to delete
     * @param onSuccess Callback to be invoked when deletion completes successfully
     * @param onFailure Callback to be invoked when deletion fails
     */
    fun deleteModel(
        languageCode: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val model = TranslateRemoteModel.Builder(languageCode).build()
        
        modelManager.deleteDownloadedModel(model)
            .addOnSuccessListener {
                Log.d(TAG, "Model $languageCode deleted successfully")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error deleting model $languageCode", exception)
                onFailure(exception)
            }
    }
}