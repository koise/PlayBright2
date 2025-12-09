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
import com.example.playbright.databinding.ActivityEmotionRecognitionBinding
import com.example.playbright.ui.viewmodel.EmotionRecognitionViewModel
import com.example.playbright.utils.SoundManager

class EmotionRecognitionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEmotionRecognitionBinding
    private val viewModel: EmotionRecognitionViewModel by viewModels()
    private lateinit var soundManager: SoundManager
    private var isProcessingAnswer = false
    
    private val imageViews = mutableListOf<android.widget.ImageView>()
    private val imageCards = mutableListOf<com.google.android.material.card.MaterialCardView>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityEmotionRecognitionBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            val moduleIdFromIntent = intent.getStringExtra("MODULE_ID") ?: "emotion-recognition"
            viewModel.setModuleId(moduleIdFromIntent)
            
            // Initialize SoundManager
            soundManager = SoundManager.getInstance(this)
            
            setupImageViews()
            setupObservers()
            setupClickListeners()
            
            binding.layoutQuiz.visibility = View.VISIBLE
            binding.layoutCompletion.visibility = View.GONE
            
            // Start background music
            soundManager.startBackgroundMusic()
            
            viewModel.loadQuestions()
        } catch (e: Exception) {
            android.util.Log.e("EmotionRecognition", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading activity: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupImageViews() {
        imageViews.addAll(listOf(
            binding.ivImage1, binding.ivImage2, binding.ivImage3, binding.ivImage4
        ))
        imageCards.addAll(listOf(
            binding.cardImage1, binding.cardImage2, binding.cardImage3, binding.cardImage4
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
        
        // Set click listeners for emotion image cards
        imageCards.forEachIndexed { index, card ->
            card.setOnClickListener {
                if (!isProcessingAnswer) {
                    // Add pulse animation
                    val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse)
                    card.startAnimation(pulseAnim)
                    
                    Handler(Looper.getMainLooper()).postDelayed({
                        viewModel.selectEmotion(index)
                        updateSelectionUI(index)
                    }, 100)
                }
            }
        }
        
        binding.btnNext.setOnClickListener {
            viewModel.nextQuestion()
            binding.btnNext.visibility = View.GONE
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
    
    private fun updateQuestionUI(question: com.example.playbright.data.model.EmotionRecognitionResponse) {
        // Animate question entrance
        binding.tvQuestion.alpha = 0f
        val questionText = question.audioText ?: question.question
        binding.tvQuestion.text = questionText
        binding.tvQuestion.animate()
            .alpha(1f)
            .setDuration(400)
            .start()
        android.util.Log.d("EmotionRecognition", "Question: $questionText")
        
        // Load emotion images from images array (backend structure)
        val images = question.images ?: emptyList()
        
        if (images.isEmpty()) {
            android.util.Log.w("EmotionRecognition", "⚠️ Question has no images: ${question.id}")
            // Hide all image cards if no images
            imageCards.forEach { it.visibility = View.GONE }
            resetSelectionUI()
            return
        }
        
        val maxImages = minOf(images.size, 4)
        android.util.Log.d("EmotionRecognition", "Loading ${images.size} images")
        
        for (i in 0 until maxImages) {
            val image = images[i]
            val imageUrl = image.url
            
            // Hide initially for animation
            imageCards[i].alpha = 0f
            
            if (imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_error_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(imageViews[i])
                android.util.Log.d("EmotionRecognition", "Loaded image $i: $imageUrl (emotion: ${image.emotion}, isCorrect: ${image.isCorrect})")
            } else {
                imageViews[i].setImageResource(R.drawable.ic_placeholder_image)
            }
            
            imageCards[i].visibility = View.VISIBLE
            
            // Animate entrance with stagger
            Handler(Looper.getMainLooper()).postDelayed({
                val popInAnim = AnimationUtils.loadAnimation(this, R.anim.pop_in)
                imageCards[i].startAnimation(popInAnim)
                imageCards[i].animate()
                    .alpha(1f)
                    .setDuration(400)
                    .start()
            }, (i * 100L))
        }
        
        // Hide unused image cards
        for (i in maxImages until 4) {
            imageCards[i].visibility = View.GONE
        }
        
        resetSelectionUI()
    }
    
    private fun updateSelectionUI(index: Int) {
        // Reset all cards first
        imageCards.forEach { card ->
            card.strokeWidth = 0
            card.setStrokeColor(ContextCompat.getColor(this, android.R.color.transparent))
            card.elevation = 2f
        }
        
        // Highlight selected card
        val selectedCard = imageCards[index]
        selectedCard.strokeWidth = 6
        selectedCard.setStrokeColor(ContextCompat.getColor(this, R.color.primary_blue))
        selectedCard.elevation = 8f
        selectedCard.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        
        // Auto-check answer when an emotion is selected
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isProcessingAnswer) {
                checkAnswer()
            }
        }, 300) // Small delay for better UX
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
        val selectedIndex = viewModel.getSelectedEmotionIndex()
        
        // Show visual feedback using images array (backend structure)
        val images = currentQuestion.images ?: emptyList()
        imageCards.forEachIndexed { index, card ->
            if (index < images.size) {
                val image = images[index]
                val isCorrectAnswer = image.isCorrect
                val isSelected = index == selectedIndex
                
                if (isCorrectAnswer) {
                    // Highlight correct answer in green
                    card.strokeWidth = 6
                    card.setStrokeColor(ContextCompat.getColor(this, R.color.correct_answer))
                } else if (isSelected && !isCorrectAnswer) {
                    // Highlight wrong selection in red
                    card.strokeWidth = 6
                    card.setStrokeColor(ContextCompat.getColor(this, R.color.wrong_answer))
                }
            }
        }
        
        if (isCorrect) {
            showSuccessFeedback()
            binding.btnNext.visibility = View.VISIBLE
        } else {
            showErrorFeedback()
            // Allow retry - show next button after delay
            Handler(Looper.getMainLooper()).postDelayed({
                binding.btnNext.visibility = View.VISIBLE
            }, 2000)
        }
        
        Handler(Looper.getMainLooper()).postDelayed({
            isProcessingAnswer = false
        }, 1000)
    }
    
    private fun showSuccessFeedback() {
        // Play success sound
        soundManager.playSuccessSound()
        
        // Success animation (star system removed)
    }
    
    private fun showErrorFeedback() {
        // Play error sound
        soundManager.playErrorSound()
        
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
    
    override fun onPause() {
        super.onPause()
        if (::soundManager.isInitialized) {
            soundManager.pauseBackgroundMusic()
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (::soundManager.isInitialized) {
            soundManager.resumeBackgroundMusic()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::soundManager.isInitialized) {
            soundManager.stopBackgroundMusic()
        }
    }
}

