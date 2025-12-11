package com.example.playbright.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.playbright.R
import com.example.playbright.databinding.ActivityTapRepeatBinding
import com.example.playbright.ui.viewmodel.TapRepeatViewModel
import com.example.playbright.utils.SoundManager
import java.util.Locale

class TapRepeatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTapRepeatBinding
    private val viewModel: TapRepeatViewModel by viewModels()
    private lateinit var soundManager: SoundManager
    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isTtsInitialized = false
    private var hasListened = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startSpeechRecognition()
        } else {
            Toast.makeText(this, "Microphone permission is required to practice speech", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityTapRepeatBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Get moduleId from intent and set it in ViewModel
            val moduleIdFromIntent = intent.getStringExtra("MODULE_ID") ?: "tap-repeat"
            viewModel.setModuleId(moduleIdFromIntent)

            // Initialize SoundManager
            soundManager = SoundManager.getInstance(this)

            // Initialize TextToSpeech
            textToSpeech = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.US
                    isTtsInitialized = true
                }
            }

            // Initialize SpeechRecognizer
            initializeSpeechRecognizer()

            setupObservers()
            setupClickListeners()

            // Start background music
            soundManager.startBackgroundMusic()

            // Load words and start the practice
            viewModel.loadWords()
        } catch (e: Exception) {
            android.util.Log.e("TapRepeat", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading practice: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.currentWord.observe(this) { word ->
            word?.let {
                updateWordUI(it)
                hasListened = false
                binding.btnRecord.isEnabled = false
            }
        }

        viewModel.currentWordIndex.observe(this) { index ->
            updateProgress()
        }

        viewModel.totalWords.observe(this) {
            updateProgress()
        }

        viewModel.completedWords.observe(this) {
            updateProgress()
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // Show loading state if needed
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                if (it.contains("No words available")) {
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

    private fun updateProgress() {
        val currentIndex = viewModel.currentWordIndex.value ?: 0
        val total = viewModel.totalWords.value ?: 0
        val completed = viewModel.completedWords.value ?: 0

        binding.tvProgress.text = "Word ${currentIndex + 1}/$total"
        binding.tvCompleted.text = "Completed: $completed"
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnListen.setOnClickListener {
            val word = viewModel.currentWord.value
            word?.let {
                speakWord(it.word)
                hasListened = true
                binding.btnRecord.isEnabled = true
                
                // Animate button
                val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse)
                binding.btnListen.startAnimation(pulseAnim)
            }
        }

        binding.btnRecord.setOnClickListener {
            if (hasListened) {
                checkPermissionAndStartSpeechRecognition()
            } else {
                Toast.makeText(this, "Please listen to the word first!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPracticeAgain.setOnClickListener {
            viewModel.restartPractice()
            binding.layoutCompletion.visibility = View.GONE
            hasListened = false
            binding.btnRecord.isEnabled = false
        }

        binding.btnFinish.setOnClickListener {
            finish()
        }
    }

    private fun updateWordUI(word: com.example.playbright.data.model.TapRepeatResponse) {
        // Update word text
        binding.tvWord.text = word.word

        // Load image if available
        if (!word.imageUrl.isNullOrEmpty()) {
            binding.cardImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(word.imageUrl)
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_error_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(binding.ivWordImage)
        } else {
            binding.cardImage.visibility = View.GONE
        }

        // Reset UI
        binding.cardSpeechResult.visibility = View.GONE
        binding.llListeningIndicator.visibility = View.GONE
        binding.btnRecord.isEnabled = false
        hasListened = false

        // Animate word appearance
        binding.tvWord.alpha = 0f
        binding.tvWord.animate()
            .alpha(1f)
            .setDuration(400)
            .start()
    }

    private fun speakWord(word: String) {
        if (isTtsInitialized) {
            textToSpeech?.speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Toast.makeText(this, "Text-to-Speech not ready yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    android.util.Log.d("TapRepeat", "✅ Ready for speech")
                    showListeningIndicator(true)
                }

                override fun onBeginningOfSpeech() {
                    android.util.Log.d("TapRepeat", "🎤 Beginning of speech")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Visual feedback for audio level
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // Not used
                }

                override fun onEndOfSpeech() {
                    android.util.Log.d("TapRepeat", "🔇 End of speech")
                    showListeningIndicator(false)
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio error - please check your microphone"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error - please try again"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error - check your connection"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout - please try again"
                        SpeechRecognizer.ERROR_NO_MATCH -> "Could not understand. Please speak clearly and try again."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy - please wait a moment"
                        SpeechRecognizer.ERROR_SERVER -> "Server error - please try again"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout - please speak louder"
                        else -> "Unknown error - please try again"
                    }
                    android.util.Log.e("TapRepeat", "❌ Speech recognition error: $errorMessage ($error)")
                    showListeningIndicator(false)
                    isListening = false

                    // Always show error message to help user understand what went wrong
                    Toast.makeText(this@TapRepeatActivity, errorMessage, Toast.LENGTH_LONG).show()
                    
                    // Re-enable record button for retry (except for permission errors)
                    if (error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            binding.btnRecord.isEnabled = hasListened
                        }, 1000)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    
                    if (matches != null && matches.isNotEmpty()) {
                        val recognizedText = matches[0].trim()
                        val confidenceScore = if (confidence != null && confidence.isNotEmpty()) confidence[0] else 0f
                        android.util.Log.d("TapRepeat", "✅ Recognized: '$recognizedText' (confidence: $confidenceScore)")
                        
                        val currentWord = viewModel.currentWord.value
                        if (currentWord != null) {
                            // Normalize both texts for comparison (remove extra spaces, lowercase)
                            val normalizedRecognized = recognizedText.lowercase().trim()
                            val normalizedWord = currentWord.word.lowercase().trim()
                            
                            // Check if recognized text matches the word (case-insensitive, flexible matching)
                            val isCorrect = normalizedRecognized == normalizedWord || 
                                          normalizedRecognized.contains(normalizedWord) ||
                                          normalizedWord.contains(normalizedRecognized)
                            
                            // Mark word as attempted and track in backend
                            viewModel.markWordAsAttempted(currentWord.id, isCorrect)
                            
                            // Show result
                            showSpeechResult(recognizedText, isCorrect)
                            
                            // Move to next word after delay
                            Handler(Looper.getMainLooper()).postDelayed({
                                viewModel.nextWord()
                                binding.cardSpeechResult.visibility = View.GONE
                                hasListened = false
                                binding.btnRecord.isEnabled = false
                            }, 3000)
                        }
                    } else {
                        // No results - show error and allow retry
                        android.util.Log.w("TapRepeat", "⚠️ No speech recognition results")
                        Toast.makeText(this@TapRepeatActivity, "Could not recognize speech. Please try again.", Toast.LENGTH_SHORT).show()
                        isListening = false
                        binding.btnRecord.isEnabled = true
                    }
                    isListening = false
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches != null && matches.isNotEmpty()) {
                        val partialText = matches[0]
                        android.util.Log.d("TapRepeat", "🎤 Partial result: $partialText")
                        // Optionally show partial results in UI for real-time feedback
                        // binding.tvSpeechResult.text = "Listening: $partialText..."
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Not used
                }
            })
        } else {
            android.util.Log.e("TapRepeat", "❌ Speech recognition not available")
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermissionAndStartSpeechRecognition() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startSpeechRecognition()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startSpeechRecognition() {
        if (speechRecognizer == null || !SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_LONG).show()
            binding.btnRecord.isEnabled = false
            return
        }

        // Check if already listening
        if (isListening) {
            android.util.Log.w("TapRepeat", "⚠️ Already listening, ignoring request")
            return
        }

        val currentWord = viewModel.currentWord.value
        val promptText = currentWord?.word ?: "Say the word!"

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3) // Get top 3 results for better matching
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say: $promptText")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            binding.cardSpeechResult.visibility = View.GONE
            binding.btnRecord.isEnabled = false // Disable while listening
            android.util.Log.d("TapRepeat", "🎤 Started listening for: $promptText")
        } catch (e: Exception) {
            android.util.Log.e("TapRepeat", "❌ Error starting speech recognition: ${e.message}", e)
            Toast.makeText(this, "Error starting speech recognition: ${e.message}", Toast.LENGTH_LONG).show()
            isListening = false
            binding.btnRecord.isEnabled = hasListened // Re-enable if user has listened
        }
    }

    private fun showListeningIndicator(show: Boolean) {
        binding.llListeningIndicator.visibility = if (show) View.VISIBLE else View.GONE
        
        if (show) {
            // Animate dots
            animateListeningDots()
        }
    }

    private fun animateListeningDots() {
        val dots = listOf(
            binding.viewListeningDot1,
            binding.viewListeningDot2,
            binding.viewListeningDot3
        )
        
        dots.forEachIndexed { index, dot ->
            Handler(Looper.getMainLooper()).postDelayed({
                dot.animate()
                    .scaleX(1.5f)
                    .scaleY(1.5f)
                    .setDuration(300)
                    .withEndAction {
                        dot.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .start()
                    }
                    .start()
            }, (index * 200L))
        }
    }

    private fun showSpeechResult(recognizedText: String, isCorrect: Boolean) {
        binding.cardSpeechResult.visibility = View.VISIBLE
        
        if (isCorrect) {
            soundManager.playSuccessSound()
            binding.tvSpeechResult.text = "✓ Great! You said: ${recognizedText.lowercase()}"
            binding.tvSpeechResult.setTextColor(ContextCompat.getColor(this, R.color.success_green))
            
            // Animate success
            binding.tvWord.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(200)
                .withEndAction {
                    binding.tvWord.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        } else {
            soundManager.playErrorSound()
            binding.tvSpeechResult.text = "You said: ${recognizedText.lowercase()}\nTry again! The word is: ${viewModel.currentWord.value?.word}"
            binding.tvSpeechResult.setTextColor(ContextCompat.getColor(this, R.color.error_red))
        }
    }

    private fun showCompletionScreen() {
        binding.layoutCompletion.visibility = View.VISIBLE
        
        // Animate trophy
        val trophyAnim = AnimationUtils.loadAnimation(this, R.anim.sparkle_rotate)
        binding.ivTrophy.startAnimation(trophyAnim)
        
        // Animate buttons
        val slideUpAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        binding.btnPracticeAgain.startAnimation(slideUpAnim)
        binding.btnFinish.startAnimation(slideUpAnim)
        
        // Update statistics
        val completed = viewModel.completedWords.value ?: 0
        val total = viewModel.totalWords.value ?: 0
        
        binding.tvCompletionStats.text = "You practiced $completed out of $total words!"
        
        // Stop background music
        soundManager.stopBackgroundMusic()
    }

    override fun onPause() {
        super.onPause()
        textToSpeech?.stop()
        stopSpeechRecognition()
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
        speechRecognizer?.destroy()
        if (::soundManager.isInitialized) {
            soundManager.stopBackgroundMusic()
        }
    }

    private fun stopSpeechRecognition() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            showListeningIndicator(false)
            android.util.Log.d("TapRepeat", "🛑 Stopped listening")
        }
    }
}

