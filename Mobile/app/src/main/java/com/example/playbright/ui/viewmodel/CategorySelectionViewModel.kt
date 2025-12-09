package com.example.playbright.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playbright.data.model.CategorySelectionResponse
import com.example.playbright.data.model.ProgressData
import com.example.playbright.data.model.UpdateProgressRequest
import com.example.playbright.data.network.ApiErrorHandler
import com.example.playbright.data.repository.ApiRepository
import com.example.playbright.data.repository.AuthRepository
import kotlinx.coroutines.launch

class CategorySelectionViewModel : ViewModel() {
    
    private val repository = ApiRepository()
    private val authRepository = AuthRepository()
    
    companion object {
        const val MODULE_ID = "category-selection"
    }
    
    private var moduleId = MODULE_ID
    
    private val _questions = MutableLiveData<List<CategorySelectionResponse>>()
    val questions: LiveData<List<CategorySelectionResponse>> = _questions
    
    private val _currentQuestion = MutableLiveData<CategorySelectionResponse?>()
    val currentQuestion: LiveData<CategorySelectionResponse?> = _currentQuestion
    
    private val _currentQuestionIndex = MutableLiveData<Int>(0)
    val currentQuestionIndex: LiveData<Int> = _currentQuestionIndex
    
    private val _originalTotalQuestions = MutableLiveData<Int>(0)
    val originalTotalQuestions: LiveData<Int> = _originalTotalQuestions
    
    private val _score = MutableLiveData<Int>(0)
    val score: LiveData<Int> = _score
    
    private val _correctAnswers = MutableLiveData<Int>(0)
    val correctAnswers: LiveData<Int> = _correctAnswers
    
    private val _wrongAnswers = MutableLiveData<Int>(0)
    val wrongAnswers: LiveData<Int> = _wrongAnswers
    
    private val _isCompleted = MutableLiveData<Boolean>(false)
    val isCompleted: LiveData<Boolean> = _isCompleted
    
    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error
    
    private val selectedIndices = mutableSetOf<Int>()
    fun setModuleId(id: String) {
        moduleId = id
    }
    
    fun loadQuestions() {
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                android.util.Log.d("CategorySelectionViewModel", "Loading questions for module: $moduleId")
                val response = repository.getCategorySelections(moduleId, "Active")
                
                android.util.Log.d("CategorySelectionViewModel", "Response status: ${response.isSuccessful}, Code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val questionsList = response.body() ?: emptyList()
                    android.util.Log.d("CategorySelectionViewModel", "Loaded ${questionsList.size} questions from backend")
                    
                    if (questionsList.isEmpty()) {
                        android.util.Log.w("CategorySelectionViewModel", "⚠️ No questions found for module: $moduleId")
                        _error.value = "No questions available for this module. Please add questions in the admin panel."
                    } else {
                        // Filter out questions with null or empty images
                        val validQuestions = questionsList.filter { 
                            it.images != null && it.images.isNotEmpty() 
                        }
                        
                        if (validQuestions.isEmpty()) {
                            android.util.Log.w("CategorySelectionViewModel", "⚠️ No valid questions (all have null/empty images)")
                            _error.value = "No valid questions available. All questions are missing images."
                        } else {
                            // Shuffle questions for variety
                            val shuffledQuestions = validQuestions.shuffled()
                            _questions.value = shuffledQuestions
                            _originalTotalQuestions.value = shuffledQuestions.size
                            _currentQuestionIndex.value = 0
                            _currentQuestion.value = shuffledQuestions[0]
                            selectedIndices.clear()
                            android.util.Log.d("CategorySelectionViewModel", "✅ Questions loaded successfully (${validQuestions.size} valid out of ${questionsList.size} total)")
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    android.util.Log.e("CategorySelectionViewModel", "❌ Failed to load questions: ${response.code()} - $errorBody")
                    _error.value = "Failed to load questions: $errorBody"
                }
            } catch (e: Exception) {
                android.util.Log.e("CategorySelectionViewModel", "❌ Exception loading questions: ${e.message}", e)
                _error.value = ApiErrorHandler.handleError(e)
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun toggleSelection(index: Int) {
        if (selectedIndices.contains(index)) {
            selectedIndices.remove(index)
        } else {
            selectedIndices.add(index)
        }
    }
    
    fun isSelected(index: Int): Boolean {
        return selectedIndices.contains(index)
    }
    
    fun checkAnswer(): Boolean {
        val current = _currentQuestion.value ?: return false
        
        // Get correct indices from images array (backend structure)
        val images = current.images ?: emptyList()
        val correctIndices = images.mapIndexedNotNull { index, image ->
            if (image.isCorrect) index else null
        }.toSet()
        
        val selectedSet = selectedIndices.toSet()
        
        android.util.Log.d("CategorySelectionViewModel", "Checking answer - Selected: $selectedSet, Correct: $correctIndices")
        
        // Answer is correct if selected set exactly matches correct set
        val isCorrect = selectedSet == correctIndices && selectedSet.isNotEmpty()
        
        if (isCorrect) {
            _score.value = (_score.value ?: 0) + 1
            _correctAnswers.value = (_correctAnswers.value ?: 0) + 1
            android.util.Log.d("CategorySelectionViewModel", "✅ Answer is correct!")
        } else {
            _wrongAnswers.value = (_wrongAnswers.value ?: 0) + 1
            android.util.Log.d("CategorySelectionViewModel", "❌ Answer is wrong")
        }
        
        // Update progress
        updateProgress(isCorrect)
        
        return isCorrect
    }
    
    fun nextQuestion() {
        val currentIndex = _currentQuestionIndex.value ?: 0
        val questionsList = _questions.value ?: emptyList()
        
        if (currentIndex < questionsList.size - 1) {
            _currentQuestionIndex.value = currentIndex + 1
            _currentQuestion.value = questionsList[currentIndex + 1]
            selectedIndices.clear()
        } else {
            // Module completed
            _isCompleted.value = true
            markModuleAsCompleted()
        }
    }
    
    fun restartQuiz() {
        val questionsList = _questions.value ?: emptyList()
        if (questionsList.isNotEmpty()) {
            _questions.value = questionsList.shuffled()
            _currentQuestionIndex.value = 0
            _currentQuestion.value = questionsList[0]
            selectedIndices.clear()
            _score.value = 0
            _correctAnswers.value = 0
            _wrongAnswers.value = 0
            _isCompleted.value = false
        }
    }
    
    fun getStatistics(): Map<String, Any> {
        val correct = _correctAnswers.value ?: 0
        val wrong = _wrongAnswers.value ?: 0
        val totalAttempted = correct + wrong
        val percentage = if (totalAttempted > 0) {
            ((correct.toFloat() / totalAttempted.toFloat()) * 100).toInt()
        } else 0
        
        return mapOf(
            "correct" to correct,
            "wrong" to wrong,
            "totalAttempted" to totalAttempted,
            "percentage" to percentage,
            "score" to (_score.value ?: 0)
        )
    }
    
    private fun updateProgress(isCorrect: Boolean) {
        val currentUser = authRepository.getCurrentUser() ?: return
        val studentId = currentUser.studentId ?: currentUser.id
        val currentIndex = _currentQuestionIndex.value ?: 0
        val questionsList = _questions.value ?: emptyList()
        val currentQuestion = _currentQuestion.value
        
        viewModelScope.launch {
            try {
                val request = UpdateProgressRequest(
                    studentId = studentId,
                    moduleId = moduleId,
                    questionId = currentQuestion?.id,
                    isCorrect = isCorrect,
                    score = _score.value ?: 0,
                    totalQuestions = questionsList.size,
                    currentQuestion = currentIndex + 1,
                    correctAnswers = _correctAnswers.value,
                    wrongAnswers = _wrongAnswers.value
                )
                
                repository.updateProgress(studentId, moduleId, request)
            } catch (e: Exception) {
                // Silently fail - progress will be synced later
            }
        }
    }
    
    fun markModuleAsCompleted() {
        val currentUser = authRepository.getCurrentUser() ?: return
        val studentId = currentUser.studentId ?: currentUser.id
        val questionsList = _questions.value ?: emptyList()
        val finalScore = _score.value ?: 0
        val finalCorrect = _correctAnswers.value ?: 0
        val finalWrong = _wrongAnswers.value ?: 0
        
        viewModelScope.launch {
            try {
                val request = UpdateProgressRequest(
                    studentId = studentId,
                    moduleId = moduleId,
                    questionId = null,
                    isCorrect = null,
                    score = finalScore,
                    totalQuestions = questionsList.size,
                    currentQuestion = questionsList.size,
                    correctAnswers = finalCorrect,
                    wrongAnswers = finalWrong
                )
                
                repository.updateProgress(studentId, moduleId, request)
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
}

