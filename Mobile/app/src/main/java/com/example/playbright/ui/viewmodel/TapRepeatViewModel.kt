package com.example.playbright.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playbright.data.model.TapRepeatResponse
import com.example.playbright.data.model.UpdateProgressRequest
import com.example.playbright.data.network.ApiErrorHandler
import com.example.playbright.data.repository.ApiRepository
import com.example.playbright.data.repository.AuthRepository
import kotlinx.coroutines.launch

class TapRepeatViewModel : ViewModel() {

    private val repository = ApiRepository()
    private val authRepository = AuthRepository()
    private var MODULE_ID = "tap-repeat" // Default module ID
    
    fun setModuleId(moduleId: String) {
        MODULE_ID = moduleId
    }
    
    fun getModuleId(): String = MODULE_ID

    private val _words = MutableLiveData<List<TapRepeatResponse>>()
    val words: LiveData<List<TapRepeatResponse>> = _words

    private val _currentWordIndex = MutableLiveData<Int>(0)
    val currentWordIndex: LiveData<Int> = _currentWordIndex

    private val _currentWord = MutableLiveData<TapRepeatResponse?>()
    val currentWord: LiveData<TapRepeatResponse?> = _currentWord

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isCompleted = MutableLiveData<Boolean>(false)
    val isCompleted: LiveData<Boolean> = _isCompleted

    private val _totalWords = MutableLiveData<Int>(0)
    val totalWords: LiveData<Int> = _totalWords

    private val _completedWords = MutableLiveData<Int>(0)
    val completedWords: LiveData<Int> = _completedWords

    private val _correctAnswers = MutableLiveData<Int>(0)
    val correctAnswers: LiveData<Int> = _correctAnswers

    private val _wrongAnswers = MutableLiveData<Int>(0)
    val wrongAnswers: LiveData<Int> = _wrongAnswers

    // Track which words have been attempted
    private val attemptedWords = mutableSetOf<String>()

    fun loadWords() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                android.util.Log.d("TapRepeatViewModel", "Loading words for module: $MODULE_ID")
                val response = repository.getTapRepeats(
                    moduleId = MODULE_ID,
                    status = "Active"
                )

                android.util.Log.d("TapRepeatViewModel", "Response status: ${response.isSuccessful}, Code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val wordsList = response.body() ?: emptyList()
                    android.util.Log.d("TapRepeatViewModel", "Loaded ${wordsList.size} words from backend")
                    
                    // Shuffle words for random order
                    val shuffledWords = wordsList.shuffled()
                    _words.value = shuffledWords
                    _totalWords.value = shuffledWords.size

                    if (shuffledWords.isNotEmpty()) {
                        _currentWordIndex.value = 0
                        _currentWord.value = shuffledWords[0]
                        android.util.Log.d("TapRepeatViewModel", "✅ Words loaded successfully")
                    } else {
                        android.util.Log.w("TapRepeatViewModel", "⚠️ No words found for module: $MODULE_ID")
                        _error.value = "No words available for this module. Please add words in the admin panel."
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    android.util.Log.e("TapRepeatViewModel", "❌ Failed to load words: ${response.code()} - $errorBody")
                    _error.value = "Failed to load words: $errorBody"
                }
            } catch (e: Exception) {
                android.util.Log.e("TapRepeatViewModel", "❌ Exception loading words: ${e.message}", e)
                val errorMessage = ApiErrorHandler.handleError(e)
                _error.value = errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun nextWord() {
        val currentIndex = _currentWordIndex.value ?: 0
        val wordsList = _words.value ?: emptyList()

        if (currentIndex < wordsList.size - 1) {
            _currentWordIndex.value = currentIndex + 1
            _currentWord.value = wordsList[currentIndex + 1]
        } else {
            _isCompleted.value = true
            markModuleAsCompleted()
        }
    }

    fun markWordAsAttempted(wordId: String, isCorrect: Boolean) {
        // Track attempt in backend
        updateProgressInBackend(wordId, isCorrect)
        
        // Track correct/wrong answers
        if (isCorrect) {
            _correctAnswers.value = (_correctAnswers.value ?: 0) + 1
        } else {
            _wrongAnswers.value = (_wrongAnswers.value ?: 0) + 1
        }
        
        if (!attemptedWords.contains(wordId)) {
            attemptedWords.add(wordId)
            _completedWords.value = attemptedWords.size
        }
    }

    private fun updateProgressInBackend(wordId: String, isCorrect: Boolean) {
        val currentUser = authRepository.getCurrentUser() ?: return
        val studentId = currentUser.studentId ?: currentUser.id
        
        viewModelScope.launch {
            try {
                val request = UpdateProgressRequest(
                    studentId = studentId,
                    moduleId = MODULE_ID,
                    questionId = wordId,
                    isCorrect = isCorrect,
                    score = _correctAnswers.value,
                    totalQuestions = _totalWords.value,
                    currentQuestion = _currentWordIndex.value,
                    correctAnswers = _correctAnswers.value,
                    wrongAnswers = _wrongAnswers.value
                )
                repository.updateProgress(studentId, MODULE_ID, request)
                android.util.Log.d("TapRepeatViewModel", "✅ Progress updated: wordId=$wordId, isCorrect=$isCorrect")
            } catch (e: Exception) {
                android.util.Log.e("TapRepeatViewModel", "❌ Failed to update progress: ${e.message}")
            }
        }
    }

    private fun markModuleAsCompleted() {
        val currentUser = authRepository.getCurrentUser() ?: return
        val studentId = currentUser.studentId ?: currentUser.id
        val wordsList = _words.value ?: emptyList()
        
        viewModelScope.launch {
            try {
                val request = UpdateProgressRequest(
                    studentId = studentId,
                    moduleId = MODULE_ID,
                    questionId = null,
                    isCorrect = null,
                    score = _correctAnswers.value,
                    totalQuestions = wordsList.size,
                    currentQuestion = wordsList.size,
                    correctAnswers = _correctAnswers.value,
                    wrongAnswers = _wrongAnswers.value
                )
                repository.updateProgress(studentId, MODULE_ID, request)
                android.util.Log.d("TapRepeatViewModel", "✅ Module marked as completed")
            } catch (e: Exception) {
                android.util.Log.e("TapRepeatViewModel", "❌ Failed to mark module as completed: ${e.message}")
            }
        }
    }

    fun restartPractice() {
        val currentWords = _words.value ?: emptyList()
        val reshuffledWords = currentWords.shuffled()
        _words.value = reshuffledWords
        
        _currentWordIndex.value = 0
        _isCompleted.value = false
        attemptedWords.clear()
        _completedWords.value = 0
        _correctAnswers.value = 0
        _wrongAnswers.value = 0

        if (reshuffledWords.isNotEmpty()) {
            _currentWord.value = reshuffledWords[0]
        }
    }

    fun getProgress(): Int {
        val total = _totalWords.value ?: 0
        val completed = _completedWords.value ?: 0
        return if (total > 0) {
            (completed * 100) / total
        } else {
            0
        }
    }
}



