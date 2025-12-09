package com.example.playbright.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playbright.data.model.UpdateProgressRequest
import com.example.playbright.data.model.YesNoQuestionResponse
import com.example.playbright.data.network.ApiErrorHandler
import com.example.playbright.data.repository.ApiRepository
import com.example.playbright.data.repository.AuthRepository
import kotlinx.coroutines.launch

class YesNoQuestionsViewModel : ViewModel() {

    private val repository = ApiRepository()
    private val authRepository = AuthRepository()
    private var MODULE_ID = "yes-no-questions" // Default module ID
    
    fun setModuleId(moduleId: String) {
        MODULE_ID = moduleId
    }
    
    fun getModuleId(): String = MODULE_ID

    private val _questions = MutableLiveData<List<YesNoQuestionResponse>>()
    val questions: LiveData<List<YesNoQuestionResponse>> = _questions

    private val _currentQuestionIndex = MutableLiveData<Int>(0)
    val currentQuestionIndex: LiveData<Int> = _currentQuestionIndex

    private val _currentQuestion = MutableLiveData<YesNoQuestionResponse?>()
    val currentQuestion: LiveData<YesNoQuestionResponse?> = _currentQuestion

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

    private val _effectiveTotalQuestions = MutableLiveData<Int>(0)
    val effectiveTotalQuestions: LiveData<Int> = _effectiveTotalQuestions

    private val _originalTotalQuestions = MutableLiveData<Int>(0)
    val originalTotalQuestions: LiveData<Int> = _originalTotalQuestions

    private val answerResults = mutableMapOf<String, Boolean>() // questionId -> isCorrect

    fun loadQuestions() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                android.util.Log.d("YesNoQuestionsViewModel", "Loading questions for module: $MODULE_ID")
                val response = repository.getYesNoQuestions(
                    moduleId = MODULE_ID,
                    status = "Active"
                )

                android.util.Log.d("YesNoQuestionsViewModel", "Response status: ${response.isSuccessful}, Code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val questionsList = response.body() ?: emptyList()
                    android.util.Log.d("YesNoQuestionsViewModel", "Loaded ${questionsList.size} questions from backend")
                    
                    // Shuffle questions for random order
                    val shuffledQuestions = questionsList.shuffled()
                    _questions.value = shuffledQuestions

                    if (shuffledQuestions.isNotEmpty()) {
                        _currentQuestionIndex.value = 0
                        _currentQuestion.value = shuffledQuestions[0]
                        _originalTotalQuestions.value = shuffledQuestions.size
                        _effectiveTotalQuestions.value = shuffledQuestions.size
                        updateProgress()
                        loadProgress()
                        android.util.Log.d("YesNoQuestionsViewModel", "✅ Questions loaded successfully")
                    } else {
                        android.util.Log.w("YesNoQuestionsViewModel", "⚠️ No questions found for module: $MODULE_ID")
                        _error.value = "No questions available for this module. Please add questions in the admin panel."
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    android.util.Log.e("YesNoQuestionsViewModel", "❌ Failed to load questions: ${response.code()} - $errorBody")
                    _error.value = "Failed to load questions: $errorBody"
                }
            } catch (e: Exception) {
                android.util.Log.e("YesNoQuestionsViewModel", "❌ Exception loading questions: ${e.message}", e)
                val errorMessage = ApiErrorHandler.handleError(e)
                _error.value = errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadProgress() {
        val currentUser = authRepository.getCurrentUser() ?: return
        val studentId = currentUser.id

        viewModelScope.launch {
            try {
                val response = repository.getProgress(studentId, MODULE_ID)
                if (response.isSuccessful && response.body()?.status == "success") {
                    val progressData = response.body()?.data
                    progressData?.let {
                        _score.value = it.score
                        _currentQuestionIndex.value = it.currentQuestion

                        // Update current question and restore totals
                        val questionsList = _questions.value ?: emptyList()
                        if (it.currentQuestion < questionsList.size) {
                            _currentQuestion.value = questionsList[it.currentQuestion]
                            
                            _originalTotalQuestions.value = questionsList.size
                            
                            val wrongCount = (it.questionsAnswered?.count { answer -> !answer.isCorrect } ?: 0)
                            val correctCount = (it.questionsAnswered?.count { answer -> answer.isCorrect } ?: 0)
                            
                            _effectiveTotalQuestions.value = (questionsList.size - wrongCount).coerceAtLeast(0)
                            
                            _correctAnswers.value = correctCount
                            _wrongAnswers.value = wrongCount
                            
                            updateProgress()
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently fail - progress will be created on first answer
            }
        }
    }

    fun checkAnswer(selectedAnswer: Boolean): Boolean {
        val question = _currentQuestion.value ?: return false
        
        // Check if selected answer matches correct answer
        val isCorrect = selectedAnswer == question.correctAnswer
        
        // Track answer result
        answerResults[question.id] = isCorrect
        
        if (isCorrect) {
            val newScore = (_score.value ?: 0) + 1
            _score.value = newScore
            _correctAnswers.value = (_correctAnswers.value ?: 0) + 1
        } else {
            _wrongAnswers.value = (_wrongAnswers.value ?: 0) + 1
            val currentEffective = _effectiveTotalQuestions.value ?: 0
            if (currentEffective > 0) {
                _effectiveTotalQuestions.value = currentEffective - 1
            }
        }

        // Update progress in backend
        updateProgressInBackend(question.id, isCorrect)

        return isCorrect
    }

    private fun updateProgressInBackend(questionId: String, isCorrect: Boolean) {
        val currentUser = authRepository.getCurrentUser() ?: return
        val studentId = currentUser.studentId ?: currentUser.id
        val currentIndex = _currentQuestionIndex.value ?: 0
        val questionsList = _questions.value ?: emptyList()

        viewModelScope.launch {
            try {
                val request = UpdateProgressRequest(
                    studentId = studentId,
                    moduleId = MODULE_ID,
                    questionId = questionId,
                    isCorrect = isCorrect,
                    score = _score.value ?: 0,
                    totalQuestions = questionsList.size,
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

    fun nextQuestion() {
        val currentIndex = _currentQuestionIndex.value ?: 0
        val questionsList = _questions.value ?: emptyList()

        if (currentIndex < questionsList.size - 1) {
            _currentQuestionIndex.value = currentIndex + 1
            _currentQuestion.value = questionsList[currentIndex + 1]
            updateProgress()
        } else {
            _isCompleted.value = true
            markModuleAsCompleted()
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
                    moduleId = MODULE_ID,
                    questionId = null,
                    isCorrect = null,
                    score = finalScore,
                    totalQuestions = questionsList.size,
                    currentQuestion = questionsList.size,
                    correctAnswers = finalCorrect,
                    wrongAnswers = finalWrong
                )

                android.util.Log.d("YesNoQuestions", "Saving final statistics: score=$finalScore, correct=$finalCorrect, wrong=$finalWrong, total=${questionsList.size}")
                repository.updateProgress(studentId, MODULE_ID, request)
                android.util.Log.d("YesNoQuestions", "Final statistics saved successfully")
            } catch (e: Exception) {
                android.util.Log.e("YesNoQuestions", "Error marking module as completed: ${e.message}", e)
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
        val currentQuestions = _questions.value ?: emptyList()
        val reshuffledQuestions = currentQuestions.shuffled()
        _questions.value = reshuffledQuestions
        
        _currentQuestionIndex.value = 0
        _score.value = 0
        _progress.value = 0
        _isCompleted.value = false
        _correctAnswers.value = 0
        _wrongAnswers.value = 0
        answerResults.clear()

        if (reshuffledQuestions.isNotEmpty()) {
            _originalTotalQuestions.value = reshuffledQuestions.size
            _effectiveTotalQuestions.value = reshuffledQuestions.size
            _currentQuestion.value = reshuffledQuestions[0]
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
}

