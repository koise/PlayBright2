package com.example.playbright.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playbright.data.model.AudioIdentificationResponse
import com.example.playbright.data.model.UpdateProgressRequest
import com.example.playbright.data.network.ApiErrorHandler
import com.example.playbright.data.repository.ApiRepository
import com.example.playbright.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AudioIdentificationViewModel : ViewModel() {

    private val repository = ApiRepository()
    private val authRepository = AuthRepository()
    private var MODULE_ID = "audio-identification"
    
    fun setModuleId(moduleId: String) {
        MODULE_ID = moduleId
    }
    
    fun getModuleId(): String = MODULE_ID

    private val _questions = MutableLiveData<List<AudioIdentificationResponse>>()
    val questions: LiveData<List<AudioIdentificationResponse>> = _questions

    private val _currentQuestionIndex = MutableLiveData<Int>(0)
    val currentQuestionIndex: LiveData<Int> = _currentQuestionIndex

    private val _currentQuestion = MutableLiveData<AudioIdentificationResponse?>()
    val currentQuestion: LiveData<AudioIdentificationResponse?> = _currentQuestion

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

    private val _starsEarned = MutableLiveData<Int>(0)
    val starsEarned: LiveData<Int> = _starsEarned

    private val _questionStars = MutableLiveData<Map<Int, Boolean>>(emptyMap())
    val questionStars: LiveData<Map<Int, Boolean>> = _questionStars

    private val _correctAnswers = MutableLiveData<Int>(0)
    val correctAnswers: LiveData<Int> = _correctAnswers

    private val _wrongAnswers = MutableLiveData<Int>(0)
    val wrongAnswers: LiveData<Int> = _wrongAnswers

    private val _originalTotalQuestions = MutableLiveData<Int>(0)
    val originalTotalQuestions: LiveData<Int> = _originalTotalQuestions

    private val questionStarsMap = mutableMapOf<Int, Boolean>()
    private val answerResults = mutableMapOf<String, Boolean>()

    fun loadQuestions() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response = repository.getAudioIdentifications(
                    moduleId = MODULE_ID,
                    status = "Active"
                )

                if (response.isSuccessful) {
                    val questionsList = response.body() ?: emptyList()
                    
                    // Shuffle questions for random order
                    val shuffledQuestions = questionsList.shuffled()
                    _questions.value = shuffledQuestions

                    if (shuffledQuestions.isNotEmpty()) {
                        _currentQuestionIndex.value = 0
                        _currentQuestion.value = shuffledQuestions[0]
                        _originalTotalQuestions.value = shuffledQuestions.size // Store original total
                        updateProgress()
                        loadProgress()
                    } else {
                        _error.value = "No questions available for this module. Please add questions in the admin panel."
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    _error.value = "Failed to load questions: $errorBody"
                }
            } catch (e: Exception) {
                val errorMessage = ApiErrorHandler.handleError(e)
                _error.value = errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadProgress() {
        val currentUser = authRepository.getCurrentUser() ?: return
        val studentId = currentUser.studentId ?: currentUser.id

        viewModelScope.launch {
            try {
                val response = repository.getProgress(studentId, MODULE_ID)
                if (response.isSuccessful && response.body()?.status == "success") {
                    val progressData = response.body()?.data
                    progressData?.let {
                        _score.value = it.score
                        _starsEarned.value = it.starsEarned
                        _currentQuestionIndex.value = it.currentQuestion

                        val starsMap = mutableMapOf<Int, Boolean>()
                        it.questionsAnswered?.forEachIndexed { index, answer ->
                            if (answer.isCorrect) {
                                starsMap[index] = true
                            }
                        }
                        questionStarsMap.putAll(starsMap)
                        _questionStars.value = questionStarsMap.toMap()

                        // Update current question and restore totals
                        val questionsList = _questions.value ?: emptyList()
                        if (it.currentQuestion < questionsList.size) {
                            _currentQuestion.value = questionsList[it.currentQuestion]
                            
                            // Restore original total (from questions list size)
                            _originalTotalQuestions.value = questionsList.size
                            
                            // Calculate wrong answers from questionsAnswered array
                            val wrongCount = (it.questionsAnswered?.count { answer -> !answer.isCorrect } ?: 0)
                            val correctCount = (it.questionsAnswered?.count { answer -> answer.isCorrect } ?: 0)
                            
                            // Restore correct/wrong counts
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

    fun checkAnswer(selectedIndex: Int): Boolean {
        val question = _currentQuestion.value ?: return false
        
        val isCorrect = if (question.images != null && question.images.isNotEmpty()) {
            if (selectedIndex < question.images.size) {
                question.images[selectedIndex].isCorrect
            } else {
                false
            }
        } else {
            selectedIndex == (question.correctAnswer ?: -1)
        }

        answerResults[question.id] = isCorrect
        
        if (isCorrect) {
            val newScore = (_score.value ?: 0) + 1
            _score.value = newScore
            _correctAnswers.value = (_correctAnswers.value ?: 0) + 1
            
            val currentIndex = _currentQuestionIndex.value ?: 0
            if (!questionStarsMap.containsKey(currentIndex) || questionStarsMap[currentIndex] != true) {
                questionStarsMap[currentIndex] = true
                _questionStars.value = questionStarsMap.toMap()
                _starsEarned.value = (_starsEarned.value ?: 0) + 1
            }
        } else {
            _wrongAnswers.value = (_wrongAnswers.value ?: 0) + 1
        }

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
                // Always send progress update, including wrong answers
                // This ensures all attempts are tracked in statistics
                val request = UpdateProgressRequest(
                    studentId = studentId,
                    moduleId = MODULE_ID,
                    questionId = questionId,
                    isCorrect = isCorrect, // Include both correct and wrong answers
                    score = _score.value ?: 0,
                    totalQuestions = questionsList.size,
                    currentQuestion = currentIndex + 1,
                    correctAnswers = _correctAnswers.value,
                    wrongAnswers = _wrongAnswers.value
                )

                repository.updateProgress(request)
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
            // Module completed - mark as completed and save to Firebase
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
                // Send final progress update with completion status and all statistics
                val request = UpdateProgressRequest(
                    studentId = studentId,
                    moduleId = MODULE_ID,
                    questionId = null, // No specific question for completion
                    isCorrect = null,
                    score = finalScore,
                    totalQuestions = questionsList.size,
                    currentQuestion = questionsList.size, // Set to total to mark as completed
                    correctAnswers = finalCorrect,
                    wrongAnswers = finalWrong
                )

                android.util.Log.d("AudioIdentification", "Saving final statistics: score=$finalScore, correct=$finalCorrect, wrong=$finalWrong, total=${questionsList.size}")
                repository.updateProgress(request)
                android.util.Log.d("AudioIdentification", "Final statistics saved successfully")
            } catch (e: Exception) {
                android.util.Log.e("AudioIdentification", "Error marking module as completed: ${e.message}", e)
            }
        }
    }

    private fun updateProgress() {
        val correct = _correctAnswers.value ?: 0
        val wrong = _wrongAnswers.value ?: 0
        val totalAnswered = correct + wrong
        val originalTotal = _originalTotalQuestions.value ?: 1
        
        // Calculate progress based on questions answered out of original total
        // Cap at 100% to prevent showing more than 100%
        val progressValue = if (originalTotal > 0) {
            ((totalAnswered * 100) / originalTotal).coerceAtMost(100)
        } else {
            100
        }
        _progress.value = progressValue
    }

    fun restartQuiz() {
        // Shuffle questions again for new attempt
        val currentQuestions = _questions.value ?: emptyList()
        val reshuffledQuestions = currentQuestions.shuffled()
        _questions.value = reshuffledQuestions
        
        _currentQuestionIndex.value = 0
        _score.value = 0
        _progress.value = 0
        _isCompleted.value = false
        _starsEarned.value = 0
        _correctAnswers.value = 0
        _wrongAnswers.value = 0
        questionStarsMap.clear()
        _questionStars.value = emptyMap()
        answerResults.clear()

        if (reshuffledQuestions.isNotEmpty()) {
            _originalTotalQuestions.value = reshuffledQuestions.size // Reset original total
            _currentQuestion.value = reshuffledQuestions[0]
            updateProgress()
        }
    }
    
    fun getStatistics(): Map<String, Any> {
        val correct = _correctAnswers.value ?: 0
        val wrong = _wrongAnswers.value ?: 0
        val totalAttempted = correct + wrong // Total questions attempted (including wrong answers)
        val originalTotal = _originalTotalQuestions.value ?: 0
        
        // Calculate accuracy based on total attempts (correct + wrong)
        // Accuracy = correct answers / total attempts
        val percentage = if (totalAttempted > 0) {
            (correct * 100) / totalAttempted
        } else {
            0
        }
        
        return mapOf(
            "correct" to correct,
            "wrong" to wrong, // Wrong answers are now properly included
            "total" to originalTotal, // Original total questions
            "totalAttempted" to totalAttempted, // Total questions attempted
            "percentage" to percentage,
            "score" to (_score.value ?: 0),
            "starsEarned" to (_starsEarned.value ?: 0)
        )
    }
}

