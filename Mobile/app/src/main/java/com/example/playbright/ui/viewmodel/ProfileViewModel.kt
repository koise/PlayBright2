package com.example.playbright.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playbright.data.model.ParentData
import com.example.playbright.data.model.StudentResponse
import com.example.playbright.data.model.UpdateStudentRequest
import com.example.playbright.data.network.ApiErrorHandler
import com.example.playbright.data.repository.ApiRepository
import com.example.playbright.data.repository.AuthRepository
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    
    private val repository = ApiRepository()
    private val authRepository = AuthRepository()
    
    private val _student = MutableLiveData<StudentResponse?>()
    val student: LiveData<StudentResponse?> = _student
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess
    
    fun loadStudentProfile() {
        val currentUser = authRepository.getCurrentUser() ?: return
        // Use studentId if available, otherwise fetch all active students and find by email
        val studentId = currentUser.studentId
        
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                if (studentId != null && studentId.isNotEmpty()) {
                    // Fetch by student document ID
                    val response = repository.getStudentById(studentId)
                    if (response.isSuccessful) {
                        _student.value = response.body()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: response.message()
                        _error.value = "Failed to load profile: $errorBody"
                    }
                } else {
                    // Fallback: fetch all active students and find by email
                    val studentsResponse = repository.getStudents(status = "Active")
                    if (studentsResponse.isSuccessful) {
                        val students = studentsResponse.body() ?: emptyList()
                        val student = students.firstOrNull { it.email == currentUser.email }
                        if (student != null) {
                            // Save the student ID for future use
                            authRepository.saveStudentId(student.id)
                            _student.value = student
                        } else {
                            _error.value = "Student not found with email: ${currentUser.email}"
                        }
                    } else {
                        val errorBody = studentsResponse.errorBody()?.string() ?: studentsResponse.message()
                        _error.value = "Failed to load profile: $errorBody"
                    }
                }
            } catch (e: Exception) {
                _error.value = ApiErrorHandler.handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateStudentProfile(
        firstName: String,
        lastName: String,
        birthday: String,
        studentNumber: String,
        photoUrl: String?,
        parentFirstName: String,
        parentLastName: String,
        parentEmail: String,
        parentContact: String
    ) {
        val currentUser = authRepository.getCurrentUser() ?: return
        // Use studentId if available, otherwise get from current student data
        val studentId = currentUser.studentId ?: _student.value?.id
        if (studentId == null) {
            _error.value = "Student ID not found. Please reload profile."
            return
        }
        
        _isLoading.value = true
        _error.value = null
        _updateSuccess.value = false
        
        viewModelScope.launch {
            try {
                val parentData = ParentData(
                    firstName = parentFirstName,
                    lastName = parentLastName,
                    email = parentEmail,
                    contactNumber = parentContact
                )
                
                val updateRequest = UpdateStudentRequest(
                    firstName = firstName,
                    lastName = lastName,
                    birthday = birthday,
                    studentNumber = studentNumber,
                    email = null, // Email cannot be changed
                    photoUrl = photoUrl,
                    parent = parentData
                )
                
                val response = repository.updateStudent(studentId, updateRequest)
                if (response.isSuccessful) {
                    _student.value = response.body()
                    _updateSuccess.value = true
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    _error.value = "Failed to update profile: $errorBody"
                }
            } catch (e: Exception) {
                _error.value = ApiErrorHandler.handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}


