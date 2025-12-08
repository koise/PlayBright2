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
import com.google.android.material.button.MaterialButton

class PictureQuizActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPictureQuizBinding
    private val viewModel: PictureQuizViewModel by viewModels()
    private var mediaPlayer: MediaPlayer? = null
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
            
            setupObservers()
            setupClickListeners()
            
            // Show quiz layout immediately (will be hidden if loading fails)
            binding.layoutQuiz.visibility = View.VISIBLE
            binding.layoutCompletion.visibility = View.GONE
            
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
            binding.tvScore.text = "$score"
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
        
        viewModel.questionStars.observe(this) { starsMap ->
            updateStarsDisplay(starsMap)
        }
        
        viewModel.starsEarned.observe(this) { stars ->
            // Stars count is already displayed in tvScore
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
        // Show the actual question text
        binding.tvQuestion.text = question.question
        
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
        
        // Reset all image cards
        imageCards.forEach { card ->
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
        
        // Load images in shuffled order
        shuffledIndices.forEachIndexed { displayIndex, originalIndex ->
            if (displayIndex < imageViews.size && originalIndex < imagesToLoad.size) {
                val image = imagesToLoad[originalIndex]
                
                // Make image clickable
                imageCards[displayIndex].isClickable = true
                imageCards[displayIndex].isFocusable = true
                imageCards[displayIndex].setOnClickListener {
                    handleImageSelection(displayIndex, originalIndex, imagesToLoad)
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
            }
        }
        
        // Option buttons removed - images are now clickable directly
        
        // Hide next button initially
        binding.btnNext.visibility = View.GONE
        binding.ivStar.visibility = View.GONE
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
            // Correct answer - highlight in green
            imageCards[displayIndex].strokeWidth = 8
            imageCards[displayIndex].setStrokeColor(ContextCompat.getColor(this, R.color.correct_answer))
            
            // Award star and show animation
            if (!hasAnsweredCorrectly) {
                hasAnsweredCorrectly = true
                viewModel.checkAnswer(originalIndex)
                playSuccessSound()
                showSuccessAnimation()
                
                // Show next button after delay
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.btnNext.visibility = View.VISIBLE
                    isProcessingAnswer = false // Re-enable after showing next button
                }, 1500)
            } else {
                isProcessingAnswer = false
            }
        } else {
            // Wrong answer - highlight in red
            imageCards[displayIndex].strokeWidth = 8
            imageCards[displayIndex].setStrokeColor(ContextCompat.getColor(this, R.color.wrong_answer))
            
            // Record wrong answer in statistics
            if (!hasAnsweredCorrectly) {
                viewModel.checkAnswer(originalIndex) // This will increment wrong answer count
            }
            
            playErrorSound()
            
            // Show correct answer in green (find the display index of the correct answer)
            shuffledIndices.forEachIndexed { dispIdx, origIdx ->
                if (origIdx < imagesToLoad.size && imagesToLoad[origIdx].isCorrect && dispIdx < imageCards.size) {
                    imageCards[dispIdx].strokeWidth = 8
                    imageCards[dispIdx].setStrokeColor(ContextCompat.getColor(this, R.color.correct_answer))
                }
            }
            
            // Proceed to next question after delay (wrong answer)
            Handler(Looper.getMainLooper()).postDelayed({
                viewModel.nextQuestion()
                binding.btnNext.visibility = View.GONE
                isProcessingAnswer = false // Re-enable after moving to next question
            }, 2000)
        }
    }
    
    
    private fun playSuccessSound() {
        try {
            // Sound file can be added later to res/raw/success_sound.mp3
            // mediaPlayer?.release()
            // mediaPlayer = MediaPlayer.create(this, R.raw.success_sound)
            // mediaPlayer?.start()
        } catch (e: Exception) {
            // Sound file might not exist, that's okay
        }
    }
    
    private fun playErrorSound() {
        try {
            // Sound file can be added later to res/raw/error_sound.mp3
            // mediaPlayer?.release()
            // mediaPlayer = MediaPlayer.create(this, R.raw.error_sound)
            // mediaPlayer?.start()
        } catch (e: Exception) {
            // Sound file might not exist, that's okay
        }
    }
    
    private fun showSuccessAnimation() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.bounce)
        binding.ivStar.visibility = View.VISIBLE
        binding.ivStar.startAnimation(animation)
        
        Handler(Looper.getMainLooper()).postDelayed({
            binding.ivStar.visibility = View.GONE
        }, 2000)
    }
    
    private fun updateStarsDisplay(starsMap: Map<Int, Boolean>) {
        binding.llStarsContainer.removeAllViews()
        
        val totalQuestions = viewModel.questions.value?.size ?: 0
        for (i in 0 until totalQuestions) {
            val starImageView = android.widget.ImageView(this)
            val size = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 2
            starImageView.layoutParams = android.widget.LinearLayout.LayoutParams(size, size).apply {
                setMargins(0, 0, resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4, 0)
            }
            
            if (starsMap[i] == true) {
                starImageView.setImageResource(R.drawable.ic_star)
                starImageView.setColorFilter(getColor(R.color.gold))
            } else {
                starImageView.setImageResource(R.drawable.ic_star)
                starImageView.setColorFilter(getColor(R.color.surface_variant))
                starImageView.alpha = 0.3f
            }
            
            binding.llStarsContainer.addView(starImageView)
        }
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
        val starsEarned = statistics["starsEarned"] as Int
        
        binding.tvFinalScore.text = "Quiz Complete!"
        binding.tvScoreText.text = "You scored $score out of $totalAttempted!"
        
        // Update statistics display
        binding.tvCorrectCount.text = "$correct"
        binding.tvWrongCount.text = "$wrong"
        binding.tvAccuracy.text = "$percentage%"
        
        // Ensure final statistics are saved to Firebase
        // markModuleAsCompleted() is already called in nextQuestion(), but we ensure it's saved here too
        viewModel.markModuleAsCompleted()
        
        // Show celebration animation
        val animation = AnimationUtils.loadAnimation(this, R.anim.celebration)
        binding.ivTrophy.startAnimation(animation)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
