package com.example.playbright.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.playbright.R
import com.example.playbright.data.model.ModuleResponse
import com.example.playbright.data.repository.AuthRepository
import com.example.playbright.databinding.ActivityStudentHomeBinding
import com.example.playbright.databinding.NavDrawerHeaderBinding
import com.example.playbright.ui.adapter.ModuleAdapter
import com.example.playbright.ui.viewmodel.ModulesViewModel
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class StudentHomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    
    private lateinit var binding: ActivityStudentHomeBinding
    private lateinit var navHeaderBinding: NavDrawerHeaderBinding
    private val viewModel: ModulesViewModel by viewModels()
    private lateinit var moduleAdapter: ModuleAdapter
    private val authRepository = AuthRepository()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupDrawer()
        setupBackPressHandler()
        setupRecyclerView()
        setupObservers()
        loadData()
        // progressViewModel.loadProgress() // Will be enabled when StudentProgressViewModel is properly set up
    }
    
    private fun setupDrawer() {
        // Set up navigation view listener
        binding.navView.setNavigationItemSelectedListener(this)
        
        // Set up menu button to open drawer
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        
        // Set up drawer header with user info
        val headerView = binding.navView.getHeaderView(0)
        navHeaderBinding = NavDrawerHeaderBinding.bind(headerView)
        
        // Update user info in drawer header
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            navHeaderBinding.tvUserName.text = user.displayName ?: "Student"
            navHeaderBinding.tvUserEmail.text = user.email ?: ""
        }
    }
    
    private fun setupRecyclerView() {
        moduleAdapter = ModuleAdapter { module ->
            openModule(module)
        }

        // Horizontal scrolling layout for modules
        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvModules.layoutManager = linearLayoutManager
        binding.rvModules.adapter = moduleAdapter
    }
    
    private fun setupObservers() {
        viewModel.modules.observe(this) { modules ->
            moduleAdapter.submitList(modules)
            if (modules.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvModules.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvModules.visibility = View.VISIBLE
            }
            
            // Modules loaded successfully
        }
        
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
    
    private fun updateProgress(modules: List<com.example.playbright.data.model.ModuleResponse>) {
        // Progress tracking removed - no longer needed
    }
    
    private fun loadData() {
        viewModel.loadModules()
    }
    
    private fun openModule(module: ModuleResponse) {
        // Get moduleId from the module, or derive it from moduleNumber if not available
        var moduleId = module.moduleId
        
        // If moduleId is null or empty, try to derive it from moduleNumber
        if (moduleId.isNullOrEmpty()) {
            moduleId = when (module.moduleNumber) {
                1 -> "picture-quiz"
                2 -> "audio-identification"
                3 -> "sequencing-cards"
                4 -> "word-builder"
                5 -> "picture-label"
                6 -> "emotion-recognition"
                7 -> "category-selection"
                8 -> "story-qa"
                9 -> "trace-follow"
                10 -> "yes-no-questions"
                11 -> "tap-repeat"
                12 -> "matching-pairs"
                13 -> "follow-instructions"
                else -> null
            }
        }
        
        if (moduleId.isNullOrEmpty()) {
            Toast.makeText(this, "Module ID not found for: ${module.name}", Toast.LENGTH_SHORT).show()
            android.util.Log.e("StudentHome", "Module ID missing - name: ${module.name}, moduleNumber: ${module.moduleNumber}, moduleId: ${module.moduleId}")
            return
        }
        
        // Launch activity based on moduleId
        try {
            when (moduleId) {
                "picture-quiz" -> {
                    val intent = Intent(this, PictureQuizActivity::class.java)
                    intent.putExtra("MODULE_ID", moduleId)
                    startActivity(intent)
                }
                "audio-identification" -> {
                    val intent = Intent(this, AudioIdentificationActivity::class.java)
                    intent.putExtra("MODULE_ID", moduleId)
                    startActivity(intent)
                }
                "sequencing-cards" -> {
                    val intent = Intent(this, SequencingCardsActivity::class.java)
                    intent.putExtra("MODULE_ID", moduleId)
                    startActivity(intent)
                }
                "picture-label", "picture-labeling" -> {
                    Toast.makeText(this, "Picture Labeling coming soon!", Toast.LENGTH_SHORT).show()
                }
                "emotion-recognition" -> {
                    Toast.makeText(this, "Emotion Recognition coming soon!", Toast.LENGTH_SHORT).show()
                }
                "category-selection" -> {
                    Toast.makeText(this, "Category Selection coming soon!", Toast.LENGTH_SHORT).show()
                }
                "yes-no-questions" -> {
                    Toast.makeText(this, "Yes/No Questions coming soon!", Toast.LENGTH_SHORT).show()
                }
                "tap-repeat" -> {
                    Toast.makeText(this, "Tap & Repeat coming soon!", Toast.LENGTH_SHORT).show()
                }
                "matching-pairs" -> {
                    Toast.makeText(this, "Matching Pairs coming soon!", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Module not available: $moduleId", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StudentHome", "Error launching module: ${e.message}", e)
            Toast.makeText(this, "Error opening module: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation drawer item selection
        when (item.itemId) {
            R.id.nav_home -> {
                // Already on home, just close drawer
            }
            R.id.nav_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_progress -> {
                // TODO: Navigate to progress screen
                Toast.makeText(this, "Progress feature coming soon!", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_achievements -> {
                // TODO: Navigate to achievements screen
                Toast.makeText(this, "Achievements feature coming soon!", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_settings -> {
                // TODO: Navigate to settings screen
                Toast.makeText(this, "Settings feature coming soon!", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_logout -> {
                // Logout user
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    finish()
                }
            }
        })
    }
}

