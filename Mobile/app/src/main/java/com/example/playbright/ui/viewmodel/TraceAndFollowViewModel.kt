package com.example.playbright.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playbright.data.model.TraceAndFollowResponse
import com.example.playbright.data.model.UpdateProgressRequest
import com.example.playbright.data.repository.ApiRepository
import com.example.playbright.data.repository.AuthRepository
import kotlinx.coroutines.launch

class TraceAndFollowViewModel : ViewModel() {

    private val repository = ApiRepository()
    private val authRepository = AuthRepository()
    private var MODULE_ID = "trace-follow" // Default module ID
    
    fun setModuleId(moduleId: String) {
        MODULE_ID = moduleId
    }
    
    fun getModuleId(): String = MODULE_ID

    private val _traces = MutableLiveData<List<TraceAndFollowResponse>>()
    val traces: LiveData<List<TraceAndFollowResponse>> = _traces

    private val _currentTraceIndex = MutableLiveData<Int>(0)
    val currentTraceIndex: LiveData<Int> = _currentTraceIndex

    private val _currentTrace = MutableLiveData<TraceAndFollowResponse?>()
    val currentTrace: LiveData<TraceAndFollowResponse?> = _currentTrace

    private val _progress = MutableLiveData<Int>(0)
    val progress: LiveData<Int> = _progress

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isCompleted = MutableLiveData<Boolean>(false)
    val isCompleted: LiveData<Boolean> = _isCompleted

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _totalTraces = MutableLiveData<Int>(0)
    val totalTraces: LiveData<Int> = _totalTraces

    private val _completedTraces = MutableLiveData<Int>(0)
    val completedTraces: LiveData<Int> = _completedTraces

    private val _score = MutableLiveData<Int>(0)
    val score: LiveData<Int> = _score

    private val _totalPoints = MutableLiveData<Int>(0)
    val totalPoints: LiveData<Int> = _totalPoints

    private val _averageSimilarity = MutableLiveData<Int>(0)
    val averageSimilarity: LiveData<Int> = _averageSimilarity

    // Track which traces have been completed
    private val completedTraceIds = mutableSetOf<String>()
    // Track similarity scores for each trace
    private val traceSimilarities = mutableMapOf<String, Int>()

    fun loadTraces(traceType: String? = null) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                android.util.Log.d("TraceAndFollowViewModel", "Loading traces for module: $MODULE_ID")
                val response = repository.getTraceAndFollows(
                    moduleId = MODULE_ID,
                    status = "Active",
                    traceType = traceType
                )

                android.util.Log.d("TraceAndFollowViewModel", "Response status: ${response.isSuccessful}, Code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val tracesList = response.body() ?: emptyList()
                    android.util.Log.d("TraceAndFollowViewModel", "Loaded ${tracesList.size} traces from backend")
                    
                    if (tracesList.isEmpty()) {
                        android.util.Log.w("TraceAndFollowViewModel", "⚠️ No traces found for module: $MODULE_ID")
                        _error.value = "No tracing activities available for this module. Please add activities in the admin panel."
                        _isLoading.value = false
                        return@launch
                    }

                    // Shuffle and take random traces (e.g., 4-6 traces)
                    val shuffledTraces = tracesList.shuffled().take(minOf(6, tracesList.size))
                    _traces.value = shuffledTraces
                    _totalTraces.value = shuffledTraces.size
                    _totalPoints.value = shuffledTraces.size // Total possible points = number of traces
                    _currentTraceIndex.value = 0
                    _currentTrace.value = shuffledTraces.firstOrNull()
                    _progress.value = 0
                    _completedTraces.value = 0
                    _score.value = 0 // Reset score when loading new traces
                    completedTraceIds.clear()
                    android.util.Log.d("TraceAndFollowViewModel", "✅ Traces loaded successfully")
                    _isLoading.value = false
                } else {
                    val errorMessage = response.message() ?: "Failed to load tracing activities"
                    android.util.Log.e("TraceAndFollowViewModel", "❌ Error loading traces: $errorMessage")
                    _error.value = errorMessage
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceAndFollowViewModel", "❌ Exception loading traces: ${e.message}", e)
                _error.value = "Failed to load tracing activities: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun markTraceAsCompleted(similarity: Int = 0) {
        val currentTrace = _currentTrace.value
        if (currentTrace != null && !completedTraceIds.contains(currentTrace.id)) {
            completedTraceIds.add(currentTrace.id)
            _completedTraces.value = completedTraceIds.size
            
            // Store similarity score
            traceSimilarities[currentTrace.id] = similarity
            
            // Calculate points: 1 point if similarity >= 80%, 0 points otherwise
            val points = if (similarity >= 80) 1 else 0
            val isCorrect = similarity >= 80
            val newScore = (_score.value ?: 0) + points
            _score.value = newScore
            // Total possible points is already set when traces are loaded (number of traces)
            // No need to update it here as it's constant
            
            // Calculate average similarity
            val avgSimilarity = traceSimilarities.values.average().toInt()
            _averageSimilarity.value = avgSimilarity
            
            // Track this individual attempt in backend
            updateProgressInBackend(currentTrace.id, isCorrect)
            
            val total = _totalTraces.value ?: 1
            _progress.value = (completedTraceIds.size * 100) / total

            // Check if all traces are completed
            if (completedTraceIds.size >= total) {
                _isCompleted.value = true
                markModuleAsCompleted()
            }
        }
    }

    private fun updateProgressInBackend(traceId: String, isCorrect: Boolean) {
        viewModelScope.launch {
            try {
                val studentId = authRepository.getCurrentUser()?.studentId 
                    ?: authRepository.getCurrentUser()?.id
                
                if (studentId != null) {
                    val correctAnswers = traceSimilarities.values.count { it >= 80 }
                    val wrongAnswers = completedTraceIds.size - correctAnswers
                    
                    val request = UpdateProgressRequest(
                        studentId = studentId,
                        moduleId = MODULE_ID,
                        questionId = traceId,
                        isCorrect = isCorrect,
                        score = _score.value,
                        totalQuestions = _totalTraces.value,
                        currentQuestion = _completedTraces.value,
                        correctAnswers = correctAnswers,
                        wrongAnswers = wrongAnswers
                    )
                    repository.updateProgress(studentId, MODULE_ID, request)
                    android.util.Log.d("TraceAndFollowViewModel", "✅ Progress updated: traceId=$traceId, isCorrect=$isCorrect, similarity=${traceSimilarities[traceId]}")
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceAndFollowViewModel", "❌ Failed to update progress: ${e.message}")
            }
        }
    }

    fun nextTrace() {
        val currentIndex = _currentTraceIndex.value ?: 0
        val tracesList = _traces.value ?: emptyList()
        
        if (currentIndex < tracesList.size - 1) {
            _currentTraceIndex.value = currentIndex + 1
            _currentTrace.value = tracesList[currentIndex + 1]
        } else {
            // All traces completed
            _isCompleted.value = true
            markModuleAsCompleted()
        }
    }

    fun restartGame() {
        _currentTraceIndex.value = 0
        _currentTrace.value = _traces.value?.firstOrNull()
        _progress.value = 0
        _completedTraces.value = 0
        _isCompleted.value = false
        _score.value = 0
        _totalPoints.value = 0
        _averageSimilarity.value = 0
        completedTraceIds.clear()
        traceSimilarities.clear()
    }

    fun getProgress(): Int {
        return _progress.value ?: 0
    }

    private fun markModuleAsCompleted() {
        viewModelScope.launch {
            try {
                val studentId = authRepository.getCurrentUser()?.studentId 
                    ?: authRepository.getCurrentUser()?.id
                
                if (studentId != null) {
                    val totalTraces = _totalTraces.value ?: 0
                    val completedTraces = _completedTraces.value ?: 0
                    val finalScore = _score.value ?: 0
                    val avgSimilarity = _averageSimilarity.value ?: 0
                    
                    // Calculate correct answers based on similarity (>=80% = correct, earns 1 point)
                    val correctAnswers = traceSimilarities.values.count { it >= 80 }
                    val wrongAnswers = completedTraces - correctAnswers
                    
                    val request = UpdateProgressRequest(
                        studentId = studentId,
                        moduleId = MODULE_ID,
                        score = finalScore,
                        totalQuestions = totalTraces,
                        currentQuestion = totalTraces,
                        correctAnswers = correctAnswers,
                        wrongAnswers = wrongAnswers
                    )
                    
                    repository.updateProgress(studentId, MODULE_ID, request)
                    android.util.Log.d("TraceAndFollowViewModel", "Progress updated for module: $MODULE_ID")
                }
            } catch (e: Exception) {
                android.util.Log.e("TraceAndFollowViewModel", "Error updating progress: ${e.message}", e)
            }
        }
    }

    fun getStatistics(): Map<String, Any> {
        val totalTraces = _totalTraces.value ?: 0
        val completedTraces = _completedTraces.value ?: 0
        val score = _score.value ?: 0
        val totalPoints = _totalPoints.value ?: 0
        val avgSimilarity = _averageSimilarity.value ?: 0
        val correctAnswers = traceSimilarities.values.count { it >= 80 }
        val wrongAnswers = completedTraces - correctAnswers
        val percentage = if (totalTraces > 0) (correctAnswers * 100) / totalTraces else 0
        
        return mapOf(
            "totalTraces" to totalTraces,
            "completedTraces" to completedTraces,
            "progress" to (_progress.value ?: 0),
            "score" to score,
            "totalPoints" to totalPoints,
            "averageSimilarity" to avgSimilarity,
            "correct" to correctAnswers,
            "wrong" to wrongAnswers,
            "percentage" to percentage
        )
    }
}

