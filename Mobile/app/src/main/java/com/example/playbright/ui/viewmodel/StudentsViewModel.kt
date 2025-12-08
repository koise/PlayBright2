package com.example.playbright.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playbright.data.model.StudentResponse
import com.example.playbright.data.repository.ApiRepository
import kotlinx.coroutines.launch

class StudentsViewModel : ViewModel() {
    
    private val repository = ApiRepository()
    
    private val _students = MutableLiveData<List<StudentResponse>>()
    val students: LiveData<List<StudentResponse>> = _students
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadStudents(status: String? = "Active", search: String? = null) {
        _loading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val response = repository.getStudents(status, search)
                if (response.isSuccessful) {
                    _students.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Failed to load students: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun refreshStudents() {
        loadStudents()
    }
}

