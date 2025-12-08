package com.example.playbright.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playbright.data.model.ModuleResponse
import com.example.playbright.data.model.ModuleStatsResponse
import com.example.playbright.data.network.ApiErrorHandler
import com.example.playbright.data.repository.ApiRepository
import kotlinx.coroutines.launch

class ModulesViewModel : ViewModel() {
    
    private val repository = ApiRepository()
    
    private val _modules = MutableLiveData<List<ModuleResponse>>()
    val modules: LiveData<List<ModuleResponse>> = _modules
    
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
    }
}

