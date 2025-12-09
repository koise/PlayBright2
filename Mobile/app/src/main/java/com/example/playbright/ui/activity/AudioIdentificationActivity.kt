package com.example.playbright.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.playbright.R
import com.example.playbright.databinding.ActivityAudioIdentificationBinding
import com.example.playbright.ui.viewmodel.AudioIdentificationViewModel
import com.example.playbright.utils.SoundManager
import java.util.Locale

class AudioIdentificationActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    
    private lateinit var binding: ActivityAudioIdentificationBinding
    private val viewModel: AudioIdentificationViewModel by viewModels()
    private lateinit var soundManager: SoundManager
    private var textToSpeech: TextToSpeech? = null
    private var hasAnsweredCorrectly = false
    private var isTtsReady = false
    private var isProcessingAnswer = false // Prevent double-tap
    private var shuffledIndices = listOf<Int>() // Track shuffled image positions
    private val orderBadges = mutableListOf<android.widget.TextView>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityAudioIdentificationBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Initialize SoundManager
            soundManager = SoundManager.getInstance(this)
            
            // Initialize TextToSpeech
            textToSpeech = TextToSpeech(this, this)
            
            // Get moduleId from intent and set it in ViewModel
            val moduleIdFromIntent = intent.getStringExtra("MODULE_ID") ?: "audio-identification"
            viewModel.setModuleId(moduleIdFromIntent)
            
            setupObservers()
            setupClickListeners()
            
            // Show quiz layout immediately
            binding.layoutQuiz.visibility = View.VISIBLE
            binding.layoutCompletion.visibility = View.GONE
            
            // Start background music
            soundManager.startBackgroundMusic()
            
            // Load questions and start the game immediately
            viewModel.loadQuestions()
        } catch (e: Exception) {
            android.util.Log.e("AudioIdentification", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading quiz: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Text-to-speech language not supported", Toast.LENGTH_SHORT).show()
            } else {
                // Set female voice
                setFemaleVoice()
                isTtsReady = true
            }
        } else {
            Toast.makeText(this, "Text-to-speech initialization failed", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setFemaleVoice() {
        textToSpeech?.let { tts ->
            try {
                // Get available voices
                val voices = tts.voices
                if (voices != null && voices.isNotEmpty()) {
                    // Try to find a female voice - check common female voice names
                    val femaleVoice = voices.firstOrNull { voice ->
                        val name = voice.name.lowercase()
                        name.contains("female", ignoreCase = true) ||
                        name.contains("woman", ignoreCase = true) ||
                        name.contains("samantha", ignoreCase = true) ||
                        name.contains("susan", ignoreCase = true) ||
                        name.contains("karen", ignoreCase = true) ||
                        name.contains("zira", ignoreCase = true) ||
                        name.contains("hazel", ignoreCase = true) ||
                        name.contains("catherine", ignoreCase = true) ||
                        (voice.locale == Locale.US && name.contains("en-us-female", ignoreCase = true))
                    }
                    
                    if (femaleVoice != null) {
                        tts.voice = femaleVoice
                        android.util.Log.d("AudioIdentification", "Female voice set: ${femaleVoice.name}")
                    } else {
                        // If no specific female voice found, try to find any US English voice
                        // and we'll adjust pitch to make it sound more feminine
                        val usVoice = voices.firstOrNull { it.locale == Locale.US }
                        if (usVoice != null) {
                            tts.voice = usVoice
                            // Set higher pitch for more feminine sound (1.0 is default, higher = more feminine)
                            tts.setPitch(1.1f)
                            android.util.Log.d("AudioIdentification", "US voice set with higher pitch: ${usVoice.name}")
                        } else {
                            // Fallback to any available voice
                            val firstVoice = voices.firstOrNull()
                            if (firstVoice != null) {
                                tts.voice = firstVoice
                                tts.setPitch(1.1f)
                            }
                        }
                    }
                } else {
                    // If voices list is null or empty, set pitch higher for default voice
                    tts.setPitch(1.1f)
                }
            } catch (e: Exception) {
                android.util.Log.e("AudioIdentification", "Error setting female voice: ${e.message}", e)
                // Fallback: just set higher pitch
                try {
                    textToSpeech?.setPitch(1.1f)
                } catch (ex: Exception) {
                    android.util.Log.e("AudioIdentification", "Error setting pitch: ${ex.message}", ex)
                }
            }
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
        val originalTotal = viewModel.originalTotalQuestions.value ?: 0
        // Show current question out of original total
        binding.tvQuestionNumber.text = "Question ${currentIndex + 1}/$originalTotal"
    }
    
    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        binding.btnPlayAudio.setOnClickListener {
            playAudio()
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
    
    private fun playAudio() {
        val question = viewModel.currentQuestion.value ?: return
        
        // Animate button on click
        binding.btnPlayAudio.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(100)
            .withEndAction {
                binding.btnPlayAudio.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
        
        speakImageText(question)
    }
    
    private fun speakImageText(question: com.example.playbright.data.model.AudioIdentificationResponse) {
        val audioText = question.audioText ?: question.question
        
        if (isTtsReady && textToSpeech != null) {
            // Stop any ongoing speech
            textToSpeech?.stop()
            // Speak with female voice
            textToSpeech?.speak(audioText, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Toast.makeText(this, "Audio not ready yet. Please wait...", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateQuestionUI(question: com.example.playbright.data.model.AudioIdentificationResponse) {
        // Animate question text entrance
        binding.tvQuestion.alpha = 0f
        binding.tvQuestion.text = "🎵 ${question.question}"
        binding.tvQuestion.animate()
            .alpha(1f)
            .setDuration(400)
            .start()
        
        // Animate play button with bounce
        binding.btnPlayAudio.scaleX = 0f
        binding.btnPlayAudio.scaleY = 0f
        binding.btnPlayAudio.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setInterpolator(android.view.animation.BounceInterpolator())
            .setStartDelay(200)
            .start()
        
        // Load 4 images
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
        
        // Reset all image cards and badges with fade out
        imageCards.forEach { card ->
            card.alpha = 0f
            card.strokeWidth = 0
            card.setStrokeColor(ContextCompat.getColor(this, android.R.color.transparent))
        }
        orderBadges.forEach { badge ->
            badge.visibility = View.GONE
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
                com.example.playbright.data.model.AudioIdentificationImage(
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
                }, (displayIndex * 100L))
            }
        }
        
        // Hide next button initially
        binding.btnNext.visibility = View.GONE
        
        // Enable play button
        binding.btnPlayAudio.isEnabled = true
    }
    
    private fun handleImageSelection(displayIndex: Int, originalIndex: Int, imagesToLoad: List<com.example.playbright.data.model.AudioIdentificationImage>) {
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
        
        // Speak the question/audio text when image is tapped
        speakImageText(question)
        
        val isCorrect = imagesToLoad[originalIndex].isCorrect
        
        if (isCorrect) {
            // Correct answer - highlight in green with animation
            imageCards[displayIndex].strokeWidth = 8
            imageCards[displayIndex].setStrokeColor(ContextCompat.getColor(this, R.color.correct_answer))
            
            // Success pop animation
            val successAnim = AnimationUtils.loadAnimation(this, R.anim.success_pop)
            imageCards[displayIndex].startAnimation(successAnim)
            
            // Award star and show animation
            if (!hasAnsweredCorrectly) {
                hasAnsweredCorrectly = true
                viewModel.checkAnswer(originalIndex)
                playSuccessSound()
                showSuccessAnimation()
                
                // Fade other cards
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
                    isProcessingAnswer = false
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
                viewModel.checkAnswer(originalIndex)
            }
            
            playErrorSound()
            
            // Show correct answer with bounce after delay
            Handler(Looper.getMainLooper()).postDelayed({
                shuffledIndices.forEachIndexed { dispIdx, origIdx ->
                    if (origIdx < imagesToLoad.size && imagesToLoad[origIdx].isCorrect && dispIdx < imageCards.size) {
                        imageCards[dispIdx].strokeWidth = 8
                        imageCards[dispIdx].setStrokeColor(ContextCompat.getColor(this, R.color.correct_answer))
                        
                        val bounceAnim = AnimationUtils.loadAnimation(this, R.anim.bounce)
                        imageCards[dispIdx].startAnimation(bounceAnim)
                    }
                }
            }, 300)
            
            // Proceed to next question
            Handler(Looper.getMainLooper()).postDelayed({
                viewModel.nextQuestion()
                binding.btnNext.visibility = View.GONE
                hasAnsweredCorrectly = false
                isProcessingAnswer = false
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
        
        // Animate text
        binding.tvFinalScore.alpha = 0f
        binding.tvFinalScore.text = "🎵 Great Listening! 🎵"
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
        
        binding.tvCorrectCount.text = "$correct"
        binding.tvWrongCount.text = "$wrong"
        binding.tvAccuracy.text = "$percentage%"
        
        viewModel.markModuleAsCompleted()
        
        // Trophy animation
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
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        if (::soundManager.isInitialized) {
            soundManager.stopBackgroundMusic()
        }
    }
}

