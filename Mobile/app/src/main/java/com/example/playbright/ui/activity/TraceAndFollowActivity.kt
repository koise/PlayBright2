package com.example.playbright.ui.activity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.playbright.R
import com.example.playbright.databinding.ActivityTraceAndFollowBinding
import com.example.playbright.ui.viewmodel.TraceAndFollowViewModel
import com.example.playbright.utils.SoundManager
import java.util.Locale

class TraceAndFollowActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTraceAndFollowBinding
    private val viewModel: TraceAndFollowViewModel by viewModels()
    private lateinit var soundManager: SoundManager
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityTraceAndFollowBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Get moduleId from intent and set it in ViewModel
            val moduleIdFromIntent = intent.getStringExtra("MODULE_ID") ?: "trace-follow"
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

            setupToolbar()
            setupObservers()
            setupClickListeners()

            // Show game layout
            binding.layoutGame.visibility = View.VISIBLE
            binding.layoutCompletion.visibility = View.GONE

            // Start background music
            if (::soundManager.isInitialized) {
                soundManager.startBackgroundMusic()
            }

            // Load traces and start the activity
            viewModel.loadTraces()
        } catch (e: Exception) {
            android.util.Log.e("TraceAndFollow", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading activity: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.currentTrace.observe(this) { trace ->
            trace?.let {
                updateTraceUI(it)
            }
        }

        viewModel.progress.observe(this) { progress ->
            binding.progressBar.setProgressCompat(progress, true)
            binding.tvProgress.text = "$progress%"
        }

        viewModel.currentTraceIndex.observe(this) { index ->
            updateTraceCounter()
        }

        viewModel.totalTraces.observe(this) {
            updateTraceCounter()
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBarLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                if (it.contains("No tracing activities available")) {
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

    private fun updateTraceCounter() {
        val currentIndex = viewModel.currentTraceIndex.value ?: 0
        val total = viewModel.totalTraces.value ?: 0
        binding.tvTraceNumber.text = "Trace ${currentIndex + 1}/$total"
    }

    private fun updateTraceUI(trace: com.example.playbright.data.model.TraceAndFollowResponse) {
        // Clear previous drawing
        binding.tracingView.clearDrawing()

        // Load template image
        if (!trace.traceImageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .asBitmap()
                .load(trace.traceImageUrl)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        binding.tracingView.setTemplateBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                        // Handle cleanup if needed
                    }
                })
        }

        // Update title
        binding.tvTitle.text = trace.title

        // Speak the audio text
        val textToSpeak = trace.audioText ?: trace.title
        speakText(textToSpeak)
    }

    private fun speakText(text: String) {
        if (isTtsInitialized && text.isNotEmpty()) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun setupClickListeners() {
        binding.btnClear.setOnClickListener {
            binding.tracingView.clearDrawing()
            soundManager.playErrorSound()
        }

        binding.btnUndo.setOnClickListener {
            binding.tracingView.undoLastStroke()
            soundManager.playErrorSound()
        }

        binding.btnNext.setOnClickListener {
            if (binding.tracingView.hasDrawing()) {
                viewModel.markTraceAsCompleted()
                viewModel.nextTrace()
                soundManager.playSuccessSound()
                showSuccessAnimation()
            } else {
                Toast.makeText(this, "Please trace the template first!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnListen.setOnClickListener {
            val currentTrace = viewModel.currentTrace.value
            currentTrace?.let {
                val textToSpeak = it.audioText ?: it.title
                speakText(textToSpeak)
            }
        }

        // Completion screen buttons
        binding.btnRestart.setOnClickListener {
            viewModel.restartGame()
            binding.layoutGame.visibility = View.VISIBLE
            binding.layoutCompletion.visibility = View.GONE
            soundManager.startBackgroundMusic()
        }

        binding.btnFinish.setOnClickListener {
            finish()
        }
    }

    private fun showSuccessAnimation() {
        binding.tracingView.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.success_pop)
        )
    }

    private fun showCompletionScreen() {
        binding.layoutGame.visibility = View.GONE
        binding.layoutCompletion.visibility = View.VISIBLE

        val stats = viewModel.getStatistics()
        val completedTraces = stats["completedTraces"] as? Int ?: 0
        val totalTraces = stats["totalTraces"] as? Int ?: 0

        binding.tvCompletionTitle.text = "Great Job!"
        binding.tvCompletionMessage.text = "You completed $completedTraces out of $totalTraces tracing activities!"

        // Animate completion screen
        binding.layoutCompletion.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        )

        // Stop background music
        if (::soundManager.isInitialized) {
            soundManager.stopBackgroundMusic()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::soundManager.isInitialized) {
            soundManager.pauseBackgroundMusic()
        }
        textToSpeech?.stop()
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

