package com.example.playbright.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playbright.data.model.SequencingCardResponse
import com.example.playbright.data.model.UpdateProgressRequest
import com.example.playbright.data.network.ApiErrorHandler
import com.example.playbright.data.repository.ApiRepository
import com.example.playbright.data.repository.AuthRepository
import kotlinx.coroutines.launch

class SequencingCardsViewModel : ViewModel() {

    private val repository = ApiRepository()
    private val authRepository = AuthRepository()
    private var MODULE_ID = "sequencing-cards"
    
    fun setModuleId(moduleId: String) {
        MODULE_ID = moduleId
    }
    
    fun getModuleId(): String = MODULE_ID

    private val _sequencingCards = MutableLiveData<List<SequencingCardResponse>>()
    val sequencingCards: LiveData<List<SequencingCardResponse>> = _sequencingCards

    private val _currentCardIndex = MutableLiveData<Int>(0)
    val currentCardIndex: LiveData<Int> = _currentCardIndex

    private val _currentCard = MutableLiveData<SequencingCardResponse?>()
    val currentCard: LiveData<SequencingCardResponse?> = _currentCard

    private val _score = MutableLiveData<Int>(0)
    val score: LiveData<Int> = _score

    private val _progress = MutableLiveData<Int>(0)
    val progress: LiveData<Int> = _progress

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isCompleted = MutableLiveData<Boolean>(false)
    val isCompleted: LiveData<Boolean> = _isCompleted

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isCorrect = MutableLiveData<Boolean?>()
    val isCorrect: LiveData<Boolean?> = _isCorrect

    private val _correctAnswers = MutableLiveData<Int>(0)
    val correctAnswers: LiveData<Int> = _correctAnswers

    private val _wrongAnswers = MutableLiveData<Int>(0)
    val wrongAnswers: LiveData<Int> = _wrongAnswers

    private val _originalTotalQuestions = MutableLiveData<Int>(0)
    val originalTotalQuestions: LiveData<Int> = _originalTotalQuestions

    private var userOrder = mutableListOf<Int>() // User's selected order

    fun loadSequencingCards() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                android.util.Log.d("SequencingCardsViewModel", "Loading cards for module: $MODULE_ID")
                val response = repository.getSequencingCards(
                    moduleId = MODULE_ID,
                    status = "Active"
                )

                android.util.Log.d("SequencingCardsViewModel", "Response status: ${response.isSuccessful}, Code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val cardsList = response.body() ?: emptyList()
                    android.util.Log.d("SequencingCardsViewModel", "Loaded ${cardsList.size} cards from backend")
                    
                    // Shuffle cards for random order
                    val shuffledCards = cardsList.shuffled()
                    _sequencingCards.value = shuffledCards

                    if (shuffledCards.isNotEmpty()) {
                        _currentCardIndex.value = 0
                        _currentCard.value = shuffledCards[0]
                        _originalTotalQuestions.value = shuffledCards.size
                        updateProgress()
                        loadProgress()
                        android.util.Log.d("SequencingCardsViewModel", "✅ Cards loaded successfully")
                    } else {
                        android.util.Log.w("SequencingCardsViewModel", "⚠️ No cards found for module: $MODULE_ID")
                        _error.value = "No cards available for this module. Please add cards in the admin panel."
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    android.util.Log.e("SequencingCardsViewModel", "❌ Failed to load cards: ${response.code()} - $errorBody")
                    _error.value = "Failed to load cards: $errorBody"
                }
            } catch (e: Exception) {
                android.util.Log.e("SequencingCardsViewModel", "❌ Exception loading cards: ${e.message}", e)
                val errorMessage = ApiErrorHandler.handleError(e)
                _error.value = errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUserOrder(order: List<Int>) {
        userOrder = order.toMutableList()
    }

    fun checkOrder(): Boolean {
        val currentCard = _currentCard.value ?: return false
        val cards = currentCard.cards
        
        if (userOrder.size != cards.size) {
            return false
        }

        // Check if user order matches correct order
        val correctOrder = cards.mapIndexed { index, card ->
            index to card.order
        }.sortedBy { it.second }.map { it.first }

        val isCorrect = userOrder == correctOrder

        if (isCorrect) {
            _correctAnswers.value = (_correctAnswers.value ?: 0) + 1
            _score.value = (_score.value ?: 0) + 1
        } else {
            _wrongAnswers.value = (_wrongAnswers.value ?: 0) + 1
        }

        _isCorrect.value = isCorrect
        updateProgressInBackend(currentCard.id, isCorrect)
        updateProgress()

        return isCorrect
    }

    private fun updateProgressInBackend(cardId: String, isCorrect: Boolean) {
        val currentUser = authRepository.getCurrentUser() ?: return
        val studentId = currentUser.studentId ?: currentUser.id
        val currentIndex = _currentCardIndex.value ?: 0
        val cardsList = _sequencingCards.value ?: emptyList()

        viewModelScope.launch {
            try {
                val request = UpdateProgressRequest(
                    studentId = studentId,
                    moduleId = MODULE_ID,
                    questionId = cardId,
                    isCorrect = isCorrect,
                    score = _score.value ?: 0,
                    totalQuestions = cardsList.size,
                    currentQuestion = currentIndex + 1,
                    correctAnswers = _correctAnswers.value,
                    wrongAnswers = _wrongAnswers.value
                )

                repository.updateProgress(studentId, MODULE_ID, request)
            } catch (e: Exception) {
                // Silently fail - progress will be synced later
            }
        }
    }

    fun nextCard() {
        val currentIndex = _currentCardIndex.value ?: 0
        val cardsList = _sequencingCards.value ?: emptyList()

        if (currentIndex < cardsList.size - 1) {
            _currentCardIndex.value = currentIndex + 1
            _currentCard.value = cardsList[currentIndex + 1]
            userOrder.clear()
            _isCorrect.value = null
            updateProgress()
        } else {
            // Module completed
            _isCompleted.value = true
            markModuleAsCompleted()
        }
    }

    fun markModuleAsCompleted() {
        val currentUser = authRepository.getCurrentUser() ?: return
        val studentId = currentUser.studentId ?: currentUser.id
        val cardsList = _sequencingCards.value ?: emptyList()
        val finalScore = _score.value ?: 0
        val finalCorrect = _correctAnswers.value ?: 0
        val finalWrong = _wrongAnswers.value ?: 0

        viewModelScope.launch {
            try {
                val request = UpdateProgressRequest(
                    studentId = studentId,
                    moduleId = MODULE_ID,
                    questionId = null,
                    isCorrect = null,
                    score = finalScore,
                    totalQuestions = cardsList.size,
                    currentQuestion = cardsList.size,
                    correctAnswers = finalCorrect,
                    wrongAnswers = finalWrong
                )

                android.util.Log.d("SequencingCards", "Saving final statistics: score=$finalScore, correct=$finalCorrect, wrong=$finalWrong, total=${cardsList.size}")
                repository.updateProgress(studentId, MODULE_ID, request)
                android.util.Log.d("SequencingCards", "Final statistics saved successfully")
            } catch (e: Exception) {
                android.util.Log.e("SequencingCards", "Error marking module as completed: ${e.message}", e)
            }
        }
    }

    private fun updateProgress() {
        val correct = _correctAnswers.value ?: 0
        val wrong = _wrongAnswers.value ?: 0
        val totalAnswered = correct + wrong
        val originalTotal = _originalTotalQuestions.value ?: 1
        
        val progressValue = if (originalTotal > 0) {
            ((totalAnswered * 100) / originalTotal).coerceAtMost(100)
        } else {
            100
        }
        _progress.value = progressValue
    }

    fun restartQuiz() {
        val currentCards = _sequencingCards.value ?: emptyList()
        val reshuffledCards = currentCards.shuffled()
        _sequencingCards.value = reshuffledCards
        
        _currentCardIndex.value = 0
        _score.value = 0
        _progress.value = 0
        _isCompleted.value = false
        _correctAnswers.value = 0
        _wrongAnswers.value = 0
        userOrder.clear()
        _isCorrect.value = null

        if (reshuffledCards.isNotEmpty()) {
            _originalTotalQuestions.value = reshuffledCards.size
            _currentCard.value = reshuffledCards[0]
            updateProgress()
        }
    }

    fun getStatistics(): Map<String, Any> {
        val correct = _correctAnswers.value ?: 0
        val wrong = _wrongAnswers.value ?: 0
        val totalAttempted = correct + wrong
        val originalTotal = _originalTotalQuestions.value ?: 0
        
        val percentage = if (totalAttempted > 0) {
            (correct * 100) / totalAttempted
        } else {
            0
        }
        
        return mapOf(
            "correct" to correct,
            "wrong" to wrong,
            "total" to originalTotal,
            "totalAttempted" to totalAttempted,
            "percentage" to percentage,
            "score" to (_score.value ?: 0)
        )
    }

    private fun loadProgress() {
        // Load existing progress if needed
        // This can be implemented later if needed
    }
}
