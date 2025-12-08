package com.example.playbright.data.model

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val role: String = "student", // student, teacher, admin
    val studentId: String? = null // Student document ID from Firestore (different from Firebase Auth UID)
)

