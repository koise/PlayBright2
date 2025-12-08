package com.example.playbright.data.network

/**
 * API Constants
 */
object ApiConstants {
    
    // Base URLs
    const val BASE_URL_EMULATOR = "http://10.0.2.2:3000/"
    const val BASE_URL_LOCALHOST = "http://localhost:3000/"
    
    // API Endpoints
    object Endpoints {
        const val AUTH_LOGIN = "auth/login"
        const val AUTH_FACULTY_LOGIN = "auth/faculty-login"
        const val AUTH_LOGOUT = "auth/logout"
        
        const val STUDENTS = "api/students"
        const val FACULTY = "api/faculty"
        const val MODULES = "api/modules"
        const val QUESTIONS = "api/questions"
        const val AUDIO_IDENTIFICATIONS = "api/audio-identifications"
        const val SEQUENCING_CARDS = "api/sequencing-cards"
        const val PICTURE_LABELS = "api/picture-labels"
        const val EMOTION_RECOGNITION = "api/emotion-recognition"
        const val CATEGORY_SELECTIONS = "api/category-selections"
        const val YES_NO_QUESTIONS = "api/yes-no-questions"
        const val TAP_REPEATS = "api/tap-repeats"
        const val MATCHING_PAIRS = "api/matching-pairs"
        const val ACTIVITY_LOGS = "api/activity-logs"
        const val UPLOAD_IMAGE = "api/upload/image"
        const val UPLOAD_IMAGES = "api/upload/images"
    }
    
    // Query Parameters
    object QueryParams {
        const val STATUS = "status"
        const val SEARCH = "search"
        const val MODULE_ID = "moduleId"
        const val ROLE = "role"
        const val ACTION = "action"
        const val ENTITY_TYPE = "entityType"
        const val LIMIT = "limit"
        const val PAIR_TYPE = "pairType"
    }
    
    // Status Values
    object Status {
        const val ACTIVE = "Active"
        const val ARCHIVED = "Archived"
    }
}

