package com.example.playbright.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.playbright.R
import com.example.playbright.databinding.ActivityPictureLabelingBinding
import com.example.playbright.ui.viewmodel.PictureLabelingViewModel
import com.example.playbright.utils.SoundManager
import java.util.Locale

class PictureLabelingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPictureLabelingBinding
    private val viewModel: PictureLabelingViewModel by viewModels()
    private lateinit var soundManager: SoundManager
    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsInitialized = false
    private var isListening = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startSpeechRecognition()
        } else {
            Toast.makeText(
                this,
                "Microphone permission is needed for speech practice",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityPictureLabelingBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val moduleIdFromIntent = intent.getStringExtra("MODULE_ID") ?: "picture-label"
            viewModel.setModuleId(moduleIdFromIntent)

            // Initialize SoundManager
            soundManager = SoundManager.getInstance(this)

            setupToolbar()
            setupObservers()
            setupClickListeners()
            initializeTextToSpeech()
            initializeSpeechRecognizer()

            // Start background music
            soundManager.startBackgroundMusic()

            viewModel.loadLabels()
        } catch (e: Exception) {
            android.util.Log.e("PictureLabeling", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading labels: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Tap to Hear Word"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupObservers() {
        viewModel.currentLabel.observe(this) { label ->
            if (label != null) {
                android.util.Log.d("PictureLabeling", "Current label updated: ${label.word}")
                updateLabelUI(label)
            } else {
                android.util.Log.w("PictureLabeling", "⚠️ Current label is null")
            }
        }

        viewModel.currentLabelIndex.observe(this) { index ->
            android.util.Log.d("PictureLabeling", "Current label index updated: $index")
            updateLabelCounter()
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBarLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.layoutContent.visibility = if (isLoading) View.GONE else View.VISIBLE
            android.util.Log.d("PictureLabeling", "Loading state: $isLoading")
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                android.util.Log.e("PictureLabeling", "❌ Error: $it")
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                if (it.contains("No labels available")) {
                    finish()
                }
            }
        }

        viewModel.speechRecognitionState.observe(this) { state ->
            updateSpeechRecognitionUI(state)
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Tap image to hear word (both card and image)
        binding.cardImage.setOnClickListener {
            android.util.Log.d("PictureLabeling", "Card image tapped")
            speakWord()
        }

        binding.ivImage.setOnClickListener {
            android.util.Log.d("PictureLabeling", "Image view tapped")
            speakWord()
        }

        // Repeat button
        binding.btnRepeat.setOnClickListener {
            android.util.Log.d("PictureLabeling", "Repeat button tapped")
            speakWord()
        }

        // Record/Speak button - Speech-to-Text
        binding.btnRecord.setOnClickListener {
            if (isListening) {
                stopSpeechRecognition()
            } else {
                checkPermissionAndStartSpeechRecognition()
            }
        }

        // Navigation buttons
        binding.btnPrevious.setOnClickListener {
            android.util.Log.d("PictureLabeling", "Previous button tapped")
            stopSpeechRecognition()
            viewModel.previousLabel()
        }

        binding.btnNext.setOnClickListener {
            android.util.Log.d("PictureLabeling", "Next button tapped")
            stopSpeechRecognition()
            viewModel.nextLabel()
        }
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    android.util.Log.e("PictureLabeling", "TTS language not supported")
                    Toast.makeText(this, "Text-to-speech language not supported", Toast.LENGTH_SHORT).show()
                    isTtsInitialized = false
                } else {
                    isTtsInitialized = true
                    android.util.Log.d("PictureLabeling", "✅ TTS initialized successfully")
                }
            } else {
                android.util.Log.e("PictureLabeling", "TTS initialization failed with status: $status")
                Toast.makeText(this, "Text-to-speech initialization failed", Toast.LENGTH_SHORT).show()
                isTtsInitialized = false
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    android.util.Log.d("PictureLabeling", "✅ Ready for speech")
                    viewModel.setSpeechRecognitionState("listening")
                }

                override fun onBeginningOfSpeech() {
                    android.util.Log.d("PictureLabeling", "🎤 Beginning of speech")
                    viewModel.setSpeechRecognitionState("listening")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Visual feedback for audio level
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // Not used
                }

                override fun onEndOfSpeech() {
                    android.util.Log.d("PictureLabeling", "🔇 End of speech")
                    viewModel.setSpeechRecognitionState("processing")
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        else -> "Unknown error"
                    }
                    android.util.Log.e("PictureLabeling", "❌ Speech recognition error: $errorMessage ($error)")
                    viewModel.setSpeechRecognitionState("idle")
                    isListening = false
                    
                    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        Toast.makeText(this@PictureLabelingActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    
                    if (matches != null && matches.isNotEmpty()) {
                        val recognizedText = matches[0]
                        android.util.Log.d("PictureLabeling", "✅ Recognized: '$recognizedText'")
                        
                        val currentLabel = viewModel.currentLabel.value
                        if (currentLabel != null) {
                            // Use ViewModel's checkSpokenWord for consistent scoring
                            val isCorrect = viewModel.checkSpokenWord(recognizedText)
                            
                            viewModel.setSpeechRecognitionState(if (isCorrect) "correct" else "incorrect")
                            
                            if (isCorrect) {
                                // Play success sound
                                soundManager.playSuccessSound()
                                
                                // Show success feedback
                                binding.tvSpeechResult.text = "✓ Excellent! You said: ${recognizedText.lowercase()}"
                                binding.tvSpeechResult.setTextColor(ContextCompat.getColor(this@PictureLabelingActivity, R.color.success_green))
                                binding.tvSpeechResult.visibility = View.VISIBLE
                                binding.cardSpeechResult.visibility = View.VISIBLE
                                
                                // Animate success - pulse effect
                                binding.cardImage.animate()
                                    .scaleX(1.1f)
                                    .scaleY(1.1f)
                                    .setDuration(200)
                                    .withEndAction {
                                        binding.cardImage.animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .setDuration(200)
                                            .start()
                                    }
                                    .start()
                                
                                // Show word label with animation
                                binding.tvWord.visibility = View.VISIBLE
                                binding.tvWord.alpha = 0f
                                binding.tvWord.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .start()
                                
                                // Auto-advance after 2.5 seconds
                                binding.cardImage.postDelayed({
                                    viewModel.nextLabel()
                                    binding.tvSpeechResult.visibility = View.GONE
                                }, 2500)
                            } else {
                                // Play error sound
                                soundManager.playErrorSound()
                                
                                // Show feedback with varied encouragement
                                val encouragements = listOf(
                                    "Keep trying! Say: ${currentLabel.word}",
                                    "You can do it! Try: ${currentLabel.word}",
                                    "Almost there! Say: ${currentLabel.word}",
                                    "Nice try! The word is: ${currentLabel.word}"
                                )
                                binding.tvSpeechResult.text = encouragements.random()
                                binding.tvSpeechResult.setTextColor(ContextCompat.getColor(this@PictureLabelingActivity, R.color.error_red))
                                binding.tvSpeechResult.visibility = View.VISIBLE
                                binding.cardSpeechResult.visibility = View.VISIBLE
                                
                                // Subtle shake animation for incorrect
                                binding.cardImage.animate()
                                    .translationX(-10f)
                                    .setDuration(50)
                                    .withEndAction {
                                        binding.cardImage.animate()
                                            .translationX(10f)
                                            .setDuration(50)
                                            .withEndAction {
                                                binding.cardImage.animate()
                                                    .translationX(0f)
                                                    .setDuration(50)
                                                    .start()
                                            }
                                            .start()
                                    }
                                    .start()
                            }
                        }
                    } else {
                        viewModel.setSpeechRecognitionState("idle")
                        binding.tvSpeechResult.text = "Couldn't hear clearly. Try again!"
                        binding.tvSpeechResult.setTextColor(ContextCompat.getColor(this@PictureLabelingActivity, R.color.warning_yellow))
                        binding.tvSpeechResult.visibility = View.VISIBLE
                        binding.tvSpeechResult.parent?.let { parent ->
                            if (parent is View) {
                                parent.visibility = View.VISIBLE
                            }
                        }
                    }
                    
                    isListening = false
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches != null && matches.isNotEmpty()) {
                        android.util.Log.d("PictureLabeling", "🎤 Partial result: ${matches[0]}")
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Not used
                }
            })
        } else {
            android.util.Log.e("PictureLabeling", "❌ Speech recognition not available")
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
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the word you see!")
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            viewModel.setSpeechRecognitionState("starting")
            binding.tvSpeechResult.visibility = View.GONE
            binding.cardSpeechResult.visibility = View.GONE
            android.util.Log.d("PictureLabeling", "🎤 Started listening")
        } catch (e: Exception) {
            android.util.Log.e("PictureLabeling", "❌ Error starting speech recognition: ${e.message}", e)
            Toast.makeText(this, "Error starting speech recognition: ${e.message}", Toast.LENGTH_SHORT).show()
            isListening = false
            viewModel.setSpeechRecognitionState("idle")
        }
    }

    private fun stopSpeechRecognition() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            viewModel.setSpeechRecognitionState("idle")
            android.util.Log.d("PictureLabeling", "🛑 Stopped listening")
        }
    }

    private fun updateSpeechRecognitionUI(state: String) {
        when (state) {
            "idle" -> {
                binding.btnRecord.text = "Tap to Speak"
                binding.btnRecord.setIconResource(R.drawable.ic_microphone)
                binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, R.color.info_blue)
                binding.ivListeningIndicator.visibility = View.GONE
            }
            "starting", "listening" -> {
                binding.btnRecord.text = "Listening..."
                binding.btnRecord.setIconResource(R.drawable.ic_microphone)
                binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, R.color.success_green)
                binding.ivListeningIndicator.visibility = View.VISIBLE
                binding.ivListeningIndicator.animate()
                    .alpha(0.3f)
                    .setDuration(500)
                    .withEndAction {
                        binding.ivListeningIndicator.animate()
                            .alpha(1f)
                            .setDuration(500)
                            .start()
                    }
                    .start()
            }
            "processing" -> {
                binding.btnRecord.text = "Processing..."
                binding.btnRecord.setIconResource(R.drawable.ic_microphone)
                binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, R.color.warning_yellow)
                binding.ivListeningIndicator.visibility = View.VISIBLE
            }
            "correct" -> {
                binding.btnRecord.text = "Correct!"
                binding.btnRecord.setIconResource(R.drawable.ic_star)
                binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, R.color.success_green)
                binding.ivListeningIndicator.visibility = View.GONE
            }
            "incorrect" -> {
                binding.btnRecord.text = "Try Again"
                binding.btnRecord.setIconResource(R.drawable.ic_microphone)
                binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(this, R.color.error_red)
                binding.ivListeningIndicator.visibility = View.GONE
            }
        }
    }

    private fun speakWord() {
        val currentLabel = viewModel.currentLabel.value
        if (currentLabel == null) {
            android.util.Log.w("PictureLabeling", "⚠️ No label available to speak")
            Toast.makeText(this, "No label available", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Use audioText if available, otherwise use word
        val textToSpeak = currentLabel.audioText ?: currentLabel.word
        android.util.Log.d("PictureLabeling", "speakWord called - text: '$textToSpeak', isTtsInitialized: $isTtsInitialized")
        
        if (textToSpeak.isEmpty()) {
            android.util.Log.w("PictureLabeling", "⚠️ No text available to speak")
            Toast.makeText(this, "No text available", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!isTtsInitialized) {
            android.util.Log.w("PictureLabeling", "⚠️ TTS not initialized yet")
            Toast.makeText(this, "Text-to-speech not ready. Please wait...", Toast.LENGTH_SHORT).show()
            // Try to initialize again
            initializeTextToSpeech()
            return
        }
        
        try {
            // Stop any ongoing speech
            textToSpeech?.stop()
            
            // Speak the text
            val result = textToSpeech?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "word_${System.currentTimeMillis()}")
            
            if (result == TextToSpeech.ERROR) {
                android.util.Log.e("PictureLabeling", "❌ Error speaking text: $textToSpeak")
                Toast.makeText(this, "Error speaking text", Toast.LENGTH_SHORT).show()
                return
            }
            
            android.util.Log.d("PictureLabeling", "✅ Speaking text: $textToSpeak")
            
            // Show visual feedback (display word, not audioText)
            binding.tvWord.visibility = View.VISIBLE
            binding.tvWord.text = currentLabel.word
            
            // Animate word appearance
            binding.tvWord.alpha = 0f
            binding.tvWord.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
            
            // Hide speech result when speaking
            binding.tvSpeechResult.visibility = View.GONE
            binding.tvSpeechResult.parent?.let { parent ->
                if (parent is View) {
                    parent.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PictureLabeling", "❌ Exception speaking text: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLabelUI(label: com.example.playbright.data.model.PictureLabelResponse) {
        android.util.Log.d("PictureLabeling", "Updating UI for label: ${label.word}, imageUrl: ${label.imageUrl}")
        
        // Reset speech recognition state
        stopSpeechRecognition()
        viewModel.setSpeechRecognitionState("idle")
        binding.tvSpeechResult.visibility = View.GONE
        binding.cardSpeechResult.visibility = View.GONE
        
        // Load image
        if (label.imageUrl.isNotEmpty()) {
            try {
                Glide.with(this)
                    .load(label.imageUrl)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_error_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(binding.ivImage)
                android.util.Log.d("PictureLabeling", "✅ Image loaded: ${label.imageUrl}")
            } catch (e: Exception) {
                android.util.Log.e("PictureLabeling", "❌ Error loading image: ${e.message}", e)
                binding.ivImage.setImageResource(R.drawable.ic_error_image)
            }
        } else {
            android.util.Log.w("PictureLabeling", "⚠️ Empty imageUrl, using placeholder")
            binding.ivImage.setImageResource(R.drawable.ic_placeholder_image)
        }

        // Hide word initially (will show when tapped)
        binding.tvWord.visibility = View.GONE
        binding.tvWord.text = label.word
        android.util.Log.d("PictureLabeling", "Word set to: ${label.word}")

        updateLabelCounter()
    }

    private fun updateLabelCounter() {
        val currentIndex = viewModel.currentLabelIndex.value ?: 0
        val total = viewModel.getTotalLabels()
        binding.tvLabelCounter.text = "${currentIndex + 1}/$total"
    }

    private fun updateAccuracyDisplay() {
        val accuracy = viewModel.getAccuracyPercentage()
        val correct = viewModel.correctAttempts.value ?: 0
        val total = viewModel.totalAttempts.value ?: 0
        
        if (total > 0) {
            binding.tvAccuracy.text = "Accuracy: $accuracy% ($correct/$total)"
            binding.tvAccuracy.visibility = View.VISIBLE
        } else {
            binding.tvAccuracy.visibility = View.GONE
        }
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
}
