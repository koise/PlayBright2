package com.example.playbright.ui.activity

import android.graphics.PointF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.playbright.R
import com.example.playbright.databinding.ActivityMatchingPairsBinding
import com.example.playbright.ui.view.LineDrawingView
import com.example.playbright.ui.viewmodel.MatchingPairsViewModel
import com.example.playbright.utils.SoundManager
import com.google.android.material.card.MaterialCardView

class MatchingPairsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMatchingPairsBinding
    private val viewModel: MatchingPairsViewModel by viewModels()
    private lateinit var soundManager: SoundManager

    private val leftCards = mutableListOf<MaterialCardView>()
    private val rightCards = mutableListOf<MaterialCardView>()
    private val leftImageViews = mutableListOf<ImageView>()
    private val rightImageViews = mutableListOf<ImageView>()
    private val leftTextViews = mutableListOf<TextView>()
    private val rightTextViews = mutableListOf<TextView>()

    private val leftCardPositions = mutableMapOf<Int, PointF>() // Left card index -> center position
    private val rightCardPositions = mutableMapOf<Int, PointF>() // Right card index -> center position
    private var currentPairs: List<com.example.playbright.data.model.MatchingPairResponse> = emptyList()
    private var shuffledRightIndices = listOf<Int>() // Maps right position to original pair index
    private var currentStartCardIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMatchingPairsBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val moduleIdFromIntent = intent.getStringExtra("MODULE_ID") ?: "matching-pairs"
            viewModel.setModuleId(moduleIdFromIntent)

            soundManager = SoundManager.getInstance(this)

            setupCardLists()
            setupObservers()
            setupClickListeners()

            soundManager.startBackgroundMusic()

            viewModel.loadPairs()
        } catch (e: Exception) {
            android.util.Log.e("MatchingPairs", "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading activity: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupCardLists() {
        leftCards.addAll(listOf(
            binding.cardItem11,
            binding.cardItem12,
            binding.cardItem13,
            binding.cardItem14
        ))

        rightCards.addAll(listOf(
            binding.cardItem21,
            binding.cardItem22,
            binding.cardItem23,
            binding.cardItem24
        ))

        leftImageViews.addAll(listOf(
            binding.ivItem11,
            binding.ivItem12,
            binding.ivItem13,
            binding.ivItem14
        ))

        rightImageViews.addAll(listOf(
            binding.ivItem21,
            binding.ivItem22,
            binding.ivItem23,
            binding.ivItem24
        ))

        leftTextViews.addAll(listOf(
            binding.tvItem11,
            binding.tvItem12,
            binding.tvItem13,
            binding.tvItem14
        ))

        rightTextViews.addAll(listOf(
            binding.tvItem21,
            binding.tvItem22,
            binding.tvItem23,
            binding.tvItem24
        ))
    }

    private fun setupObservers() {
        viewModel.currentPairs.observe(this) { pairs ->
            if (pairs.isNotEmpty()) {
                currentPairs = pairs
                displayPairs(pairs)
            }
        }

        viewModel.matchedPairs.observe(this) { matched ->
            updateMatchedCards(matched)
            
            if (viewModel.areAllMatched()) {
                showAllMatchedMessage()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // Show loading state if needed
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                if (it.contains("No matching pairs available")) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 3000)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.btnClear.setOnClickListener {
            binding.lineDrawingView.clearLines()
            viewModel.resetMatches()
            updateMatchedCards(emptySet())
        }

        binding.btnSubmit.setOnClickListener {
            if (viewModel.areAllMatched()) {
                Toast.makeText(this, "🎉 Great job! All pairs matched correctly!", Toast.LENGTH_LONG).show()
                soundManager.playSuccessSound()
                
                // Load new random pairs after a delay
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.lineDrawingView.clearLines()
                    viewModel.resetMatches()
                    viewModel.loadRandomPairs()
                }, 2000)
            } else {
                val matched = viewModel.matchedPairs.value?.size ?: 0
                val total = viewModel.currentPairs.value?.size ?: 0
                Toast.makeText(this, "Please match all $total pairs! ($matched/$total matched)", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup line drawing listeners
        binding.lineDrawingView.onTouchStart = { x, y ->
            findNearestCardIndex(x, y, isLeft = true).also {
                currentStartCardIndex = it
            }
        }

        binding.lineDrawingView.onTouchEnd = { x, y ->
            findNearestCardIndex(x, y, isLeft = false)
        }

        binding.lineDrawingView.onLineDrawn = { startX, startY, endX, endY ->
            handleLineDrawn(startX, startY, endX, endY)
        }

        // Calculate card positions after layout
        binding.llLeftColumn.post {
            calculateCardPositions()
        }
    }

    private fun calculateCardPositions() {
        leftCardPositions.clear()
        rightCardPositions.clear()
        
        // Calculate left card positions relative to LineDrawingView
        leftCards.forEachIndexed { index, card ->
            val location = IntArray(2)
            card.getLocationInWindow(location)
            val viewLocation = IntArray(2)
            binding.lineDrawingView.getLocationInWindow(viewLocation)
            val x = (location[0] - viewLocation[0]) + card.width / 2f
            val y = (location[1] - viewLocation[1]) + card.height / 2f
            leftCardPositions[index] = PointF(x, y)
        }

        // Calculate right card positions relative to LineDrawingView
        rightCards.forEachIndexed { index, card ->
            val location = IntArray(2)
            card.getLocationInWindow(location)
            val viewLocation = IntArray(2)
            binding.lineDrawingView.getLocationInWindow(viewLocation)
            val x = (location[0] - viewLocation[0]) + card.width / 2f
            val y = (location[1] - viewLocation[1]) + card.height / 2f
            rightCardPositions[index] = PointF(x, y)
        }
    }

    private fun displayPairs(pairs: List<com.example.playbright.data.model.MatchingPairResponse>) {
        // Shuffle right side items
        shuffledRightIndices = (0 until pairs.size).shuffled()

        // Display left side (item1s) in order
        pairs.forEachIndexed { index, pair ->
            if (index < leftCards.size) {
                // Load image
                if (!pair.item1ImageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(pair.item1ImageUrl)
                        .placeholder(R.drawable.ic_placeholder_image)
                        .error(R.drawable.ic_error_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(leftImageViews[index])
                } else {
                    leftImageViews[index].setImageResource(R.drawable.ic_placeholder_image)
                }

                // Set label
                leftTextViews[index].text = pair.item1Label

                // Animate card entrance
                leftCards[index].alpha = 0f
                leftCards[index].animate()
                    .alpha(1f)
                    .setDuration(400)
                    .setStartDelay((index * 100L).toLong())
                    .start()
            }
        }

        // Display right side (item2s) in shuffled order
        shuffledRightIndices.forEachIndexed { displayIndex, originalIndex ->
            if (displayIndex < rightCards.size && originalIndex < pairs.size) {
                val pair = pairs[originalIndex]

                // Load image
                if (!pair.item2ImageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(pair.item2ImageUrl)
                        .placeholder(R.drawable.ic_placeholder_image)
                        .error(R.drawable.ic_error_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(rightImageViews[displayIndex])
                } else {
                    rightImageViews[displayIndex].setImageResource(R.drawable.ic_placeholder_image)
                }

                // Set label
                rightTextViews[displayIndex].text = pair.item2Label

                // Animate card entrance
                rightCards[displayIndex].alpha = 0f
                rightCards[displayIndex].animate()
                    .alpha(1f)
                    .setDuration(400)
                    .setStartDelay((displayIndex * 100L + 400).toLong())
                    .start()
            }
        }

        // Recalculate positions after animation
        Handler(Looper.getMainLooper()).postDelayed({
            calculateCardPositions()
        }, 1000)
    }

    private fun handleLineDrawn(startX: Float, startY: Float, endX: Float, endY: Float) {
        if (currentStartCardIndex < 0) return

        val leftIndex = currentStartCardIndex
        val rightIndex = findNearestCardIndex(endX, endY, isLeft = false)

        if (leftIndex >= 0 && rightIndex >= 0 && leftIndex < currentPairs.size && rightIndex < shuffledRightIndices.size) {
            // Get the original pair index for the right card
            val rightOriginalIndex = shuffledRightIndices[rightIndex]
            val leftPair = currentPairs[leftIndex]
            val rightPair = currentPairs[rightOriginalIndex]

            // Check if they match
            val isMatch = leftPair.id == rightPair.id

            if (isMatch) {
                // Correct match - draw line from card center to card center
                val leftPos = leftCardPositions[leftIndex]
                val rightPos = rightCardPositions[rightIndex]
                
                if (leftPos != null && rightPos != null) {
                    binding.lineDrawingView.addLine(
                        leftPos.x, leftPos.y,
                        rightPos.x, rightPos.y,
                        isCorrect = true
                    )
                }

                soundManager.playSuccessSound()
                viewModel.addMatch(leftPair.id)
                
                // Highlight cards
                highlightCards(leftIndex, rightIndex, true)
            } else {
                // Wrong match
                soundManager.playErrorSound()
                
                // Remove the line after short delay
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.lineDrawingView.removeLine(startX, startY, endX, endY)
                }, 500)
            }
        }
        
        currentStartCardIndex = -1
    }

    private fun findNearestCardIndex(x: Float, y: Float, isLeft: Boolean): Int {
        var nearestIndex = -1
        var minDistance = Float.MAX_VALUE

        val positions = if (isLeft) leftCardPositions else rightCardPositions

        positions.forEach { (index, position) ->
            val distance = Math.sqrt(
                Math.pow((position.x - x).toDouble(), 2.0) +
                Math.pow((position.y - y).toDouble(), 2.0)
            ).toFloat()

            if (distance < minDistance && distance < 200f) { // Threshold for card detection
                minDistance = distance
                nearestIndex = index
            }
        }

        return nearestIndex
    }

    private fun highlightCards(leftIndex: Int, rightIndex: Int, isCorrect: Boolean) {
        val color = if (isCorrect) {
            ContextCompat.getColor(this, R.color.success_green)
        } else {
            ContextCompat.getColor(this, R.color.error_red)
        }

        if (leftIndex < leftCards.size) {
            leftCards[leftIndex].strokeColor = color
            leftCards[leftIndex].strokeWidth = 4
        }

        if (rightIndex < rightCards.size) {
            rightCards[rightIndex].strokeColor = color
            rightCards[rightIndex].strokeWidth = 4
        }
    }

    private fun updateMatchedCards(matched: Set<String>) {
        currentPairs.forEachIndexed { index, pair ->
            val isMatched = matched.contains(pair.id)
            
            if (isMatched) {
                // Find the right card position for this pair
                val rightDisplayIndex = shuffledRightIndices.indexOf(index)
                if (rightDisplayIndex >= 0 && rightDisplayIndex < rightCards.size) {
                    highlightCards(index, rightDisplayIndex, true)
                }
            }
        }
    }

    private fun showAllMatchedMessage() {
        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(this, "🎉 Great job! All pairs matched!", Toast.LENGTH_LONG).show()
            soundManager.playSuccessSound()
        }, 500)
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

