package com.example.playbright.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.playbright.data.model.StudentProgress
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentProgressViewModel : ViewModel() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _progress = MutableLiveData<StudentProgress>()
    val progress: LiveData<StudentProgress> = _progress
    
    fun loadProgress() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("student_progress")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val progress = document.toObject(StudentProgress::class.java)
                    _progress.value = progress
                } else {
                    // Create initial progress
                    val initialProgress = StudentProgress(
                        studentId = userId,
                        totalStars = 0,
                        currentLevel = 1
                    )
                    saveProgress(initialProgress)
                    _progress.value = initialProgress
                }
            }
    }
    
    fun addStars(stars: Int) {
        val current = _progress.value ?: StudentProgress(
            studentId = auth.currentUser?.uid ?: ""
        )
        
        val newTotal = current.totalStars + stars
        val newLevel = (newTotal / 10) + 1 // Level up every 10 stars
        
        val updated = current.copy(
            totalStars = newTotal,
            currentLevel = newLevel
        )
        
        saveProgress(updated)
        _progress.value = updated
    }
    
    fun completeModule(moduleId: String) {
        val current = _progress.value ?: StudentProgress(
            studentId = auth.currentUser?.uid ?: ""
        )
        
        val updatedModules = if (!current.modulesCompleted.contains(moduleId)) {
            current.modulesCompleted + moduleId
        } else {
            current.modulesCompleted
        }
        
        val updated = current.copy(modulesCompleted = updatedModules)
        saveProgress(updated)
        _progress.value = updated
    }
    
    private fun saveProgress(progress: StudentProgress) {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("student_progress")
            .document(userId)
            .set(progress)
            .addOnFailureListener {
                // Handle error
            }
    }
}

