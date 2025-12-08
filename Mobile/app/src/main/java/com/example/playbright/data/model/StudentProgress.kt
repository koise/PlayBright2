package com.example.playbright.data.model

data class StudentProgress(
    val studentId: String,
    val totalStars: Int = 0,
    val currentLevel: Int = 1,
    val modulesCompleted: List<String> = emptyList(),
    val achievements: List<Achievement> = emptyList()
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val unlockedAt: String?
)

