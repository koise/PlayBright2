package com.example.playbright.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playbright.data.model.User
import com.example.playbright.data.network.ApiErrorHandler
import com.example.playbright.data.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    
    private val repository = AuthRepository()
    
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState
    
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser
    
    init {
        checkCurrentUser()
    }
    
    private fun checkCurrentUser() {
        _currentUser.value = repository.getCurrentUser()
    }
    
    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }
        
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            val result = repository.signInWithEmail(email, password)
            
            result.onSuccess { user ->
                _currentUser.value = user
                _authState.value = AuthState.Success(user)
            }.onFailure { exception ->
                val errorMessage = ApiErrorHandler.handleError(exception)
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }
    
    fun signInWithGoogle(account: GoogleSignInAccount) {
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            val result = repository.signInWithGoogle(account)
            
            result.onSuccess { user ->
                _currentUser.value = user
                _authState.value = AuthState.Success(user)
            }.onFailure { exception ->
                val errorMessage = ApiErrorHandler.handleError(exception)
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }
    
    fun signUp(email: String, password: String, displayName: String) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _authState.value = AuthState.Error("All fields are required")
            return
        }
        
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }
        
        _authState.value = AuthState.Loading
        
        viewModelScope.launch {
            val result = repository.signUp(email, password, displayName)
            
            result.onSuccess { user ->
                _currentUser.value = user
                _authState.value = AuthState.Success(user)
            }.onFailure { exception ->
                val errorMessage = ApiErrorHandler.handleError(exception)
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }
    
    fun signOut() {
        repository.signOut()
        _currentUser.value = null
        _authState.value = AuthState.SignedOut
    }
    
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
    
    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val user: User) : AuthState()
        data class Error(val message: String) : AuthState()
        object SignedOut : AuthState()
    }
}

