package com.example.playbright.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playbright.data.model.PictureLabelResponse
import com.example.playbright.data.network.ApiErrorHandler
import com.example.playbright.data.repository.ApiRepository
import kotlinx.coroutines.launch

class PictureLabelingViewModel : ViewModel() {

    private val repository = ApiRepository()
    private var MODULE_ID = "picture-label"
    
    fun setModuleId(moduleId: String) {
        MODULE_ID = moduleId
    }
    
    fun getModuleId(): String = MODULE_ID

    private val _labels = MutableLiveData<List<PictureLabelResponse>>()
    val labels: LiveData<List<PictureLabelResponse>> = _labels

    private val _currentLabelIndex = MutableLiveData<Int>(0)
    val currentLabelIndex: LiveData<Int> = _currentLabelIndex

    private val _currentLabel = MutableLiveData<PictureLabelResponse?>()
    val currentLabel: LiveData<PictureLabelResponse?> = _currentLabel

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _speechRecognitionState = MutableLiveData<String>("idle")
    val speechRecognitionState: LiveData<String> = _speechRecognitionState

    private val _score = MutableLiveData<Int>(0)
    val score: LiveData<Int> = _score

    private val _correctAttempts = MutableLiveData<Int>(0)
    val correctAttempts: LiveData<Int> = _correctAttempts

    private val _totalAttempts = MutableLiveData<Int>(0)
    val totalAttempts: LiveData<Int> = _totalAttempts

    fun setSpeechRecognitionState(state: String) {
        _speechRecognitionState.value = state
    }

    fun loadLabels() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                android.util.Log.d("PictureLabelingViewModel", "Loading labels for module: $MODULE_ID")
                val response = repository.getPictureLabels(
                    moduleId = MODULE_ID,
                    status = "Active"
                )

                android.util.Log.d("PictureLabelingViewModel", "Response status: ${response.isSuccessful}, Code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val labelsList = response.body() ?: emptyList()
                    android.util.Log.d("PictureLabelingViewModel", "Loaded ${labelsList.size} labels from backend")
                    
                    // Shuffle labels for variety
                    val shuffledLabels = labelsList.shuffled()
                    _labels.value = shuffledLabels

                    if (shuffledLabels.isNotEmpty()) {
                        _currentLabelIndex.value = 0
                        _currentLabel.value = shuffledLabels[0]
                        android.util.Log.d("PictureLabelingViewModel", "✅ Labels loaded successfully")
                    } else {
                        android.util.Log.w("PictureLabelingViewModel", "⚠️ No labels found for module: $MODULE_ID")
                        _error.value = "No labels available for this module. Please add labels in the admin panel."
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    android.util.Log.e("PictureLabelingViewModel", "❌ Failed to load labels: ${response.code()} - $errorBody")
                    _error.value = "Failed to load labels: $errorBody"
                }
            } catch (e: Exception) {
                android.util.Log.e("PictureLabelingViewModel", "❌ Exception loading labels: ${e.message}", e)
                val errorMessage = ApiErrorHandler.handleError(e)
                _error.value = errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun nextLabel() {
        val currentIndex = _currentLabelIndex.value ?: 0
        val labelsList = _labels.value ?: emptyList()

        if (currentIndex < labelsList.size - 1) {
            _currentLabelIndex.value = currentIndex + 1
            _currentLabel.value = labelsList[currentIndex + 1]
        } else {
            // Loop back to first label
            _currentLabelIndex.value = 0
            _currentLabel.value = labelsList[0]
        }
    }

    fun previousLabel() {
        val currentIndex = _currentLabelIndex.value ?: 0
        val labelsList = _labels.value ?: emptyList()

        if (currentIndex > 0) {
            _currentLabelIndex.value = currentIndex - 1
            _currentLabel.value = labelsList[currentIndex - 1]
        } else {
            // Loop to last label
            _currentLabelIndex.value = labelsList.size - 1
            _currentLabel.value = labelsList[labelsList.size - 1]
        }
    }

    fun getCurrentWord(): String {
        return _currentLabel.value?.word ?: ""
    }

    fun getTotalLabels(): Int {
        return _labels.value?.size ?: 0
    }

    fun checkSpokenWord(spokenText: String): Boolean {
        val currentWord = getCurrentWord().lowercase().trim()
        val spoken = spokenText.lowercase().trim()
        
        _totalAttempts.value = (_totalAttempts.value ?: 0) + 1
        
        // Check if spoken word matches (exact or contains the word)
        val isCorrect = spoken == currentWord || 
                       spoken.contains(currentWord) || 
                       currentWord.contains(spoken) ||
                       calculateSimilarity(spoken, currentWord) > 0.75
        
        if (isCorrect) {
            _correctAttempts.value = (_correctAttempts.value ?: 0) + 1
            _score.value = (_score.value ?: 0) + 10
        }
        
        return isCorrect
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1
        
        if (longer.isEmpty()) return 1.0
        
        val editDistance = computeLevenshteinDistance(longer, shorter)
        return (longer.length - editDistance).toDouble() / longer.length
    }

    private fun computeLevenshteinDistance(s1: String, s2: String): Int {
        val costs = IntArray(s2.length + 1)
        
        for (i in 0..s1.length) {
            var lastValue = i
            for (j in 0..s2.length) {
                if (i == 0) {
                    costs[j] = j
                } else if (j > 0) {
                    var newValue = costs[j - 1]
                    if (s1[i - 1] != s2[j - 1]) {
                        newValue = minOf(newValue, lastValue, costs[j]) + 1
                    }
                    costs[j - 1] = lastValue
                    lastValue = newValue
                }
            }
            if (i > 0) costs[s2.length] = lastValue
        }
        
        return costs[s2.length]
    }

    fun resetScore() {
        _score.value = 0
        _correctAttempts.value = 0
        _totalAttempts.value = 0
    }

    fun getAccuracyPercentage(): Int {
        val total = _totalAttempts.value ?: 0
        val correct = _correctAttempts.value ?: 0
        return if (total > 0) ((correct.toDouble() / total) * 100).toInt() else 0
    }
}

