package com.example.playbright

import android.app.Application
import com.example.playbright.data.network.RetrofitClient

class PlayBrightApplication : Application() {
    
    companion object {
        @Volatile
        private var appInstance: PlayBrightApplication? = null
        
        fun getInstance(): PlayBrightApplication {
            return appInstance ?: throw IllegalStateException("Application not initialized")
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        appInstance = this
        
        // Initialize Retrofit with context for network checking
        RetrofitClient.initialize(this)
    }
}

