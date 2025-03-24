package com.wissotsky.screentranslator.domain

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.wissotsky.screentranslator.data.TranslationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TranslationEngine(private var config: TranslationConfig) {
    private var translator: Translator? = null
    private val translationCache = LruCache<String, String>(100) // Cache up to 100 translations
    
    init {
        createTranslator()
    }
    
    private fun createTranslator() {
        // Clean up existing translator
        translator?.close()
        
        // Create new translator with current config
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(config.sourceLanguage)
            .setTargetLanguage(config.targetLanguage)
            .build()
            
        translator = Translation.getClient(options)
        
        // Download model if needed
        ensureModelDownloaded()
    }
    
    fun updateConfiguration(newConfig: TranslationConfig) {
        if (config != newConfig) {
            config = newConfig
            createTranslator()
        }
    }
    
    private fun ensureModelDownloaded() {
        val conditions = DownloadConditions.Builder().build()
        translator?.downloadModelIfNeeded(conditions)
            ?.addOnSuccessListener {
                Log.d(TAG, "Translation model downloaded successfully")
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "Error downloading translation model", e)
            }
    }
    
    suspend fun translate(text: String): String {
        // Check cache first
        translationCache[text]?.let { return it }
        
        return withContext(Dispatchers.IO) {
            try {
                val result = translator?.let { translator ->
                    suspendCancellableCoroutine<String> { continuation ->
                        val task: Task<String> = translator.translate(text)
                        
                        task.addOnSuccessListener { translatedText ->
                            if (continuation.isActive) {
                                continuation.resume(translatedText)
                            }
                        }
                        
                        task.addOnFailureListener { exception ->
                            if (continuation.isActive) {
                                continuation.resumeWithException(exception)
                            }
                        }
                        
                        continuation.invokeOnCancellation {
                            if (!task.isComplete) {
                                // Google Tasks API doesn't have a direct cancel method
                                // Let it complete but we won't use the result as the coroutine is cancelled
                                Log.d(TAG, "Translation task coroutine was cancelled")
                            }
                        }
                    }
                } ?: text
                
                // Cache the result
                translationCache[text] = result
                result
            } catch (e: Exception) {
                Log.e(TAG, "Translation failed", e)
                text // Return original text on error
            }
        }
    }
    
    fun close() {
        translator?.close()
        translator = null
    }
    
    companion object {
        private const val TAG = "TranslationEngine"
    }
}

// Simple LRU Cache implementation
class LruCache<K, V>(private val maxSize: Int) {
    private val cache = LinkedHashMap<K, V>(maxSize / 4, 0.75f, true)
    
    operator fun set(key: K, value: V) {
        synchronized(cache) {
            cache[key] = value
            trimToSize()
        }
    }
    
    operator fun get(key: K): V? {
        synchronized(cache) {
            return cache[key]
        }
    }
    
    private fun trimToSize() {
        synchronized(cache) {
            while (cache.size > maxSize) {
                val firstKey = cache.keys.firstOrNull() ?: break
                cache.remove(firstKey)
            }
        }
    }
    
    fun clear() {
        synchronized(cache) {
            cache.clear()
        }
    }
}
