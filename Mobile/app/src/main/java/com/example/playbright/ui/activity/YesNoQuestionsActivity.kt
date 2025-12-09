package com.example.playbright.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
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
import com.example.playbright.databinding.ActivityYesNoQuestionsBinding
import com.example.playbright.ui.viewmodel.YesNoQuestionsViewModel
import com.example.playbright.utils.SoundManager
import java.util.Locale

class YesNoQuestionsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityYesNoQuestionsBinding
    private val viewModel: YesNoQuestionsViewModel by viewModels()
    private lateinit var soundManager: SoundManager
    private var textToSpeech: TextToSpeech? = null
    private var hasAnswered = false
    private var isProcessingAnswer = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityYesNoQuestionsBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Get moduleId from intent and set it in ViewModel
            val moduleIdFromIntent = intent.getStringExtra("MODULE_ID") ?: "yes-no-questions"
            viewModel.setModuleId(moduleIdFromIntent)
            
            // Initialize SoundManager
            soundManager = SoundManager.getInstance(this)
            
            // Initialize TextToSpeech
            textToSpeech = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.US
                }
            }
            
            setupObservers()
            setupClickListeners()
            
            // Show quiz layout immediately
            binding.layoutQuiz.visibility = View.VISIBLE
            binding.layoutCompletion.visibility = View.GONE
            
            // Start background music
            soundManager.startBackgroundMusic()
            
            // Load questions and start the game
            viewModel.loadQuestions()
        } catch (e: Exception) {
            android.util.Log.e("YesNoQuestions", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading quiz: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupObservers() {
        viewModel.currentQuestion.observe(this) { question ->
            question?.let {
                updateQuestionUI(it)
                hasAnswered = false
            }
        }
        
        viewModel.score.observe(this) { score ->
            binding.tvScore.text = "Score: $score"
        }
        
        viewModel.progress.observe(this) { progress ->
            binding.progressRing.progress = progress
            binding.tvProgressPercent.text = "$progress%"
        }
        
        viewModel.currentQuestionIndex.observe(this) { index ->
            updateQuestionCounter()
        }
        
        viewModel.originalTotalQuestions.observe(this) {
            updateQuestionCounter()
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            // Show loading state if needed
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
        val originalTotal = viewModel.originalTotalQuestions.value ?: 0
        binding.tvQuestionNumber.text = "Question ${currentIndex + 1}/$originalTotal"
    }
    
    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        binding.btnYes.setOnClickListener {
            if (!isProcessingAnswer && !hasAnswered) {
                handleAnswer(true)
            }
        }
        
        binding.btnNo.setOnClickListener {
            if (!isProcessingAnswer && !hasAnswered) {
                handleAnswer(false)
            }
        }
        
        binding.btnPlayAgain.setOnClickListener {
            viewModel.restartQuiz()
            binding.layoutCompletion.visibility = View.GONE
            binding.layoutQuiz.visibility = View.VISIBLE
            hasAnswered = false
            isProcessingAnswer = false
        }
        
        binding.btnFinish.setOnClickListener {
            finish()
        }
    }
    
    private fun updateQuestionUI(question: com.example.playbright.data.model.YesNoQuestionResponse) {
        // Animate question text entrance
        binding.tvQuestion.alpha = 0f
        binding.tvQuestion.text = question.question
        binding.tvQuestion.animate()
            .alpha(1f)
            .setDuration(400)
            .start()
        
        // Speak the question
        speakQuestion(question.question)
        
        // Reset buttons
        resetButtons()
        
        // Reset processing flag
        isProcessingAnswer = false
        hasAnswered = false
        
        // Load image with animation
        binding.cardImage.alpha = 0f
        if (question.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(question.imageUrl)
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_error_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(binding.ivQuestionImage)
        } else {
            binding.ivQuestionImage.setImageResource(R.drawable.ic_placeholder_image)
        }
        
        // Animate image card entrance
        Handler(Looper.getMainLooper()).postDelayed({
            val popInAnim = AnimationUtils.loadAnimation(this, R.anim.pop_in)
            binding.cardImage.startAnimation(popInAnim)
            binding.cardImage.animate()
                .alpha(1f)
                .setDuration(400)
                .start()
        }, 200)
    }
    
    private fun speakQuestion(question: String) {
        textToSpeech?.speak(question, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    private fun resetButtons() {
        binding.btnYes.isEnabled = true
        binding.btnNo.isEnabled = true
        binding.btnYes.alpha = 1f
        binding.btnNo.alpha = 1f
    }
    
    private fun handleAnswer(selectedAnswer: Boolean) {
        if (isProcessingAnswer || hasAnswered) return
        
        isProcessingAnswer = true
        hasAnswered = true
        
        val question = viewModel.currentQuestion.value ?: return
        val isCorrect = viewModel.checkAnswer(selectedAnswer)
        
        // Disable buttons
        binding.btnYes.isEnabled = false
        binding.btnNo.isEnabled = false
        
        // Visual feedback
        if (isCorrect) {
            showSuccessAnimation(selectedAnswer)
            soundManager.playSuccessSound()
        } else {
            showErrorAnimation(selectedAnswer)
            soundManager.playErrorSound()
        }
        
        // Show correct answer highlight
        highlightCorrectAnswer(question.correctAnswer)
        
        // Move to next question after delay
        Handler(Looper.getMainLooper()).postDelayed({
            isProcessingAnswer = false
            viewModel.nextQuestion()
        }, 2000)
    }
    
    private fun showSuccessAnimation(selectedAnswer: Boolean) {
        val button = if (selectedAnswer) binding.btnYes else binding.btnNo
        
        // Pulse animation
        val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse)
        button.startAnimation(pulseAnim)
    }
    
    private fun showErrorAnimation(selectedAnswer: Boolean) {
        val button = if (selectedAnswer) binding.btnYes else binding.btnNo
        
        // Shake animation
        val shakeAnim = AnimationUtils.loadAnimation(this, R.anim.shake)
        button.startAnimation(shakeAnim)
    }
    
    private fun highlightCorrectAnswer(correctAnswer: Boolean) {
        val correctButton = if (correctAnswer) binding.btnYes else binding.btnNo
        val wrongButton = if (correctAnswer) binding.btnNo else binding.btnYes
        
        // Highlight correct answer with green background tint
        correctButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.success_green)
        
        // Dim wrong answer if it was selected
        if (hasAnswered && !correctAnswer) {
            wrongButton.alpha = 0.5f
        }
    }
    
    private fun showCompletionScreen() {
        binding.layoutQuiz.visibility = View.GONE
        binding.layoutCompletion.visibility = View.VISIBLE
        
        // Animate trophy
        val trophyAnim = AnimationUtils.loadAnimation(this, R.anim.sparkle_rotate)
        binding.ivTrophy.startAnimation(trophyAnim)
        
        // Animate buttons
        val slideUpAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        binding.btnPlayAgain.startAnimation(slideUpAnim)
        binding.btnFinish.startAnimation(slideUpAnim)
        
        // Update statistics
        val stats = viewModel.getStatistics()
        val score = stats["score"] as? Int ?: 0
        val correct = stats["correct"] as? Int ?: 0
        val total = stats["total"] as? Int ?: 0
        val percentage = stats["percentage"] as? Int ?: 0
        
        binding.tvCompletionStats.text = "You scored $score points!\n$correct out of $total correct ($percentage%)"
        
        // Stop background music
        soundManager.stopBackgroundMusic()
    }
    
    override fun onPause() {
        super.onPause()
        textToSpeech?.stop()
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
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        if (::soundManager.isInitialized) {
            soundManager.stopBackgroundMusic()
        }
    }
}

