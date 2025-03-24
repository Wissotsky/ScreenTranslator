package com.wissotsky.screentranslator

import android.app.Application
import com.wissotsky.screentranslator.di.AppContainer
import com.wissotsky.screentranslator.di.ServiceLocator

class ScreenTranslatorApplication : Application() {
    
    // Dependency containers
    lateinit var appContainer: AppContainer
        private set
        
    lateinit var serviceLocator: ServiceLocator
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize dependency containers
        appContainer = AppContainer(applicationContext)
        serviceLocator = ServiceLocator(applicationContext)
    }
}
