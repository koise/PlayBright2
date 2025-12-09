package com.example.playbright.ui.activity

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.playbright.R
import com.example.playbright.databinding.ActivityPictureQuizBinding
import com.example.playbright.ui.viewmodel.PictureQuizViewModel
import com.example.playbright.utils.SoundManager
import com.google.android.material.button.MaterialButton

class PictureQuizActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPictureQuizBinding
    private val viewModel: PictureQuizViewModel by viewModels()
    private lateinit var soundManager: SoundManager
    private var hasAnsweredCorrectly = false
    private var isProcessingAnswer = false // Prevent double-tap
    private var shuffledIndices = listOf<Int>() // Track shuffled image positions
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityPictureQuizBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Get moduleId from intent and set it in ViewModel
            val moduleIdFromIntent = intent.getStringExtra("MODULE_ID") ?: "picture-quiz"
            viewModel.setModuleId(moduleIdFromIntent)
            
            // Initialize SoundManager
            soundManager = SoundManager.getInstance(this)
            
            setupObservers()
            setupClickListeners()
            
            // Show quiz layout immediately (will be hidden if loading fails)
            binding.layoutQuiz.visibility = View.VISIBLE
            binding.layoutCompletion.visibility = View.GONE
            
            // Start background music
            soundManager.startBackgroundMusic()
            
            // Load questions and start the game immediately
            viewModel.loadQuestions()
        } catch (e: Exception) {
            android.util.Log.e("PictureQuiz", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading quiz: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupObservers() {
        viewModel.currentQuestion.observe(this) { question ->
            question?.let {
                updateQuestionUI(it)
                hasAnsweredCorrectly = false
            }
        }
        
        viewModel.score.observe(this) { score ->
            binding.tvScore.text = "Score: $score"
        }
        
        viewModel.progress.observe(this) { progress ->
            // Update circular progress ring
            binding.progressRing.progress = progress
            binding.tvProgressPercent.text = "$progress%"
        }
        
        viewModel.currentQuestionIndex.observe(this) { index ->
            updateQuestionCounter()
        }
        
        viewModel.originalTotalQuestions.observe(this) {
            updateQuestionCounter()
        }
        
        viewModel.correctAnswers.observe(this) {
            updateAccuracy()
        }
        
        viewModel.wrongAnswers.observe(this) {
            updateAccuracy()
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBarLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            // Keep quiz layout visible during loading to show immediate feedback
            // Only hide if there's an error
        }
        
        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                // Optionally finish activity if no questions available
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
        
        viewModel.correctAnswers.observe(this) { 
            // Statistics will be updated in completion screen
        }
        
        viewModel.wrongAnswers.observe(this) { 
            updateAccuracy()
        }
    }
    
    private fun updateQuestionCounter() {
        val currentIndex = viewModel.currentQuestionIndex.value ?: 0
        val originalTotal = viewModel.originalTotalQuestions.value ?: 0
        binding.tvQuestionNumber.text = "Question ${currentIndex + 1}/$originalTotal"
    }
    
    private fun updateAccuracy() {
        val correct = viewModel.correctAnswers.value ?: 0
        val wrong = viewModel.wrongAnswers.value ?: 0
        val totalAttempted = correct + wrong
        
        // Calculate real-time accuracy (correct / total attempts)
        // Accuracy = correct answers / (correct + wrong)
        val accuracy = if (totalAttempted > 0) {
            (correct * 100) / totalAttempted
        } else {
            0
        }
        
        // Update accuracy display in real-time (if visible during quiz)
        // The accuracy will also be shown in completion screen
    }
    
    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        binding.btnNext.setOnClickListener {
            viewModel.nextQuestion()
            binding.btnNext.visibility = View.GONE
            hasAnsweredCorrectly = false
        }
        
        binding.btnPlayAgain.setOnClickListener {
            viewModel.restartQuiz()
            binding.layoutCompletion.visibility = View.GONE
            binding.layoutQuiz.visibility = View.VISIBLE
            hasAnsweredCorrectly = false
        }
        
        binding.btnHome.setOnClickListener {
            finish()
        }
    }
    
    private fun updateQuestionUI(question: com.example.playbright.data.model.QuestionResponse) {
        // Animate question text entrance
        binding.tvQuestion.alpha = 0f
        binding.tvQuestion.text = question.question
        binding.tvQuestion.animate()
            .alpha(1f)
            .setDuration(400)
            .start()
        
        // Load 4 images - use new structure if available, fallback to legacy
        val imageViews = listOf(
            binding.ivImage1,
            binding.ivImage2,
            binding.ivImage3,
            binding.ivImage4
        )
        
        val imageCards = listOf(
            binding.cardImage1,
            binding.cardImage2,
            binding.cardImage3,
            binding.cardImage4
        )
        
        // Reset all image cards with fade out animation
        imageCards.forEach { card ->
            card.alpha = 0f
            card.strokeWidth = 0
            card.setStrokeColor(ContextCompat.getColor(this, android.R.color.transparent))
        }
        
        // Reset processing flag
        isProcessingAnswer = false
        hasAnsweredCorrectly = false
        
        // Use new images array structure if available
        val imagesToLoad = if (question.images != null && question.images.isNotEmpty()) {
            question.images
        } else {
            // Fallback to legacy imageUrls structure
            question.imageUrls?.mapIndexed { index, url ->
                com.example.playbright.data.model.QuestionImage(
                    id = index.toString(),
                    isCorrect = index == (question.correctAnswer ?: -1),
                    url = url
                )
            } ?: emptyList()
        }
        
        // Shuffle images for random display
        shuffledIndices = (0 until imagesToLoad.size).shuffled()
        
        // Load images in shuffled order with staggered animations
        shuffledIndices.forEachIndexed { displayIndex, originalIndex ->
            if (displayIndex < imageViews.size && originalIndex < imagesToLoad.size) {
                val image = imagesToLoad[originalIndex]
                val card = imageCards[displayIndex]
                
                // Make image clickable
                card.isClickable = true
                card.isFocusable = true
                card.setOnClickListener {
                    // Add pulse animation on click
                    val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse)
                    card.startAnimation(pulseAnim)
                    
                    // Small delay for visual feedback
                    Handler(Looper.getMainLooper()).postDelayed({
                        handleImageSelection(displayIndex, originalIndex, imagesToLoad)
                    }, 100)
                }
                
                // Load image
                if (image.url.isNotEmpty()) {
                    Glide.with(this)
                        .load(image.url)
                        .placeholder(R.drawable.ic_placeholder_image)
                        .error(R.drawable.ic_error_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(imageViews[displayIndex])
                } else {
                    imageViews[displayIndex].setImageResource(R.drawable.ic_placeholder_image)
                }
                
                // Animate card entrance with staggered delay
                Handler(Looper.getMainLooper()).postDelayed({
                    val popInAnim = AnimationUtils.loadAnimation(this, R.anim.pop_in)
                    card.startAnimation(popInAnim)
                    card.animate()
                        .alpha(1f)
                        .setDuration(400)
                        .start()
                }, (displayIndex * 100L)) // Stagger by 100ms each
            }
        }
        
        // Option buttons removed - images are now clickable directly
        
        // Hide next button initially
        binding.btnNext.visibility = View.GONE
    }
    
    private fun handleImageSelection(displayIndex: Int, originalIndex: Int, imagesToLoad: List<com.example.playbright.data.model.QuestionImage>) {
        // Prevent double-tap/rapid taps
        if (isProcessingAnswer) return
        
        val question = viewModel.currentQuestion.value ?: return
        
        if (originalIndex >= imagesToLoad.size) return
        
        // Set processing flag to prevent multiple selections
        isProcessingAnswer = true
        
        // Disable all cards temporarily
        val imageCards = listOf(
            binding.cardImage1,
            binding.cardImage2,
            binding.cardImage3,
            binding.cardImage4
        )
        imageCards.forEach { it.isClickable = false }
        
        val isCorrect = imagesToLoad[originalIndex].isCorrect
        
        if (isCorrect) {
            // Correct answer - highlight in green with animation
            imageCards[displayIndex].strokeWidth = 8
            imageCards[displayIndex].setStrokeColor(ContextCompat.getColor(this, R.color.correct_answer))
            
            // Bouncy success animation
            val successAnim = AnimationUtils.loadAnimation(this, R.anim.success_pop)
            imageCards[displayIndex].startAnimation(successAnim)
            
            // Award star and show animation
            if (!hasAnsweredCorrectly) {
                hasAnsweredCorrectly = true
                viewModel.checkAnswer(originalIndex)
                playSuccessSound()
                showSuccessAnimation()
                
                // Animate all other cards to fade slightly
                imageCards.forEachIndexed { idx, card ->
                    if (idx != displayIndex) {
                        card.animate()
                            .alpha(0.5f)
                            .setDuration(300)
                            .start()
                    }
                }
                
                // Show next button with slide up animation
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.btnNext.visibility = View.VISIBLE
                    val slideUpAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
                    binding.btnNext.startAnimation(slideUpAnim)
                    isProcessingAnswer = false // Re-enable after showing next button
                }, 1200)
            } else {
                isProcessingAnswer = false
            }
        } else {
            // Wrong answer - shake and highlight in red
            val shakeAnim = AnimationUtils.loadAnimation(this, R.anim.shake)
            imageCards[displayIndex].startAnimation(shakeAnim)
            
            imageCards[displayIndex].strokeWidth = 8
            imageCards[displayIndex].setStrokeColor(ContextCompat.getColor(this, R.color.wrong_answer))
            
            // Record wrong answer in statistics
            if (!hasAnsweredCorrectly) {
                viewModel.checkAnswer(originalIndex) // This will increment wrong answer count
            }
            
            playErrorSound()
            
            // Show correct answer in green with pop animation (find the display index of the correct answer)
            Handler(Looper.getMainLooper()).postDelayed({
                shuffledIndices.forEachIndexed { dispIdx, origIdx ->
                    if (origIdx < imagesToLoad.size && imagesToLoad[origIdx].isCorrect && dispIdx < imageCards.size) {
                        imageCards[dispIdx].strokeWidth = 8
                        imageCards[dispIdx].setStrokeColor(ContextCompat.getColor(this, R.color.correct_answer))
                        
                        // Highlight correct answer with bounce
                        val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.bounce)
                        imageCards[dispIdx].startAnimation(bounceAnim)
                    }
                }
            }, 300)
            
            // Proceed to next question after delay (wrong answer)
            Handler(Looper.getMainLooper()).postDelayed({
                viewModel.nextQuestion()
                binding.btnNext.visibility = View.GONE
                isProcessingAnswer = false // Re-enable after moving to next question
            }, 2500)
        }
    }
    
    
    private fun playSuccessSound() {
        soundManager.playSuccessSound()
    }
    
    private fun playErrorSound() {
        soundManager.playErrorSound()
    }
    
    private fun showSuccessAnimation() {
        // Success animation (star system removed)
    }
    
    private fun showCompletionScreen() {
        // Fade out quiz layout
        binding.layoutQuiz.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.layoutQuiz.visibility = View.GONE
                binding.layoutQuiz.alpha = 1f
                
                // Show completion layout with animation
                binding.layoutCompletion.alpha = 0f
                binding.layoutCompletion.visibility = View.VISIBLE
                binding.layoutCompletion.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .start()
            }
            .start()
        
        val statistics = viewModel.getStatistics()
        val correct = statistics["correct"] as Int
        val wrong = statistics["wrong"] as Int
        val totalAttempted = statistics["totalAttempted"] as Int
        val percentage = statistics["percentage"] as Int
        val score = statistics["score"] as Int
        
        // Animate text appearance
        binding.tvFinalScore.alpha = 0f
        binding.tvFinalScore.text = "🎉 Quiz Complete! 🎉"
        binding.tvFinalScore.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(300)
            .start()
        
        binding.tvScoreText.alpha = 0f
        binding.tvScoreText.text = "You scored $score out of $totalAttempted!"
        binding.tvScoreText.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(600)
            .start()
        
        // Update statistics display with staggered animation
        binding.tvCorrectCount.text = "$correct"
        binding.tvWrongCount.text = "$wrong"
        binding.tvAccuracy.text = "$percentage%"
        
        // Ensure final statistics are saved to Firebase
        viewModel.markModuleAsCompleted()
        
        // Show trophy with big celebration animation
        binding.ivTrophy.alpha = 0f
        binding.ivTrophy.scaleX = 0f
        binding.ivTrophy.scaleY = 0f
        
        Handler(Looper.getMainLooper()).postDelayed({
            binding.ivTrophy.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setInterpolator(android.view.animation.BounceInterpolator())
                .start()
            
            val celebrationAnim = AnimationUtils.loadAnimation(this, R.anim.sparkle_rotate)
            binding.ivTrophy.startAnimation(celebrationAnim)
        }, 400)
        
        // Animate buttons
        binding.btnPlayAgain.alpha = 0f
        binding.btnPlayAgain.translationY = 50f
        binding.btnPlayAgain.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(1000)
            .start()
        
        binding.btnHome.alpha = 0f
        binding.btnHome.translationY = 50f
        binding.btnHome.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(1100)
            .start()
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
