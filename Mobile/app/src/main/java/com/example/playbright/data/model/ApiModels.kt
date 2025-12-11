package com.example.playbright.data.model

import com.google.gson.annotations.SerializedName

// ==================== COMMON MODELS ====================

data class ApiResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: Any?
)

data class CountResponse(
    @SerializedName("count") val count: Int
)

data class UploadResponse(
    @SerializedName("url") val url: String,
    @SerializedName("fileName") val fileName: String
)

data class UploadMultipleResponse(
    @SerializedName("images") val images: List<UploadResponse>
)

// ==================== AUTH MODELS ====================

data class LoginRequest(
    @SerializedName("idToken") val idToken: String
)

data class LoginResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("role") val role: String,
    @SerializedName("user") val user: UserData?
)

data class FacultyLoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class FacultyLoginResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("role") val role: String,
    @SerializedName("customToken") val customToken: String?,
    @SerializedName("user") val user: UserData?
)

data class StudentLoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class StudentLoginResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String,
    @SerializedName("role") val role: String,
    @SerializedName("customToken") val customToken: String?,
    @SerializedName("user") val user: StudentUserData?
)

data class StudentUserData(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("studentNumber") val studentNumber: String?,
    @SerializedName("photoUrl") val photoUrl: String?
)

data class UserData(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String?
)

// ==================== STUDENT MODELS ====================

data class StudentResponse(
    @SerializedName("id") val id: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("birthday") val birthday: String,
    @SerializedName("studentNumber") val studentNumber: String,
    @SerializedName("email") val email: String,
    @SerializedName("photoUrl") val photoUrl: String?,
    @SerializedName("parent") val parent: ParentData?,
    @SerializedName("status") val status: String,
    @SerializedName("dateCreated") val dateCreated: String?,
    @SerializedName("age") val age: Int?
)

data class ParentData(
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("email") val email: String,
    @SerializedName("contactNumber") val contactNumber: String
)

data class CreateStudentRequest(
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("birthday") val birthday: String,
    @SerializedName("studentNumber") val studentNumber: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String?,
    @SerializedName("photoUrl") val photoUrl: String?,
    @SerializedName("parent") val parent: ParentData?
)

data class UpdateStudentRequest(
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("lastName") val lastName: String?,
    @SerializedName("birthday") val birthday: String?,
    @SerializedName("studentNumber") val studentNumber: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("photoUrl") val photoUrl: String?,
    @SerializedName("parent") val parent: ParentData?
)

// ==================== FACULTY MODELS ====================

data class FacultyResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("photoUrl") val photoUrl: String?,
    @SerializedName("status") val status: String,
    @SerializedName("dateJoined") val dateJoined: String?
)

data class CreateFacultyRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("password") val password: String?,
    @SerializedName("birthday") val birthday: String?,
    @SerializedName("photoUrl") val photoUrl: String?
)

data class UpdateFacultyRequest(
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("photoUrl") val photoUrl: String?
)

// ==================== MODULE MODELS ====================

data class ModuleResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("moduleId") val moduleId: String?,
    @SerializedName("moduleNumber") val moduleNumber: Int?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("status") val status: String?
)

data class ModuleStatsResponse(
    @SerializedName("modules") val modules: Map<String, ModuleStat>
)

data class ModuleStat(
    @SerializedName("students") val students: Int,
    @SerializedName("questions") val questions: Int
)

data class CreateModuleRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("moduleId") val moduleId: String,
    @SerializedName("icon") val icon: String?
)

data class UpdateModuleRequest(
    @SerializedName("name") val name: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("icon") val icon: String?
)

// ==================== QUESTION MODELS ====================

data class QuestionImage(
    @SerializedName("id") val id: String,
    @SerializedName("isCorrect") val isCorrect: Boolean,
    @SerializedName("url") val url: String
)

data class QuestionResponse(
    @SerializedName("id") val id: String,
    @SerializedName("question") val question: String,
    @SerializedName("images") val images: List<QuestionImage>?,
    @SerializedName("moduleId") val moduleId: String,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?,
    // Legacy fields for backward compatibility
    @SerializedName("correctAnswer") val correctAnswer: Int? = null,
    @SerializedName("options") val options: List<String>? = null,
    @SerializedName("imageUrls") val imageUrls: List<String>? = null
)

data class CreateQuestionRequest(
    @SerializedName("question") val question: String,
    @SerializedName("correctAnswer") val correctAnswer: Int,
    @SerializedName("options") val options: List<String>,
    @SerializedName("imageUrls") val imageUrls: List<String>,
    @SerializedName("moduleId") val moduleId: String
)

data class UpdateQuestionRequest(
    @SerializedName("question") val question: String?,
    @SerializedName("correctAnswer") val correctAnswer: Int?,
    @SerializedName("options") val options: List<String>?,
    @SerializedName("imageUrls") val imageUrls: List<String>?
)

// ==================== AUDIO IDENTIFICATION MODELS ====================

data class AudioIdentificationImage(
    @SerializedName("id") val id: String,
    @SerializedName("isCorrect") val isCorrect: Boolean,
    @SerializedName("url") val url: String
)

data class AudioIdentificationResponse(
    @SerializedName("id") val id: String,
    @SerializedName("question") val question: String,
    @SerializedName("audioText") val audioText: String,
    @SerializedName("audioUrl") val audioUrl: String?,
    @SerializedName("images") val images: List<AudioIdentificationImage>?,
    @SerializedName("moduleId") val moduleId: String,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?,
    // Legacy fields for backward compatibility
    @SerializedName("correctAnswer") val correctAnswer: Int? = null,
    @SerializedName("options") val options: List<String>? = null,
    @SerializedName("imageUrls") val imageUrls: List<String>? = null
)

data class CreateAudioIdentificationRequest(
    @SerializedName("audioText") val audioText: String,
    @SerializedName("correctAnswer") val correctAnswer: Int,
    @SerializedName("options") val options: List<String>,
    @SerializedName("imageUrls") val imageUrls: List<String>,
    @SerializedName("moduleId") val moduleId: String
)

data class UpdateAudioIdentificationRequest(
    @SerializedName("audioText") val audioText: String?,
    @SerializedName("correctAnswer") val correctAnswer: Int?,
    @SerializedName("options") val options: List<String>?,
    @SerializedName("imageUrls") val imageUrls: List<String>?
)

// ==================== SEQUENCING CARD MODELS ====================

data class SequencingCardResponse(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("cards") val cards: List<CardItem>,
    @SerializedName("moduleId") val moduleId: String,
    @SerializedName("status") val status: String
)

data class CardItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("order") val order: Int,
    @SerializedName("label") val label: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("url") val url: String? = null // Support both url and imageUrl
) {
    // Helper to get the image URL (supports both url and imageUrl fields)
    fun getImageUrlString(): String {
        return url ?: imageUrl ?: ""
    }
}

data class CreateSequencingCardRequest(
    @SerializedName("title") val title: String,
    @SerializedName("cards") val cards: List<CardItem>,
    @SerializedName("moduleId") val moduleId: String
)

data class UpdateSequencingCardRequest(
    @SerializedName("title") val title: String?,
    @SerializedName("cards") val cards: List<CardItem>?
)

// ==================== PICTURE LABEL MODELS ====================

data class PictureLabelResponse(
    @SerializedName("id") val id: String,
    @SerializedName("word") val word: String,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("audioText") val audioText: String? = null,
    @SerializedName("audioUrl") val audioUrl: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("moduleId") val moduleId: String,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class CreatePictureLabelRequest(
    @SerializedName("word") val word: String,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("moduleId") val moduleId: String
)

data class UpdatePictureLabelRequest(
    @SerializedName("word") val word: String?,
    @SerializedName("imageUrl") val imageUrl: String?
)

// ==================== EMOTION RECOGNITION MODELS ====================

data class EmotionRecognitionResponse(
    @SerializedName("id") val id: String,
    @SerializedName("question") val question: String,
    @SerializedName("audioText") val audioText: String? = null,
    @SerializedName("images") val images: List<EmotionImage>? = null,
    @SerializedName("moduleId") val moduleId: String,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    // Legacy fields for backward compatibility
    @SerializedName("correctAnswer") val correctAnswer: String? = null,
    @SerializedName("options") val options: List<EmotionOption>? = null
)

data class EmotionImage(
    @SerializedName("id") val id: String,
    @SerializedName("url") val url: String,
    @SerializedName("emotion") val emotion: String,
    @SerializedName("isCorrect") val isCorrect: Boolean
)

data class EmotionOption(
    @SerializedName("emotion") val emotion: String,
    @SerializedName("imageUrl") val imageUrl: String
)

data class CreateEmotionRecognitionRequest(
    @SerializedName("question") val question: String,
    @SerializedName("correctAnswer") val correctAnswer: String,
    @SerializedName("options") val options: List<EmotionOption>,
    @SerializedName("moduleId") val moduleId: String
)

data class UpdateEmotionRecognitionRequest(
    @SerializedName("question") val question: String?,
    @SerializedName("correctAnswer") val correctAnswer: String?,
    @SerializedName("options") val options: List<EmotionOption>?
)

// ==================== CATEGORY SELECTION MODELS ====================

data class CategorySelectionResponse(
    @SerializedName("id") val id: String,
    @SerializedName("question") val question: String,
    @SerializedName("audioText") val audioText: String? = null,
    @SerializedName("category") val category: String,
    @SerializedName("images") val images: List<CategoryImage>? = null,
    @SerializedName("moduleId") val moduleId: String,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    // Legacy fields for backward compatibility
    @SerializedName("correctAnswers") val correctAnswers: List<Int>? = null,
    @SerializedName("options") val options: List<CategoryOption>? = null
)

data class CategoryImage(
    @SerializedName("id") val id: String,
    @SerializedName("url") val url: String,
    @SerializedName("isCorrect") val isCorrect: Boolean
)

data class CategoryOption(
    @SerializedName("label") val label: String,
    @SerializedName("imageUrl") val imageUrl: String
)

data class CreateCategorySelectionRequest(
    @SerializedName("question") val question: String,
    @SerializedName("category") val category: String,
    @SerializedName("correctAnswers") val correctAnswers: List<Int>,
    @SerializedName("options") val options: List<CategoryOption>,
    @SerializedName("moduleId") val moduleId: String
)

data class UpdateCategorySelectionRequest(
    @SerializedName("question") val question: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("correctAnswers") val correctAnswers: List<Int>?,
    @SerializedName("options") val options: List<CategoryOption>?
)

// ==================== YES/NO QUESTION MODELS ====================

data class YesNoQuestionResponse(
    @SerializedName("id") val id: String,
    @SerializedName("question") val question: String,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("correctAnswer") val correctAnswer: Boolean,
    @SerializedName("moduleId") val moduleId: String,
    @SerializedName("status") val status: String
)

data class CreateYesNoQuestionRequest(
    @SerializedName("question") val question: String,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("correctAnswer") val correctAnswer: Boolean,
    @SerializedName("moduleId") val moduleId: String
)

data class UpdateYesNoQuestionRequest(
    @SerializedName("question") val question: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("correctAnswer") val correctAnswer: Boolean?
)

// ==================== TAP REPEAT MODELS ====================

data class TapRepeatResponse(
    @SerializedName("id") val id: String,
    @SerializedName("word") val word: String,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("audioUrl") val audioUrl: String?,
    @SerializedName("moduleId") val moduleId: String,
    @SerializedName("status") val status: String
)

data class CreateTapRepeatRequest(
    @SerializedName("word") val word: String,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("audioUrl") val audioUrl: String?,
    @SerializedName("moduleId") val moduleId: String
)

data class UpdateTapRepeatRequest(
    @SerializedName("word") val word: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("audioUrl") val audioUrl: String?
)

// ==================== MATCHING PAIR MODELS ====================

data class MatchingPairResponse(
    @SerializedName("id") val id: String,
    @SerializedName("pairType") val pairType: String,
    @SerializedName("item1Label") val item1Label: String,
    @SerializedName("item1ImageUrl") val item1ImageUrl: String,
    @SerializedName("item2Label") val item2Label: String,
    @SerializedName("item2ImageUrl") val item2ImageUrl: String,
    @SerializedName("category") val category: String?,
    @SerializedName("moduleId") val moduleId: String,
    @SerializedName("status") val status: String
)

data class CreateMatchingPairRequest(
    @SerializedName("pairType") val pairType: String,
    @SerializedName("item1Label") val item1Label: String,
    @SerializedName("item1ImageUrl") val item1ImageUrl: String,
    @SerializedName("item2Label") val item2Label: String,
    @SerializedName("item2ImageUrl") val item2ImageUrl: String,
    @SerializedName("category") val category: String?,
    @SerializedName("moduleId") val moduleId: String
)

data class UpdateMatchingPairRequest(
    @SerializedName("pairType") val pairType: String?,
    @SerializedName("item1Label") val item1Label: String?,
    @SerializedName("item1ImageUrl") val item1ImageUrl: String?,
    @SerializedName("item2Label") val item2Label: String?,
    @SerializedName("item2ImageUrl") val item2ImageUrl: String?,
    @SerializedName("category") val category: String?
)

// ==================== WORD BUILDER MODELS ====================

data class WordBuilderResponse(
    @SerializedName("id") val id: String,
    @SerializedName("word") val word: String,
    @SerializedName("syllables") val syllables: List<String>,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("audioText") val audioText: String?,
    @SerializedName("audioUrl") val audioUrl: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("difficulty") val difficulty: String?,
    @SerializedName("moduleId") val moduleId: String,
    @SerializedName("status") val status: String
)

// ==================== TRACE AND FOLLOW MODELS ====================
data class TraceAndFollowResponse(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("traceType") val traceType: String, // straight-line, curved, circle, letter
    @SerializedName("traceImageUrl") val traceImageUrl: String,
    @SerializedName("audioText") val audioText: String?,
    @SerializedName("audioUrl") val audioUrl: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("difficulty") val difficulty: String?,
    @SerializedName("moduleId") val moduleId: String,
    @SerializedName("status") val status: String,
    @SerializedName("referenceDrawingPoints") val referenceDrawingPoints: List<DrawingPoint>? = null
)

data class DrawingPoint(
    @SerializedName("x") val x: Float,
    @SerializedName("y") val y: Float
)

// ==================== PROGRESS MODELS ====================

data class UpdateProgressRequest(
    @SerializedName("studentId") val studentId: String,
    @SerializedName("moduleId") val moduleId: String,
    @SerializedName("questionId") val questionId: String? = null,
    @SerializedName("isCorrect") val isCorrect: Boolean? = null,
    @SerializedName("score") val score: Int? = null,
    @SerializedName("totalQuestions") val totalQuestions: Int? = null,
    @SerializedName("currentQuestion") val currentQuestion: Int? = null,
    @SerializedName("correctAnswers") val correctAnswers: Int? = null,
    @SerializedName("wrongAnswers") val wrongAnswers: Int? = null
)

data class ProgressResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: ProgressData?
)

data class ProgressData(
    @SerializedName("id") val id: String?,
    @SerializedName("studentId") val studentId: String,
    @SerializedName("moduleId") val moduleId: String,
    @SerializedName("score") val score: Int,
    @SerializedName("totalQuestions") val totalQuestions: Int,
    @SerializedName("currentQuestion") val currentQuestion: Int,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("questionsAnswered") val questionsAnswered: List<QuestionAnswer>?,
    @SerializedName("starsEarned") val starsEarned: Int
)

data class QuestionAnswer(
    @SerializedName("questionId") val questionId: String,
    @SerializedName("isCorrect") val isCorrect: Boolean,
    @SerializedName("answeredAt") val answeredAt: String?
)

// ==================== ACTIVITY LOG MODELS ====================

data class ActivityLogResponse(
    @SerializedName("id") val id: String,
    @SerializedName("action") val action: String,
    @SerializedName("entityType") val entityType: String,
    @SerializedName("entityId") val entityId: String,
    @SerializedName("entityName") val entityName: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("userName") val userName: String,
    @SerializedName("userRole") val userRole: String,
    @SerializedName("details") val details: Map<String, Any>?,
    @SerializedName("ipAddress") val ipAddress: String?,
    @SerializedName("timestamp") val timestamp: String
)

