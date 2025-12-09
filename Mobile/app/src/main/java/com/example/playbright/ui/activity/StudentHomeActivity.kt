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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
        setupSwipeRefresh()
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
        moduleAdapter = ModuleAdapter(
            onModuleClick = { module ->
                openModule(module)
            },
            progressMap = emptyMap()
        )

        // Grid layout with 2 columns for modules
        val gridLayoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 2)
        binding.rvModules.layoutManager = gridLayoutManager
        binding.rvModules.adapter = moduleAdapter
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeColors(
            resources.getColor(R.color.primary_blue, theme),
            resources.getColor(R.color.success_green, theme),
            resources.getColor(R.color.card_orange, theme)
        )
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            android.util.Log.d("StudentHome", "Pull-to-refresh triggered")
            refreshData()
        }
    }
    
    private fun setupObservers() {
        viewModel.modules.observe(this) { modules ->
            android.util.Log.d("StudentHome", "Received ${modules.size} modules from API")
            
            // Filter modules to only show those with existing activities
            // Available games:
            // 1. Choose The Right One (picture-quiz)
            // 2. Audio Identification (audio-identification)
            // 3. Sequencing Cards (sequencing-cards)
            // 4. Picture Labeling (picture-label / picture-labeling)
            // 5. Emotion Recognition Board (emotion-recognition)
            // 6. Choose-the-Category Activity (category-selection)
            // 7. Yes or No Questions (yes-no-questions)
            // 8. Tap & Repeat (tap-repeat)
            // 9. Matching Pairs (matching-pairs)
            // 10. Word Builder (word-builder)
            // 11. Trace-and-Follow (trace-follow)
            val availableModuleIds = setOf(
                "picture-quiz",              // 1. Choose The Right One
                "audio-identification",      // 2. Audio Identification
                "sequencing-cards",          // 3. Sequencing Cards
                "picture-label",             // 4. Picture Labeling
                "picture-labeling",          // 4. Picture Labeling (alias)
                "emotion-recognition",       // 5. Emotion Recognition Board
                "category-selection",        // 6. Choose-the-Category Activity
                "yes-no-questions",         // 7. Yes or No Questions
                "tap-repeat",               // 8. Tap & Repeat
                "matching-pairs",           // 9. Matching Pairs
                "word-builder",             // 10. Word Builder
                "trace-follow"              // 11. Trace-and-Follow
            )
            
            val filteredModules = modules.mapNotNull { module ->
                // Get moduleId - try direct field first, then derive from moduleNumber
                val moduleId = when {
                    !module.moduleId.isNullOrEmpty() -> module.moduleId
                    module.moduleNumber != null -> when (module.moduleNumber) {
                        1 -> "picture-quiz"              // Choose The Right One
                        2 -> "audio-identification"      // Audio Identification
                        3 -> "sequencing-cards"          // Sequencing Cards
                        4 -> "picture-label"             // Picture Labeling
                        5 -> "emotion-recognition"       // Emotion Recognition Board
                        6 -> "category-selection"         // Choose-the-Category Activity
                        7 -> "yes-no-questions"          // Yes or No Questions
                        8 -> "tap-repeat"                // Tap & Repeat
                        9 -> "matching-pairs"            // Matching Pairs
                        10 -> "word-builder"            // Word Builder
                        11 -> "trace-follow"            // Trace-and-Follow
                        else -> null
                    }
                    else -> null
                }
                
                val isAvailable = availableModuleIds.contains(moduleId)
                android.util.Log.d("StudentHome", "Module: ${module.name}, moduleId: ${module.moduleId}, moduleNumber: ${module.moduleNumber}, derivedId: $moduleId, available: $isAvailable")
                
                if (isAvailable && moduleId != null) {
                    // Return Pair of moduleId and module for duplicate removal
                    Pair(moduleId, module)
                } else {
                    null
                }
            }
            // Remove duplicates by moduleId (keep first occurrence)
            .distinctBy { it.first }
            .map { it.second }
            
            android.util.Log.d("StudentHome", "Filtered to ${filteredModules.size} available modules (duplicates removed)")
            filteredModules.forEach { module ->
                android.util.Log.d("StudentHome", "Available module: ${module.name} (${module.moduleId})")
            }
            
            // Update adapter with progress data
            val progressMap = viewModel.progressMap.value ?: emptyMap()
            moduleAdapter.updateProgress(progressMap)
            moduleAdapter.submitList(filteredModules)
            
            if (filteredModules.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvModules.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvModules.visibility = View.VISIBLE
            }
            
            // Stop refresh indicator when modules are loaded
            binding.swipeRefreshLayout.isRefreshing = false
            android.util.Log.d("StudentHome", "Modules loaded, refresh indicator stopped")
        }
        
        viewModel.progressMap.observe(this) { progressMap ->
            // Update adapter when progress changes
            moduleAdapter.updateProgress(progressMap)
            // Stop refresh indicator when progress is loaded
            binding.swipeRefreshLayout.isRefreshing = false
        }
        
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            // Don't show refresh indicator if initial loading (only for pull-to-refresh)
            if (!isLoading && !binding.swipeRefreshLayout.isRefreshing) {
                // Data loaded, ensure refresh indicator is stopped
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private fun updateProgress(modules: List<com.example.playbright.data.model.ModuleResponse>) {
        // Progress tracking removed - no longer needed
    }
    
    private fun loadData() {
        viewModel.loadModules()
        viewModel.loadProgress()
    }
    
    private fun refreshData() {
        android.util.Log.d("StudentHome", "Refreshing data...")
        viewModel.refreshModules()
        // Progress will be loaded by refreshModules, but we also call it explicitly
        viewModel.loadProgress()
    }
    
    private fun openModule(module: ModuleResponse) {
        // Get moduleId from the module, or derive it from moduleNumber if not available
        var moduleId = module.moduleId
        
            // If moduleId is null or empty, try to derive it from moduleNumber
            // Available games:
            // 1. Choose The Right One (picture-quiz)
            // 2. Audio Identification (audio-identification)
            // 3. Sequencing Cards (sequencing-cards)
                // 4. Word Builder (word-builder)
                // 5. Picture Labeling (picture-label)
                // 6. Emotion Recognition Board (emotion-recognition)
                // 7. Choose-the-Category Activity (category-selection)
                // 8. Yes or No Questions (yes-no-questions)
                // 9. Tap & Repeat (tap-repeat)
                // 10. Matching Pairs (matching-pairs)
                // 11. Trace-and-Follow (trace-follow)
            if (moduleId.isNullOrEmpty()) {
                moduleId = when (module.moduleNumber) {
                    1 -> "picture-quiz"              // Choose The Right One
                    2 -> "audio-identification"      // Audio Identification
                    3 -> "sequencing-cards"          // Sequencing Cards
                    4 -> "word-builder"              // Word Builder
                    5 -> "picture-label"             // Picture Labeling
                    6 -> "emotion-recognition"       // Emotion Recognition Board
                    7 -> "category-selection"         // Choose-the-Category Activity
                    8 -> "yes-no-questions"          // Yes or No Questions
                    9 -> "tap-repeat"                // Tap & Repeat
                    10 -> "matching-pairs"           // Matching Pairs
                    11 -> "trace-follow"             // Trace-and-Follow
                else -> null
            }
        }
        
        if (moduleId.isNullOrEmpty()) {
            Toast.makeText(this, "Module ID not found for: ${module.name}", Toast.LENGTH_SHORT).show()
            android.util.Log.e("StudentHome", "Module ID missing - name: ${module.name}, moduleNumber: ${module.moduleNumber}, moduleId: ${module.moduleId}")
            return
        }
        
        // Launch activity based on moduleId
        // All 6 games fetch data from admin backend via their respective ViewModels
        try {
            when (moduleId) {
                // 1. Choose The Right One
                "picture-quiz" -> {
                    val intent = Intent(this, PictureQuizActivity::class.java)
                    intent.putExtra("MODULE_ID", moduleId)
                    startActivity(intent)
                }
                // 2. Audio Identification
                "audio-identification" -> {
                    val intent = Intent(this, AudioIdentificationActivity::class.java)
                    intent.putExtra("MODULE_ID", moduleId)
                    startActivity(intent)
                }
                // 3. Sequencing Cards
                "sequencing-cards" -> {
                    val intent = Intent(this, SequencingCardsActivity::class.java)
                    intent.putExtra("MODULE_ID", moduleId)
                    startActivity(intent)
                }
                // 4. Picture Labeling
                "picture-label", "picture-labeling" -> {
                    val intent = Intent(this, com.example.playbright.ui.activity.PictureLabelingActivity::class.java)
                    intent.putExtra("MODULE_ID", moduleId)
                    startActivity(intent)
                }
                // 5. Emotion Recognition Board
                "emotion-recognition" -> {
                    val intent = Intent(this, EmotionRecognitionActivity::class.java)
                    intent.putExtra("MODULE_ID", moduleId)
                    startActivity(intent)
                }
                // 6. Choose-the-Category Activity
                "category-selection" -> {
                    val intent = Intent(this, CategorySelectionActivity::class.java)
                    intent.putExtra("MODULE_ID", moduleId)
                    startActivity(intent)
                }
                // 7. Yes or No Questions
                "yes-no-questions" -> {
                    val intent = Intent(this, YesNoQuestionsActivity::class.java)
                    intent.putExtra("MODULE_ID", moduleId)
                    startActivity(intent)
                }
                // 8. Tap & Repeat
                "tap-repeat" -> {
                    val intent = Intent(this, TapRepeatActivity::class.java)
                    intent.putExtra("MODULE_ID", moduleId)
                    startActivity(intent)
                }
                // 9. Matching Pairs
                "matching-pairs" -> {
                    val intent = Intent(this, MatchingPairsActivity::class.java)
                    intent.putExtra("MODULE_ID", moduleId)
                    startActivity(intent)
                }
                // 10. Word Builder
                "word-builder" -> {
                    val intent = Intent(this, WordBuilderActivity::class.java)
                    intent.putExtra("MODULE_ID", moduleId)
                    startActivity(intent)
                }
                // 11. Trace-and-Follow
                "trace-follow" -> {
                    val intent = Intent(this, TraceAndFollowActivity::class.java)
                    intent.putExtra("MODULE_ID", moduleId)
                    startActivity(intent)
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
            R.id.nav_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
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

