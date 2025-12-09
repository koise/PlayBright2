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

    // Track which traces have been completed
    private val completedTraceIds = mutableSetOf<String>()

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
                    _currentTraceIndex.value = 0
                    _currentTrace.value = shuffledTraces.firstOrNull()
                    _progress.value = 0
                    _completedTraces.value = 0
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

    fun markTraceAsCompleted() {
        val currentTrace = _currentTrace.value
        if (currentTrace != null && !completedTraceIds.contains(currentTrace.id)) {
            completedTraceIds.add(currentTrace.id)
            _completedTraces.value = completedTraceIds.size
            
            val total = _totalTraces.value ?: 1
            _progress.value = (completedTraceIds.size * 100) / total

            // Check if all traces are completed
            if (completedTraceIds.size >= total) {
                _isCompleted.value = true
                markModuleAsCompleted()
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
        completedTraceIds.clear()
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
                    
                    val request = UpdateProgressRequest(
                        studentId = studentId,
                        moduleId = MODULE_ID,
                        score = completedTraces,
                        totalQuestions = totalTraces,
                        currentQuestion = totalTraces,
                        correctAnswers = completedTraces,
                        wrongAnswers = 0
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
        return mapOf(
            "totalTraces" to (_totalTraces.value ?: 0),
            "completedTraces" to (_completedTraces.value ?: 0),
            "progress" to (_progress.value ?: 0)
        )
    }
}

