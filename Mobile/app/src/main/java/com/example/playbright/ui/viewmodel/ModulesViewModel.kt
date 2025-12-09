package com.example.playbright.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playbright.data.model.ModuleResponse
import com.example.playbright.data.model.ModuleStatsResponse
import com.example.playbright.data.model.ProgressData
import com.example.playbright.data.network.ApiErrorHandler
import com.example.playbright.data.repository.ApiRepository
import com.example.playbright.data.repository.AuthRepository
import kotlinx.coroutines.launch

class ModulesViewModel : ViewModel() {
    
    private val repository = ApiRepository()
    private val authRepository = AuthRepository()
    
    private val _modules = MutableLiveData<List<ModuleResponse>>()
    val modules: LiveData<List<ModuleResponse>> = _modules
    
    private val _progressMap = MutableLiveData<Map<String, ProgressData>>()
    val progressMap: LiveData<Map<String, ProgressData>> = _progressMap
    
    private val _stats = MutableLiveData<ModuleStatsResponse?>()
    val stats: LiveData<ModuleStatsResponse?> = _stats
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadModules() {
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val response = repository.getModules()
                if (response.isSuccessful) {
                    val modules = response.body() ?: emptyList()
                    // Filter only Active modules and log for debugging
                    val activeModules = modules.filter { it.status == "Active" }
                    android.util.Log.d("ModulesViewModel", "Loaded ${modules.size} total modules, ${activeModules.size} active")
                    activeModules.forEach { module ->
                        android.util.Log.d("ModulesViewModel", "Module: ${module.name}, moduleId: ${module.moduleId}, moduleNumber: ${module.moduleNumber}, status: ${module.status}")
                    }
                    _modules.value = activeModules
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    _error.value = "Failed to load modules: $errorBody"
                }
            } catch (e: Exception) {
                _error.value = ApiErrorHandler.handleError(e)
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun loadModuleStats() {
        viewModelScope.launch {
            try {
                val response = repository.getModuleStats()
                if (response.isSuccessful) {
                    _stats.value = response.body()
                }
            } catch (e: Exception) {
                // Silently fail for stats
            }
        }
    }
    
    fun refreshModules() {
        loadModules()
        loadModuleStats()
        loadProgress()
    }
    
    fun loadProgress() {
        val currentUser = authRepository.getCurrentUser() ?: return
        val studentId = currentUser.studentId ?: currentUser.id
        
        viewModelScope.launch {
            try {
                val response = repository.getAllProgress(studentId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    val responseBody = response.body()
                    val progressList = when {
                        responseBody?.data is List<*> -> (responseBody.data as? List<Map<String, Any>>) ?: emptyList()
                        else -> emptyList()
                    }
                    val progressMap = progressList.associate { progress ->
                        val moduleId = progress["moduleId"] as? String ?: ""
                        val progressData = ProgressData(
                            id = progress["id"] as? String,
                            studentId = progress["studentId"] as? String ?: studentId,
                            moduleId = moduleId,
                            score = (progress["score"] as? Number)?.toInt() ?: 0,
                            totalQuestions = (progress["totalQuestions"] as? Number)?.toInt() ?: 0,
                            currentQuestion = (progress["currentQuestion"] as? Number)?.toInt() ?: 0,
                            completed = progress["completed"] as? Boolean ?: false,
                            questionsAnswered = null,
                            starsEarned = 0
                        )
                        moduleId to progressData
                    }
                    _progressMap.value = progressMap
                    android.util.Log.d("ModulesViewModel", "Loaded progress for ${progressMap.size} modules")
                }
            } catch (e: Exception) {
                android.util.Log.e("ModulesViewModel", "Error loading progress: ${e.message}", e)
                // Silently fail - progress is optional
            }
        }
    }
}

