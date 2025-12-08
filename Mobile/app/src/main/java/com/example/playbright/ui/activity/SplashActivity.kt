package com.example.playbright.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.playbright.R
import com.example.playbright.data.repository.AuthRepository

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    
    private val repository = AuthRepository()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Hide system UI for immersive experience
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthAndNavigate()
        }, 2000)
    }
    
    private fun checkAuthAndNavigate() {
        val intent = if (repository.isUserLoggedIn()) {
            // For students, go to StudentHomeActivity
            // For faculty/admin, go to MainActivity (web access only)
            Intent(this, StudentHomeActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        
        startActivity(intent)
        finish()
    }
}

