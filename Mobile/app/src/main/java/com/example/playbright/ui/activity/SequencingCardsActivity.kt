package com.example.playbright.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.playbright.R
import com.example.playbright.databinding.ActivitySequencingCardsBinding
import com.example.playbright.ui.viewmodel.SequencingCardsViewModel
import com.google.android.material.card.MaterialCardView

class SequencingCardsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySequencingCardsBinding
    private val viewModel: SequencingCardsViewModel by viewModels()
    
    private var selectedOrder = mutableListOf<Int>() // Track which original card index is selected for each position (1, 2, 3, 4)
    private val imageCards = mutableListOf<MaterialCardView>()
    private val imageViews = mutableListOf<ImageView>()
    private val orderBadges = mutableListOf<TextView>()
    private var shuffledIndices = listOf<Int>() // Maps display position to original card index
    private var isProcessingSelection = false // Prevent double-tap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivitySequencingCardsBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val moduleIdFromIntent = intent.getStringExtra("MODULE_ID") ?: "sequencing-cards"
            viewModel.setModuleId(moduleIdFromIntent)

            setupToolbar()
            setupObservers()
            setupClickListeners()

            binding.layoutQuiz.visibility = View.VISIBLE
            binding.layoutCompletion.visibility = View.GONE

            // Initialize image cards and badges
            imageCards.addAll(listOf(
                binding.cardImage1,
                binding.cardImage2,
                binding.cardImage3,
                binding.cardImage4
            ))
            imageViews.addAll(listOf(
                binding.ivImage1,
                binding.ivImage2,
                binding.ivImage3,
                binding.ivImage4
            ))
            orderBadges.addAll(listOf(
                binding.tvOrder1,
                binding.tvOrder2,
                binding.tvOrder3,
                binding.tvOrder4
            ))

            viewModel.loadSequencingCards()
        } catch (e: Exception) {
            android.util.Log.e("SequencingCards", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading cards: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Order the Steps"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupObservers() {
        viewModel.currentCard.observe(this) { card ->
            card?.let {
                updateCardUI(it)
            }
        }

        viewModel.isCorrect.observe(this) { isCorrect ->
            isCorrect?.let {
                if (it) {
                    showSuccessFeedback()
                } else {
                    showErrorFeedback()
                }
            }
        }

        viewModel.score.observe(this) { score ->
            binding.tvScore.text = "$score"
        }

        viewModel.progress.observe(this) { progress ->
            binding.progressRing.progress = progress
            binding.tvProgressPercent.text = "$progress%"
        }

        viewModel.currentCardIndex.observe(this) { index ->
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
                if (it.contains("No cards available")) {
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
        val currentIndex = viewModel.currentCardIndex.value ?: 0
        val originalTotal = viewModel.originalTotalQuestions.value ?: 0
        val totalCards = viewModel.sequencingCards.value?.size ?: originalTotal
        binding.tvQuestionNumber.text = "Question ${currentIndex + 1}/$totalCards"
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Set click listeners for image cards
        imageCards.forEachIndexed { index, card ->
            card.setOnClickListener {
                handleImageSelection(index)
            }
        }

        binding.btnCheck.setOnClickListener {
            checkOrder()
        }

        binding.btnNext.setOnClickListener {
            viewModel.nextCard()
            binding.btnNext.visibility = View.GONE
            binding.btnCheck.visibility = View.VISIBLE
            resetSelection()
        }

        binding.btnPlayAgain.setOnClickListener {
            viewModel.restartQuiz()
            binding.layoutCompletion.visibility = View.GONE
            binding.layoutQuiz.visibility = View.VISIBLE
            resetSelection()
        }

        binding.btnHome.setOnClickListener {
            finish()
        }
    }

    private fun updateCardUI(card: com.example.playbright.data.model.SequencingCardResponse) {
        binding.tvTitle.text = card.title
        resetSelection()
        loadImages(card)
    }

    private fun loadImages(card: com.example.playbright.data.model.SequencingCardResponse) {
        val cards = card.cards
        
        // Shuffle the indices to randomize display order
        shuffledIndices = (0 until cards.size).shuffled()
        
        // Load up to 4 images in shuffled order
        for (displayIndex in 0 until minOf(4, cards.size)) {
            val originalIndex = shuffledIndices[displayIndex]
            val cardItem = cards[originalIndex]
            val imageUrl = cardItem.getImageUrlString()
            
            if (imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_error_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(imageViews[displayIndex])
            } else {
                imageViews[displayIndex].setImageResource(R.drawable.ic_placeholder_image)
            }
            
            // Make card visible and ensure badge is properly initialized
            imageCards[displayIndex].visibility = View.VISIBLE
            orderBadges[displayIndex].visibility = View.GONE // Reset badge visibility
            orderBadges[displayIndex].bringToFront() // Ensure badge is on top layer
        }
        
        // Hide unused cards
        for (i in cards.size until 4) {
            imageCards[i].visibility = View.GONE
            orderBadges[i].visibility = View.GONE
        }
    }

    private fun handleImageSelection(displayIndex: Int) {
        // Prevent double-tap/rapid taps
        if (isProcessingSelection) return
        
        val currentCard = viewModel.currentCard.value ?: return
        val cards = currentCard.cards
        
        // Ensure shuffledIndices is initialized and displayIndex is valid
        if (shuffledIndices.isEmpty() || displayIndex >= shuffledIndices.size || displayIndex >= orderBadges.size) {
            android.util.Log.w("SequencingCards", "Invalid displayIndex: $displayIndex, shuffledIndices.size: ${shuffledIndices.size}, badges.size: ${orderBadges.size}")
            return
        }
        
        // Get the original card index from the shuffled mapping
        val originalIndex = shuffledIndices[displayIndex]
        
        // Check if this image is already selected
        if (selectedOrder.contains(originalIndex)) {
            // Remove from selection
            isProcessingSelection = true
            val orderIndex = selectedOrder.indexOf(originalIndex)
            selectedOrder.removeAt(orderIndex)
            orderBadges[displayIndex].visibility = View.GONE
            
            // Reset card highlight
            imageCards[displayIndex].strokeWidth = 0
            imageCards[displayIndex].setStrokeColor(ContextCompat.getColor(this, android.R.color.transparent))
            
            // Update remaining badges
            updateOrderBadges()
            updateOrderDisplay()
            
            // Re-enable after a short delay
            Handler(Looper.getMainLooper()).postDelayed({
                isProcessingSelection = false
            }, 300)
            return
        }
        
        // Set processing flag
        isProcessingSelection = true
        
        // Find the next available order position (1, 2, 3, 4)
        val nextOrder = selectedOrder.size + 1
        
        // Add to selection (store original index)
        selectedOrder.add(originalIndex)
        val badge = orderBadges[displayIndex]
        badge.text = "$nextOrder" // Show number without parentheses for cleaner look
        
        // Make sure badge is visible and on top - do this before animation
        badge.visibility = View.VISIBLE
        badge.elevation = 20f // Ensure badge is above other views
        
        // Get parent FrameLayout and bring badge to front
        val parentFrame = badge.parent as? android.view.ViewGroup
        parentFrame?.bringChildToFront(badge)
        parentFrame?.elevation = 10f // Ensure parent is also elevated
        
        // Also bring the card to front to ensure badge is visible
        imageCards[displayIndex].bringToFront()
        
        // Add animation to badge for better visibility
        badge.alpha = 0f
        badge.scaleX = 0.5f
        badge.scaleY = 0.5f
        badge.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .start()
        
        // Highlight the card with thicker stroke for better visibility
        imageCards[displayIndex].strokeWidth = 6
        imageCards[displayIndex].setStrokeColor(ContextCompat.getColor(this, R.color.primary_blue))
        
        // Add haptic feedback for better user experience
        imageCards[displayIndex].performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        
        android.util.Log.d("SequencingCards", "Badge $nextOrder shown on image $displayIndex, badge visibility: ${badge.visibility}, text: ${badge.text}")
        
        updateOrderDisplay()
        
        // Re-enable after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            isProcessingSelection = false
        }, 300)
    }

    private fun updateOrderBadges() {
        // Update all badges based on current selection
        orderBadges.forEachIndexed { displayIndex, badge ->
            if (displayIndex < shuffledIndices.size) {
                val originalIndex = shuffledIndices[displayIndex]
                if (selectedOrder.contains(originalIndex)) {
                    val order = selectedOrder.indexOf(originalIndex) + 1
                    badge.text = "$order" // Show number without parentheses
                    badge.visibility = View.VISIBLE
                    badge.bringToFront() // Ensure badge is always on top
                    
                    // Get parent FrameLayout and bring badge to front
                    val parentFrame = badge.parent as? android.view.ViewGroup
                    parentFrame?.bringChildToFront(badge)
                } else {
                    badge.visibility = View.GONE
                }
            } else {
                badge.visibility = View.GONE
            }
        }
    }

    private fun updateOrderDisplay() {
        val container = binding.llOrderDisplay
        container.removeAllViews()
        
        selectedOrder.forEachIndexed { position, imageIndex ->
            val badge = TextView(this).apply {
                text = "${position + 1}"
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.primary_blue))
                setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
                background = ContextCompat.getDrawable(context, R.drawable.bg_star_badge)
            }
            
            container.addView(badge)
            
            // Add arrow between items (except last)
            if (position < selectedOrder.size - 1) {
            val arrow = TextView(this).apply {
                text = "→"
                textSize = 20f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setPadding(8.dpToPx(), 0, 8.dpToPx(), 0)
                gravity = android.view.Gravity.CENTER
            }
                container.addView(arrow)
            }
        }
    }

    private fun checkOrder() {
        val currentCard = viewModel.currentCard.value ?: return
        val cards = currentCard.cards
        
        // Check if all cards are selected
        if (selectedOrder.size != cards.size) {
            Toast.makeText(this, "Please select all ${cards.size} images in order", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Convert selected order to the order expected by ViewModel
        // ViewModel expects a list of card indices in the order they should appear
        viewModel.updateUserOrder(selectedOrder)
        val isCorrect = viewModel.checkOrder()
        
        if (isCorrect) {
            // Show success - cards will be highlighted in green
            selectedOrder.forEachIndexed { position, originalIndex ->
                // Find the display index for this original index
                val displayIndex = shuffledIndices.indexOf(originalIndex)
                if (displayIndex >= 0 && displayIndex < imageCards.size) {
                    imageCards[displayIndex].strokeWidth = 6
                    imageCards[displayIndex].setStrokeColor(ContextCompat.getColor(this, R.color.correct_answer))
                }
            }
        } else {
            // Show error - highlight wrong cards in red
            selectedOrder.forEachIndexed { position, originalIndex ->
                // Find the display index for this original index
                val displayIndex = shuffledIndices.indexOf(originalIndex)
                if (displayIndex >= 0 && displayIndex < imageCards.size) {
                    imageCards[displayIndex].strokeWidth = 6
                    imageCards[displayIndex].setStrokeColor(ContextCompat.getColor(this, R.color.wrong_answer))
                }
            }
            
            // Show correct order after delay
            Handler(Looper.getMainLooper()).postDelayed({
                showCorrectOrder()
            }, 2000)
        }
    }

    private fun showCorrectOrder() {
        val currentCard = viewModel.currentCard.value ?: return
        val cards = currentCard.cards
        
        // Show correct order - highlight all cards, correct ones in green
        val correctOrder = cards.mapIndexed { index, card ->
            index to card.order
        }.sortedBy { it.second }.map { it.first }
        
        // Highlight cards based on their display position
        shuffledIndices.forEachIndexed { displayIndex, originalIndex ->
            if (displayIndex < imageCards.size) {
                imageCards[displayIndex].strokeWidth = 6
                if (correctOrder.contains(originalIndex)) {
                    imageCards[displayIndex].setStrokeColor(ContextCompat.getColor(this, R.color.correct_answer))
                } else {
                    imageCards[displayIndex].setStrokeColor(ContextCompat.getColor(this, R.color.wrong_answer))
                }
            }
        }
        
        // Reset after showing correct order
        Handler(Looper.getMainLooper()).postDelayed({
            resetSelection()
        }, 2000)
    }

    private fun resetSelection() {
        selectedOrder.clear()
        orderBadges.forEach { it.visibility = View.GONE }
        imageCards.forEach { card ->
            card.strokeWidth = 0
            card.setStrokeColor(ContextCompat.getColor(this, android.R.color.transparent))
        }
        binding.llOrderDisplay.removeAllViews()
        // Note: shuffledIndices will be regenerated when loadImages is called
    }

    private fun showSuccessFeedback() {
        binding.ivStar.visibility = View.VISIBLE
        val animation = AnimationUtils.loadAnimation(this, R.anim.bounce)
        binding.ivStar.startAnimation(animation)
        
        Handler(Looper.getMainLooper()).postDelayed({
            binding.ivStar.visibility = View.GONE
            binding.btnNext.visibility = View.VISIBLE
            binding.btnCheck.visibility = View.GONE
        }, 2000)
    }

    private fun showErrorFeedback() {
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
        val starsEarned = statistics["starsEarned"] as Int

        binding.tvFinalScore.text = "Module Complete!"
        binding.tvScoreText.text = "You scored $score out of $totalAttempted!\nEarned $starsEarned stars"

        binding.tvCorrectCount.text = "$correct"
        binding.tvWrongCount.text = "$wrong"
        binding.tvAccuracy.text = "$percentage%"

        // Ensure final statistics are saved to Firebase
        // markModuleAsCompleted() is already called in nextCard(), but we ensure it's saved here too
        viewModel.markModuleAsCompleted()

        val animation = AnimationUtils.loadAnimation(this, R.anim.celebration)
        binding.ivTrophy.startAnimation(animation)
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
