package com.example.playbright.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playbright.data.model.WordBuilderResponse
import com.example.playbright.data.model.UpdateProgressRequest
import com.example.playbright.data.network.ApiErrorHandler
import com.example.playbright.data.repository.ApiRepository
import com.example.playbright.data.repository.AuthRepository
import kotlinx.coroutines.launch

class WordBuilderViewModel : ViewModel() {

    private val repository = ApiRepository()
    private val authRepository = AuthRepository()
    private var MODULE_ID = "word-builder" // Default module ID
    
    fun setModuleId(moduleId: String) {
        MODULE_ID = moduleId
    }
    
    fun getModuleId(): String = MODULE_ID

    private val _words = MutableLiveData<List<WordBuilderResponse>>()
    val words: LiveData<List<WordBuilderResponse>> = _words

    private val _currentWordIndex = MutableLiveData<Int>(0)
    val currentWordIndex: LiveData<Int> = _currentWordIndex

    private val _currentWord = MutableLiveData<WordBuilderResponse?>()
    val currentWord: LiveData<WordBuilderResponse?> = _currentWord

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

    private val _correctAnswers = MutableLiveData<Int>(0)
    val correctAnswers: LiveData<Int> = _correctAnswers

    private val _wrongAnswers = MutableLiveData<Int>(0)
    val wrongAnswers: LiveData<Int> = _wrongAnswers

    private val _totalWords = MutableLiveData<Int>(0)
    val totalWords: LiveData<Int> = _totalWords

    private val _completedWords = MutableLiveData<Int>(0)
    val completedWords: LiveData<Int> = _completedWords

    // Track selected syllables in order
    private val selectedSyllables = mutableListOf<String>()
    
    // Track which words have been attempted
    private val attemptedWords = mutableSetOf<String>()

    fun loadWords() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                android.util.Log.d("WordBuilderViewModel", "Loading words for module: $MODULE_ID")
                val response = repository.getWordBuilders(
                    moduleId = MODULE_ID,
                    status = "Active"
                )

                android.util.Log.d("WordBuilderViewModel", "Response status: ${response.isSuccessful}, Code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val wordsList = response.body() ?: emptyList()
                    android.util.Log.d("WordBuilderViewModel", "Loaded ${wordsList.size} words from backend")
                    
                    if (wordsList.isEmpty()) {
                        android.util.Log.w("WordBuilderViewModel", "⚠️ No words found for module: $MODULE_ID")
                        _error.value = "No word builder items available for this module. Please add words in the admin panel."
                        _isLoading.value = false
                        return@launch
                    }

                    // Shuffle and take random words (e.g., 4-6 words)
                    val shuffledWords = wordsList.shuffled().take(minOf(6, wordsList.size))
                    _words.value = shuffledWords
                    _totalWords.value = shuffledWords.size
                    _currentWordIndex.value = 0
                    _currentWord.value = shuffledWords.firstOrNull()
                    android.util.Log.d("WordBuilderViewModel", "✅ Words loaded successfully")
                    _isLoading.value = false
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    android.util.Log.e("WordBuilderViewModel", "❌ Failed to load words: ${response.code()} - $errorBody")
                    _error.value = "Failed to load words: $errorBody"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("WordBuilderViewModel", "❌ Exception loading words: ${e.message}", e)
                val errorMessage = ApiErrorHandler.handleError(e)
                _error.value = errorMessage
                _isLoading.value = false
            }
        }
    }

    fun addSyllable(syllable: String) {
        selectedSyllables.add(syllable)
    }

    fun removeSyllable(index: Int) {
        if (index in selectedSyllables.indices) {
            selectedSyllables.removeAt(index)
        }
    }

    fun clearSelectedSyllables() {
        selectedSyllables.clear()
    }

    fun getSelectedSyllables(): List<String> = selectedSyllables.toList()

    fun checkWord(): Boolean {
        val current = _currentWord.value ?: return false
        
        // Normalize syllables first (remove spaces, hyphens, special dashes, trim)
        val normalizeSyllable: (String) -> String = { syllable ->
            syllable.replace(" ", "")
                .replace("-", "")
                .replace("–", "") // En dash
                .replace("—", "") // Em dash
                .trim()
                .lowercase()
        }
        
        val normalizedSelectedSyllables = selectedSyllables.map(normalizeSyllable)
        val normalizedExpectedSyllables = current.syllables.map(normalizeSyllable)
        
        // Method 1: Compare joined syllables
        val selectedWord = normalizedSelectedSyllables.joinToString("")
        val targetWord = current.word
            .replace(" ", "")
            .replace("-", "")
            .replace("–", "") // En dash
            .replace("—", "") // Em dash
            .lowercase()
            .trim()
        
        // Method 2: Compare syllable arrays directly (in order)
        val syllablesMatch = normalizedSelectedSyllables.size == normalizedExpectedSyllables.size &&
                normalizedSelectedSyllables.zip(normalizedExpectedSyllables).all { (selected, expected) ->
                    selected == expected
                }
        
        // Method 3: Compare joined word
        val wordMatches = selectedWord == targetWord
        
        // Accept if either method confirms correctness
        val isCorrect = wordMatches || syllablesMatch
        
        // Log for debugging
        android.util.Log.d("WordBuilder", "Checking word:")
        android.util.Log.d("WordBuilder", "  Selected syllables (original): ${selectedSyllables.joinToString(", ")}")
        android.util.Log.d("WordBuilder", "  Selected syllables (normalized): ${normalizedSelectedSyllables.joinToString(", ")}")
        android.util.Log.d("WordBuilder", "  Expected syllables (original): ${current.syllables.joinToString(", ")}")
        android.util.Log.d("WordBuilder", "  Expected syllables (normalized): ${normalizedExpectedSyllables.joinToString(", ")}")
        android.util.Log.d("WordBuilder", "  Selected word (joined): '$selectedWord'")
        android.util.Log.d("WordBuilder", "  Target word (original): '${current.word}'")
        android.util.Log.d("WordBuilder", "  Target word (normalized): '$targetWord'")
        android.util.Log.d("WordBuilder", "  Word matches: $wordMatches")
        android.util.Log.d("WordBuilder", "  Syllables match: $syllablesMatch")
        android.util.Log.d("WordBuilder", "  Final result: $isCorrect")
        
        if (isCorrect) {
            val newScore = (_score.value ?: 0) + 1
            _score.value = newScore
            _correctAnswers.value = (_correctAnswers.value ?: 0) + 1
            attemptedWords.add(current.id)
            _completedWords.value = attemptedWords.size
            
            // Update progress
            updateProgress(current.id, true)
        } else {
            _wrongAnswers.value = (_wrongAnswers.value ?: 0) + 1
            updateProgress(current.id, false)
        }
        
        // Calculate progress percentage
        val total = _totalWords.value ?: 1
        val completed = _completedWords.value ?: 0
        _progress.value = if (total > 0) (completed * 100) / total else 0
        
        return isCorrect
    }

    fun nextWord() {
        val currentIndex = _currentWordIndex.value ?: 0
        val wordsList = _words.value ?: emptyList()
        
        if (currentIndex < wordsList.size - 1) {
            _currentWordIndex.value = currentIndex + 1
            _currentWord.value = wordsList[currentIndex + 1]
            clearSelectedSyllables()
        } else {
            // All words completed
            _isCompleted.value = true
        }
    }

    fun restartQuiz() {
        _score.value = 0
        _progress.value = 0
        _isCompleted.value = false
        _correctAnswers.value = 0
        _wrongAnswers.value = 0
        _currentWordIndex.value = 0
        attemptedWords.clear()
        selectedSyllables.clear()
        _completedWords.value = 0
        
        val wordsList = _words.value ?: emptyList()
        if (wordsList.isNotEmpty()) {
            _currentWord.value = wordsList[0]
        }
    }

    private fun updateProgress(wordId: String, isCorrect: Boolean) {
        val currentUser = authRepository.getCurrentUser() ?: return
        val studentId = currentUser.studentId ?: currentUser.id
        
        viewModelScope.launch {
            try {
                val request = UpdateProgressRequest(
                    studentId = studentId,
                    moduleId = MODULE_ID,
                    questionId = wordId,
                    isCorrect = isCorrect,
                    score = _score.value,
                    totalQuestions = _totalWords.value,
                    currentQuestion = _currentWordIndex.value,
                    correctAnswers = _correctAnswers.value,
                    wrongAnswers = _wrongAnswers.value
                )
                repository.updateProgress(studentId, MODULE_ID, request)
            } catch (e: Exception) {
                android.util.Log.e("WordBuilderViewModel", "Failed to update progress: ${e.message}")
            }
        }
    }

    fun getStatistics(): Map<String, Any> {
        val correct = _correctAnswers.value ?: 0
        val wrong = _wrongAnswers.value ?: 0
        val totalAttempted = correct + wrong
        val total = _totalWords.value ?: 0
        val percentage = if (total > 0) (correct * 100) / total else 0
        
        return mapOf(
            "correct" to correct,
            "wrong" to wrong,
            "total" to total,
            "totalAttempted" to totalAttempted,
            "percentage" to percentage,
            "score" to (_score.value ?: 0)
        )
    }
}

