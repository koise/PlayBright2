package com.example.playbright.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.playbright.R
import com.example.playbright.databinding.ActivityCategorySelectionBinding
import com.example.playbright.ui.viewmodel.CategorySelectionViewModel

class CategorySelectionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCategorySelectionBinding
    private val viewModel: CategorySelectionViewModel by viewModels()
    private var isProcessingAnswer = false
    
    private val imageViews = mutableListOf<android.widget.ImageView>()
    private val imageCards = mutableListOf<com.google.android.material.card.MaterialCardView>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityCategorySelectionBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            val moduleIdFromIntent = intent.getStringExtra("MODULE_ID") ?: "category-selection"
            viewModel.setModuleId(moduleIdFromIntent)
            
            setupImageViews()
            setupObservers()
            setupClickListeners()
            
            binding.layoutQuiz.visibility = View.VISIBLE
            binding.layoutCompletion.visibility = View.GONE
            
            viewModel.loadQuestions()
        } catch (e: Exception) {
            android.util.Log.e("CategorySelection", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading activity: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupImageViews() {
        imageViews.addAll(listOf(
            binding.ivImage1, binding.ivImage2, binding.ivImage3, binding.ivImage4,
            binding.ivImage5, binding.ivImage6, binding.ivImage7, binding.ivImage8
        ))
        imageCards.addAll(listOf(
            binding.cardImage1, binding.cardImage2, binding.cardImage3, binding.cardImage4,
            binding.cardImage5, binding.cardImage6, binding.cardImage7, binding.cardImage8
        ))
    }
    
    private fun setupObservers() {
        viewModel.currentQuestion.observe(this) { question ->
            question?.let {
                updateQuestionUI(it)
            }
        }
        
        viewModel.score.observe(this) { score ->
            binding.tvScore.text = "Score: $score"
        }
        
        viewModel.currentQuestionIndex.observe(this) {
            updateQuestionCounter()
        }
        
        viewModel.originalTotalQuestions.observe(this) {
            updateQuestionCounter()
        }
        
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBarLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                if (it.contains("No questions available")) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 3000)
                }
            }
        }
        
        viewModel.isCompleted.observe(this) { isCompleted ->
            if (isCompleted) {
                showCompletionScreen()
            }
        }
    }
    
    private fun updateQuestionCounter() {
        val currentIndex = viewModel.currentQuestionIndex.value ?: 0
        val total = viewModel.originalTotalQuestions.value ?: 0
        binding.tvQuestionNumber.text = "Question ${currentIndex + 1}/$total"
    }
    
    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Set click listeners for image cards
        imageCards.forEachIndexed { index, card ->
            card.setOnClickListener {
                if (!isProcessingAnswer) {
                    viewModel.toggleSelection(index)
                    updateSelectionUI(index)
                }
            }
        }
        
        binding.btnCheck.setOnClickListener {
            if (!isProcessingAnswer) {
                checkAnswer()
            }
        }
        
        binding.btnNext.setOnClickListener {
            viewModel.nextQuestion()
            binding.btnNext.visibility = View.GONE
            binding.btnCheck.visibility = View.VISIBLE
            resetSelectionUI()
        }
        
        binding.btnPlayAgain.setOnClickListener {
            viewModel.restartQuiz()
            binding.layoutCompletion.visibility = View.GONE
            binding.layoutQuiz.visibility = View.VISIBLE
            resetSelectionUI()
        }
        
        binding.btnHome.setOnClickListener {
            finish()
        }
    }
    
    private fun updateQuestionUI(question: com.example.playbright.data.model.CategorySelectionResponse) {
        // Show question text (use audioText if available, otherwise question)
        val questionText = question.audioText ?: question.question
        binding.tvQuestion.text = questionText
        android.util.Log.d("CategorySelection", "Question: $questionText, Category: ${question.category}")
        
        // Load images from images array (backend structure)
        val images = question.images ?: emptyList()
        val maxImages = minOf(images.size, 8) // Support up to 8 images
        
        android.util.Log.d("CategorySelection", "Loading ${images.size} images")
        
        for (i in 0 until maxImages) {
            val image = images[i]
            val imageUrl = image.url
            
            if (imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_error_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(imageViews[i])
                android.util.Log.d("CategorySelection", "Loaded image $i: $imageUrl (isCorrect: ${image.isCorrect})")
            } else {
                imageViews[i].setImageResource(R.drawable.ic_placeholder_image)
            }
            
            imageCards[i].visibility = View.VISIBLE
        }
        
        // Hide unused image cards
        for (i in maxImages until 8) {
            imageCards[i].visibility = View.GONE
        }
        
        resetSelectionUI()
    }
    
    private fun updateSelectionUI(index: Int) {
        val isSelected = viewModel.isSelected(index)
        val card = imageCards[index]
        
        if (isSelected) {
            card.strokeWidth = 6
            card.setStrokeColor(ContextCompat.getColor(this, R.color.primary_blue))
            card.elevation = 8f
            card.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        } else {
            card.strokeWidth = 0
            card.setStrokeColor(ContextCompat.getColor(this, android.R.color.transparent))
            card.elevation = 2f
        }
    }
    
    private fun resetSelectionUI() {
        imageCards.forEach { card ->
            card.strokeWidth = 0
            card.setStrokeColor(ContextCompat.getColor(this, android.R.color.transparent))
            card.elevation = 2f
        }
    }
    
    private fun checkAnswer() {
        if (isProcessingAnswer) return
        
        isProcessingAnswer = true
        val isCorrect = viewModel.checkAnswer()
        
        val currentQuestion = viewModel.currentQuestion.value ?: return
        
        // Get correct indices from images array (backend structure)
        val images = currentQuestion.images ?: emptyList()
        val correctIndices = images.mapIndexedNotNull { index, image ->
            if (image.isCorrect) index else null
        }.toSet()
        
        android.util.Log.d("CategorySelection", "Showing feedback - Correct indices: $correctIndices")
        
        // Show visual feedback
        imageCards.forEachIndexed { index, card ->
            if (index < images.size) {
                val isSelected = viewModel.isSelected(index)
                val isCorrectAnswer = correctIndices.contains(index)
                
                if (isCorrectAnswer) {
                    // Highlight correct answers in green
                    card.strokeWidth = 6
                    card.setStrokeColor(ContextCompat.getColor(this, R.color.correct_answer))
                } else if (isSelected && !isCorrectAnswer) {
                    // Highlight wrong selections in red
                    card.strokeWidth = 6
                    card.setStrokeColor(ContextCompat.getColor(this, R.color.wrong_answer))
                }
            }
        }
        
        if (isCorrect) {
            showSuccessFeedback()
            binding.btnNext.visibility = View.VISIBLE
            binding.btnCheck.visibility = View.GONE
        } else {
            showErrorFeedback()
            // Allow retry - keep check button visible
        }
        
        Handler(Looper.getMainLooper()).postDelayed({
            isProcessingAnswer = false
        }, 1000)
    }
    
    private fun showSuccessFeedback() {
        // Success animation (star system removed)
    }
    
    private fun showErrorFeedback() {
        Toast.makeText(this, "Not quite right. Try again!", Toast.LENGTH_SHORT).show()
    }
    
    private fun showCompletionScreen() {
        binding.layoutQuiz.visibility = View.GONE
        binding.layoutCompletion.visibility = View.VISIBLE
        
        val statistics = viewModel.getStatistics()
        val correct = statistics["correct"] as Int
        val wrong = statistics["wrong"] as Int
        val totalAttempted = statistics["totalAttempted"] as Int
        val percentage = statistics["percentage"] as Int
        val score = statistics["score"] as Int
        binding.tvFinalScore.text = "Module Complete!"
        binding.tvScoreText.text = "You scored $score out of $totalAttempted!"
        
        binding.tvCorrectCount.text = "$correct"
        binding.tvWrongCount.text = "$wrong"
        binding.tvAccuracy.text = "$percentage%"
        
        viewModel.markModuleAsCompleted()
        
        val animation = AnimationUtils.loadAnimation(this, R.anim.celebration)
        binding.ivTrophy.startAnimation(animation)
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }
}

