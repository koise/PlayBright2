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
            setupDrawingListeners()

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

        // Set reference drawing points if available
        trace.referenceDrawingPoints?.let { points ->
            binding.tracingView.setReferencePoints(points)
        }

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

    private fun setupDrawingListeners() {
        binding.tracingView.onSimilarityCalculated = { similarity ->
            // Update UI with similarity feedback (optional)
            android.util.Log.d("TraceAndFollow", "Similarity: $similarity%")
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
                // Calculate similarity before marking as completed
                val currentTrace = viewModel.currentTrace.value
                val referencePoints = currentTrace?.referenceDrawingPoints
                val studentPoints = binding.tracingView.getDrawingPoints()
                
                var similarity = 0
                if (referencePoints != null && referencePoints.isNotEmpty() && studentPoints.isNotEmpty()) {
                    // Similarity is already calculated in TracingView, but we need to get it
                    // For now, we'll calculate it here or use a callback
                    similarity = calculateSimilarity(referencePoints, studentPoints)
                }
                
                viewModel.markTraceAsCompleted(similarity)
                viewModel.nextTrace()
                soundManager.playSuccessSound()
                showSuccessAnimation()
                
                if (similarity > 0) {
                    val pointsEarned = if (similarity >= 80) 1 else 0
                    val message = when {
                        similarity >= 90 -> "Perfect! ${similarity}% match! 🌟 +$pointsEarned point"
                        similarity >= 80 -> "Great! ${similarity}% match! ✨ +$pointsEarned point"
                        similarity >= 70 -> "Good! ${similarity}% match! 👍 (Need 80% for point)"
                        similarity >= 50 -> "Getting there! ${similarity}% match (Need 80% for point)"
                        else -> "Keep practicing! ${similarity}% match (Need 80% for point)"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
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

    private fun calculateSimilarity(
        referencePoints: List<com.example.playbright.data.model.DrawingPoint>,
        studentPoints: List<android.graphics.PointF>
    ): Int {
        if (referencePoints.isEmpty() || studentPoints.isEmpty()) return 0

        // Convert student points to DrawingPoint format
        val studentDrawingPoints = studentPoints.map { 
            com.example.playbright.data.model.DrawingPoint(it.x, it.y) 
        }

        // Normalize both drawings
        val normalizedRef = normalizePoints(referencePoints)
        val normalizedStudent = normalizePoints(studentDrawingPoints)

        // Resample both to same number of points
        val targetPoints = 50
        val resampledRef = resamplePoints(normalizedRef, targetPoints)
        val resampledStudent = resamplePoints(normalizedStudent, targetPoints)

        if (resampledRef.isEmpty() || resampledStudent.isEmpty()) return 0

        // Calculate average distance
        var totalDistance = 0f
        for (i in resampledRef.indices) {
            if (i < resampledStudent.size) {
                val dx = resampledRef[i].x - resampledStudent[i].x
                val dy = resampledRef[i].y - resampledStudent[i].y
                totalDistance += kotlin.math.sqrt(dx * dx + dy * dy)
            }
        }

        val avgDistance = totalDistance / minOf(resampledRef.size, resampledStudent.size)
        // Convert distance to similarity percentage (0-100)
        val similarity = ((1f - avgDistance.coerceIn(0f, 1f)) * 100f).toInt()
        return similarity.coerceIn(0, 100)
    }

    private fun normalizePoints(points: List<com.example.playbright.data.model.DrawingPoint>): List<android.graphics.PointF> {
        if (points.isEmpty()) return emptyList()

        val minX = points.minOfOrNull { it.x } ?: 0f
        val maxX = points.maxOfOrNull { it.x } ?: 1f
        val minY = points.minOfOrNull { it.y } ?: 0f
        val maxY = points.maxOfOrNull { it.y } ?: 1f

        val width = (maxX - minX).coerceAtLeast(1f)
        val height = (maxY - minY).coerceAtLeast(1f)
        val scale = maxOf(width, height)

        return points.map { point ->
            android.graphics.PointF(
                (point.x - minX) / scale,
                (point.y - minY) / scale
            )
        }
    }

    private fun resamplePoints(
        points: List<android.graphics.PointF>,
        n: Int
    ): List<android.graphics.PointF> {
        if (points.isEmpty() || n <= 0) return emptyList()
        if (points.size <= n) return points

        // Calculate total path length
        var totalLength = 0f
        for (i in 1 until points.size) {
            val dx = points[i].x - points[i - 1].x
            val dy = points[i].y - points[i - 1].y
            totalLength += kotlin.math.sqrt(dx * dx + dy * dy)
        }

        if (totalLength == 0f) return points.take(n)

        val segmentLength = totalLength / (n - 1)
        val resampled = mutableListOf<android.graphics.PointF>()
        resampled.add(points[0])

        var currentLength = 0f
        var targetLength = segmentLength

        for (i in 1 until points.size) {
            val dx = points[i].x - points[i - 1].x
            val dy = points[i].y - points[i - 1].y
            val segDist = kotlin.math.sqrt(dx * dx + dy * dy)
            currentLength += segDist

            while (currentLength >= targetLength && resampled.size < n) {
                val ratio = (targetLength - (currentLength - segDist)) / segDist.coerceAtLeast(0.001f)
                val newPoint = android.graphics.PointF(
                    points[i - 1].x + ratio * dx,
                    points[i - 1].y + ratio * dy
                )
                resampled.add(newPoint)
                targetLength += segmentLength
            }
        }

        // Ensure we have exactly n points
        while (resampled.size < n) {
            resampled.add(points.last())
        }

        return resampled.take(n)
    }

    private fun showCompletionScreen() {
        binding.layoutGame.visibility = View.GONE
        binding.layoutCompletion.visibility = View.VISIBLE

        val stats = viewModel.getStatistics()
        val completedTraces = stats["completedTraces"] as? Int ?: 0
        val totalTraces = stats["totalTraces"] as? Int ?: 0
        val score = stats["score"] as? Int ?: 0
        val totalPoints = stats["totalPoints"] as? Int ?: 0
        val avgSimilarity = stats["averageSimilarity"] as? Int ?: 0
        val correct = stats["correct"] as? Int ?: 0
        val wrong = stats["wrong"] as? Int ?: 0
        val percentage = stats["percentage"] as? Int ?: 0

        binding.tvCompletionTitle.text = "Great Job!"
        
        val message = buildString {
            append("You completed $completedTraces out of $totalTraces tracing activities!\n\n")
            append("Score: $score/$totalPoints points\n")
            append("Average Similarity: $avgSimilarity%\n")
            append("Correct: $correct | Needs Practice: $wrong\n")
            append("Accuracy: $percentage%")
        }
        binding.tvCompletionMessage.text = message

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

