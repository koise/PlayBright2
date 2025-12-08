package com.example.playbright.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.playbright.databinding.ActivityMainBinding
import com.example.playbright.ui.viewmodel.AuthViewModel

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupObservers() {
        viewModel.currentUser.observe(this) { user ->
            if (user != null) {
                binding.tvWelcome.text = "Welcome, ${user.displayName}!"
                binding.tvEmail.text = user.email
            } else {
                navigateToLogin()
            }
        }
        
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.SignedOut -> {
                    navigateToLogin()
                }
                else -> {}
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnSignOut.setOnClickListener {
            viewModel.signOut()
        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

