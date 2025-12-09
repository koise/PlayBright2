package com.example.playbright.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.playbright.R
import com.example.playbright.databinding.ActivityWordBuilderBinding
import com.example.playbright.ui.adapter.SyllableAdapter
import com.example.playbright.ui.viewmodel.WordBuilderViewModel
import com.example.playbright.utils.SoundManager
import java.util.Locale

class WordBuilderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWordBuilderBinding
    private val viewModel: WordBuilderViewModel by viewModels()
    private lateinit var soundManager: SoundManager
    private var textToSpeech: TextToSpeech? = null
    private var syllableAdapter: SyllableAdapter? = null
    private var isTtsInitialized = false
    private var hasChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityWordBuilderBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Get moduleId from intent and set it in ViewModel
            val moduleIdFromIntent = intent.getStringExtra("MODULE_ID") ?: "word-builder"
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

            setupObservers()
            setupClickListeners()
            setupRecyclerView()

            // Show game layout
            binding.layoutGame.visibility = View.VISIBLE
            binding.layoutCompletion.visibility = View.GONE

            // Start background music
            soundManager.startBackgroundMusic()

            // Load words and start the game
            viewModel.loadWords()
        } catch (e: Exception) {
            android.util.Log.e("WordBuilder", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading game: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.currentWord.observe(this) { word ->
            word?.let {
                updateWordUI(it)
                hasChecked = false
                binding.btnNext.visibility = View.GONE
                binding.btnCheck.isEnabled = true
            }
        }

        viewModel.score.observe(this) { score ->
            binding.tvScore.text = "Score: $score"
        }

        viewModel.progress.observe(this) { progress ->
            binding.progressRing.progress = progress
            binding.tvProgressPercent.text = "$progress%"
        }

        viewModel.currentWordIndex.observe(this) { index ->
            updateWordCounter()
        }

        viewModel.totalWords.observe(this) {
            updateWordCounter()
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // Show loading state if needed
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                if (it.contains("No word builder items available")) {
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

    private fun setupRecyclerView() {
        binding.rvSyllables.layoutManager = GridLayoutManager(this, 3).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return 1
                }
            }
        }
    }

    private fun updateWordCounter() {
        val currentIndex = viewModel.currentWordIndex.value ?: 0
        val total = viewModel.totalWords.value ?: 0
        binding.tvWordNumber.text = "Word ${currentIndex + 1}/$total"
    }

    private fun updateWordUI(word: com.example.playbright.data.model.WordBuilderResponse) {
        // Clear previous selection
        viewModel.clearSelectedSyllables()
        updateSelectedSyllablesDisplay()

        // Load image if available
        if (!word.imageUrl.isNullOrEmpty()) {
            binding.cardImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(word.imageUrl)
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_placeholder_image)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .into(binding.ivWordImage)
        } else {
            binding.cardImage.visibility = View.GONE
        }

        // Shuffle syllables and create adapter
        val shuffledSyllables = word.syllables.shuffled()
        syllableAdapter = SyllableAdapter(shuffledSyllables) { syllable ->
            viewModel.addSyllable(syllable)
            updateSelectedSyllablesDisplay()
            soundManager.playSuccessSound()
            
            // Animate the syllable button
            val adapterPosition = shuffledSyllables.indexOf(syllable)
            val viewHolder = binding.rvSyllables.findViewHolderForAdapterPosition(adapterPosition)
            viewHolder?.itemView?.let {
                val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse)
                it.startAnimation(pulseAnim)
            }
        }
        binding.rvSyllables.adapter = syllableAdapter

        // Speak the word
        Handler(Looper.getMainLooper()).postDelayed({
            speakWord(word.audioText ?: word.word)
        }, 500)
    }

    private fun updateSelectedSyllablesDisplay() {
        val selectedSyllables = viewModel.getSelectedSyllables()
        binding.llSelectedSyllables.removeAllViews()

        if (selectedSyllables.isEmpty()) {
            binding.tvEmptySelection.visibility = View.VISIBLE
        } else {
            binding.tvEmptySelection.visibility = View.GONE
            
            selectedSyllables.forEachIndexed { index, syllable ->
                val chip = TextView(this).apply {
                    text = syllable
                    textSize = 24f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(16, 12, 16, 12)
                    setBackgroundResource(R.drawable.bg_syllable_chip)
                    setTextColor(ContextCompat.getColor(this@WordBuilderActivity, R.color.white))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = if (index < selectedSyllables.size - 1) 8 else 0
                    }
                }
                binding.llSelectedSyllables.addView(chip)
                
                // Animate chip appearance
                chip.alpha = 0f
                chip.scaleX = 0.5f
                chip.scaleY = 0.5f
                chip.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setStartDelay(index * 100L.toLong())
                    .start()
            }
        }
    }

    private fun speakWord(text: String) {
        if (isTtsInitialized && text.isNotEmpty()) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnClear.setOnClickListener {
            viewModel.clearSelectedSyllables()
            updateSelectedSyllablesDisplay()
            syllableAdapter?.resetUsedSyllables()
            soundManager.playErrorSound()
        }

        binding.btnCheck.setOnClickListener {
            if (hasChecked) return@setOnClickListener
            
            val isCorrect = viewModel.checkWord()
            hasChecked = true
            binding.btnCheck.isEnabled = false

            if (isCorrect) {
                // Correct answer
                soundManager.playSuccessSound()
                showSuccessAnimation()
                
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.btnNext.visibility = View.VISIBLE
                    val slideUpAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
                    binding.btnNext.startAnimation(slideUpAnim)
                }, 1000)
            } else {
                // Wrong answer
                soundManager.playErrorSound()
                showErrorAnimation()
                
                Handler(Looper.getMainLooper()).postDelayed({
                    // Clear and allow retry
                    viewModel.clearSelectedSyllables()
                    updateSelectedSyllablesDisplay()
                    syllableAdapter?.resetUsedSyllables()
                    hasChecked = false
                    binding.btnCheck.isEnabled = true
                }, 2000)
            }
        }

        binding.btnNext.setOnClickListener {
            viewModel.nextWord()
            hasChecked = false
            binding.btnNext.visibility = View.GONE
        }

        binding.btnRestart.setOnClickListener {
            viewModel.restartQuiz()
            binding.layoutGame.visibility = View.VISIBLE
            binding.layoutCompletion.visibility = View.GONE
            hasChecked = false
        }

        binding.btnFinish.setOnClickListener {
            finish()
        }
    }

    private fun showSuccessAnimation() {
        binding.llSelectedSyllables.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(200)
            .withEndAction {
                binding.llSelectedSyllables.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()

        val sparkleAnim = AnimationUtils.loadAnimation(this, R.anim.sparkle_rotate)
        binding.llSelectedSyllables.startAnimation(sparkleAnim)
    }

    private fun showErrorAnimation() {
        val shakeAnim = AnimationUtils.loadAnimation(this, R.anim.shake)
        binding.llSelectedSyllables.startAnimation(shakeAnim)
    }

    private fun showCompletionScreen() {
        binding.layoutGame.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.layoutGame.visibility = View.GONE
                binding.layoutGame.alpha = 1f

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
        val total = statistics["total"] as Int
        val percentage = statistics["percentage"] as Int
        val score = statistics["score"] as Int

        binding.tvFinalScore.text = "You completed all $total words!"
        binding.tvScoreText.text = "Score: $score"
        binding.tvCorrectCount.text = "$correct"
        binding.tvWrongCount.text = "$wrong"
        binding.tvAccuracyText.text = "$percentage%"

        // Animate trophy
        val sparkleAnim = AnimationUtils.loadAnimation(this, R.anim.sparkle_rotate)
        binding.ivTrophy.startAnimation(sparkleAnim)

        // Animate text appearance
        binding.tvCompletionTitle.alpha = 0f
        binding.tvCompletionTitle.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(300)
            .start()

        soundManager.playSuccessSound()
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

