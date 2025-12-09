package com.example.playbright.data.repository

import com.example.playbright.data.model.*
import com.example.playbright.data.network.ApiService
import com.example.playbright.data.network.RetrofitClient
import retrofit2.Response

/**
 * Repository class that handles all API calls
 * This follows the Repository pattern in MVVM architecture
 */
class ApiRepository {
    
    private val apiService: ApiService = RetrofitClient.apiService
    
    // ==================== AUTH ====================
    suspend fun login(idToken: String): Response<LoginResponse> {
        return apiService.login(LoginRequest(idToken))
    }
    
    suspend fun facultyLogin(email: String, password: String): Response<FacultyLoginResponse> {
        return apiService.facultyLogin(FacultyLoginRequest(email, password))
    }
    
    suspend fun studentLogin(email: String, password: String): Response<StudentLoginResponse> {
        return apiService.studentLogin(StudentLoginRequest(email, password))
    }
    
    suspend fun logout(): Response<ApiResponse> {
        return apiService.logout()
    }
    
    // ==================== STUDENTS ====================
    suspend fun getStudents(status: String? = null, search: String? = null): Response<List<StudentResponse>> {
        return apiService.getStudents(status, search)
    }
    
    suspend fun getStudentById(id: String): Response<StudentResponse> {
        return apiService.getStudentById(id)
    }
    
    suspend fun createStudent(student: CreateStudentRequest): Response<StudentResponse> {
        return apiService.createStudent(student)
    }
    
    suspend fun updateStudent(id: String, student: UpdateStudentRequest): Response<StudentResponse> {
        return apiService.updateStudent(id, student)
    }
    
    suspend fun archiveStudent(id: String): Response<StudentResponse> {
        return apiService.archiveStudent(id)
    }
    
    suspend fun restoreStudent(id: String): Response<StudentResponse> {
        return apiService.restoreStudent(id)
    }
    
    suspend fun deleteStudent(id: String): Response<ApiResponse> {
        return apiService.deleteStudent(id)
    }
    
    // ==================== FACULTY ====================
    suspend fun getFaculty(status: String? = null, role: String? = null): Response<List<FacultyResponse>> {
        return apiService.getFaculty(status, role)
    }
    
    suspend fun getFacultyById(id: String): Response<FacultyResponse> {
        return apiService.getFacultyById(id)
    }
    
    suspend fun createFaculty(faculty: CreateFacultyRequest): Response<FacultyResponse> {
        return apiService.createFaculty(faculty)
    }
    
    suspend fun updateFaculty(id: String, faculty: UpdateFacultyRequest): Response<FacultyResponse> {
        return apiService.updateFaculty(id, faculty)
    }
    
    // ==================== MODULES ====================
    suspend fun getModules(): Response<List<ModuleResponse>> {
        return apiService.getModules()
    }
    
    suspend fun getModuleById(id: String): Response<ModuleResponse> {
        return apiService.getModuleById(id)
    }
    
    suspend fun getModuleStats(): Response<ModuleStatsResponse> {
        return apiService.getModuleStats()
    }
    
    suspend fun createModule(module: CreateModuleRequest): Response<ModuleResponse> {
        return apiService.createModule(module)
    }
    
    suspend fun updateModule(id: String, module: UpdateModuleRequest): Response<ModuleResponse> {
        return apiService.updateModule(id, module)
    }
    
    suspend fun deleteModule(id: String): Response<ApiResponse> {
        return apiService.deleteModule(id)
    }
    
    // ==================== QUESTIONS ====================
    suspend fun getQuestions(
        moduleId: String? = null,
        status: String? = null,
        search: String? = null
    ): Response<List<QuestionResponse>> {
        return apiService.getQuestions(moduleId, status, search)
    }
    
    suspend fun getQuestionCount(moduleId: String? = null): Response<CountResponse> {
        return apiService.getQuestionCount(moduleId)
    }
    
    suspend fun getQuestionById(id: String): Response<QuestionResponse> {
        return apiService.getQuestionById(id)
    }
    
    suspend fun createQuestion(question: CreateQuestionRequest): Response<QuestionResponse> {
        return apiService.createQuestion(question)
    }
    
    suspend fun updateQuestion(id: String, question: UpdateQuestionRequest): Response<QuestionResponse> {
        return apiService.updateQuestion(id, question)
    }
    
    suspend fun archiveQuestion(id: String): Response<QuestionResponse> {
        return apiService.archiveQuestion(id)
    }
    
    suspend fun deleteQuestion(id: String): Response<ApiResponse> {
        return apiService.deleteQuestion(id)
    }
    
    // ==================== AUDIO IDENTIFICATION ====================
    suspend fun getAudioIdentifications(
        moduleId: String? = null,
        status: String? = null
    ): Response<List<AudioIdentificationResponse>> {
        return apiService.getAudioIdentifications(moduleId, status)
    }
    
    suspend fun getAudioIdentificationById(id: String): Response<AudioIdentificationResponse> {
        return apiService.getAudioIdentificationById(id)
    }
    
    suspend fun createAudioIdentification(audio: CreateAudioIdentificationRequest): Response<AudioIdentificationResponse> {
        return apiService.createAudioIdentification(audio)
    }
    
    suspend fun updateAudioIdentification(
        id: String,
        audio: UpdateAudioIdentificationRequest
    ): Response<AudioIdentificationResponse> {
        return apiService.updateAudioIdentification(id, audio)
    }
    
    // ==================== SEQUENCING CARDS ====================
    suspend fun getSequencingCards(
        moduleId: String? = null,
        status: String? = null
    ): Response<List<SequencingCardResponse>> {
        return apiService.getSequencingCards(moduleId, status)
    }
    
    suspend fun getSequencingCardById(id: String): Response<SequencingCardResponse> {
        return apiService.getSequencingCardById(id)
    }
    
    suspend fun createSequencingCard(card: CreateSequencingCardRequest): Response<SequencingCardResponse> {
        return apiService.createSequencingCard(card)
    }
    
    suspend fun updateSequencingCard(
        id: String,
        card: UpdateSequencingCardRequest
    ): Response<SequencingCardResponse> {
        return apiService.updateSequencingCard(id, card)
    }
    
    // ==================== PICTURE LABELS ====================
    suspend fun getPictureLabels(
        moduleId: String? = null,
        status: String? = null
    ): Response<List<PictureLabelResponse>> {
        return apiService.getPictureLabels(moduleId, status)
    }
    
    suspend fun getPictureLabelById(id: String): Response<PictureLabelResponse> {
        return apiService.getPictureLabelById(id)
    }
    
    suspend fun createPictureLabel(label: CreatePictureLabelRequest): Response<PictureLabelResponse> {
        return apiService.createPictureLabel(label)
    }
    
    suspend fun updatePictureLabel(
        id: String,
        label: UpdatePictureLabelRequest
    ): Response<PictureLabelResponse> {
        return apiService.updatePictureLabel(id, label)
    }
    
    // ==================== EMOTION RECOGNITION ====================
    suspend fun getEmotionRecognitionQuestions(
        moduleId: String? = null,
        status: String? = null
    ): Response<List<EmotionRecognitionResponse>> {
        return apiService.getEmotionRecognitionQuestions(moduleId, status)
    }
    
    suspend fun getEmotionRecognitionCount(moduleId: String? = null): Response<CountResponse> {
        return apiService.getEmotionRecognitionCount(moduleId)
    }
    
    suspend fun getEmotionRecognitionById(id: String): Response<EmotionRecognitionResponse> {
        return apiService.getEmotionRecognitionById(id)
    }
    
    suspend fun createEmotionRecognition(emotion: CreateEmotionRecognitionRequest): Response<EmotionRecognitionResponse> {
        return apiService.createEmotionRecognition(emotion)
    }
    
    suspend fun updateEmotionRecognition(
        id: String,
        emotion: UpdateEmotionRecognitionRequest
    ): Response<EmotionRecognitionResponse> {
        return apiService.updateEmotionRecognition(id, emotion)
    }
    
    // ==================== CATEGORY SELECTION ====================
    suspend fun getCategorySelections(
        moduleId: String? = null,
        status: String? = null
    ): Response<List<CategorySelectionResponse>> {
        return apiService.getCategorySelections(moduleId, status)
    }
    
    suspend fun getCategorySelectionById(id: String): Response<CategorySelectionResponse> {
        return apiService.getCategorySelectionById(id)
    }
    
    suspend fun createCategorySelection(category: CreateCategorySelectionRequest): Response<CategorySelectionResponse> {
        return apiService.createCategorySelection(category)
    }
    
    suspend fun updateCategorySelection(
        id: String,
        category: UpdateCategorySelectionRequest
    ): Response<CategorySelectionResponse> {
        return apiService.updateCategorySelection(id, category)
    }
    
    // ==================== YES/NO QUESTIONS ====================
    suspend fun getYesNoQuestions(
        moduleId: String? = null,
        status: String? = null
    ): Response<List<YesNoQuestionResponse>> {
        return apiService.getYesNoQuestions(moduleId, status)
    }
    
    suspend fun getYesNoQuestionById(id: String): Response<YesNoQuestionResponse> {
        return apiService.getYesNoQuestionById(id)
    }
    
    suspend fun createYesNoQuestion(question: CreateYesNoQuestionRequest): Response<YesNoQuestionResponse> {
        return apiService.createYesNoQuestion(question)
    }
    
    suspend fun updateYesNoQuestion(
        id: String,
        question: UpdateYesNoQuestionRequest
    ): Response<YesNoQuestionResponse> {
        return apiService.updateYesNoQuestion(id, question)
    }
    
    // ==================== TAP REPEAT ====================
    suspend fun getTapRepeats(
        moduleId: String? = null,
        status: String? = null
    ): Response<List<TapRepeatResponse>> {
        return apiService.getTapRepeats(moduleId, status)
    }
    
    suspend fun getTapRepeatById(id: String): Response<TapRepeatResponse> {
        return apiService.getTapRepeatById(id)
    }
    
    suspend fun createTapRepeat(tapRepeat: CreateTapRepeatRequest): Response<TapRepeatResponse> {
        return apiService.createTapRepeat(tapRepeat)
    }
    
    suspend fun updateTapRepeat(
        id: String,
        tapRepeat: UpdateTapRepeatRequest
    ): Response<TapRepeatResponse> {
        return apiService.updateTapRepeat(id, tapRepeat)
    }
    
    // ==================== MATCHING PAIRS ====================
    suspend fun getMatchingPairs(
        moduleId: String? = null,
        status: String? = null,
        pairType: String? = null
    ): Response<List<MatchingPairResponse>> {
        return apiService.getMatchingPairs(moduleId, status, pairType)
    }
    
    suspend fun getMatchingPairById(id: String): Response<MatchingPairResponse> {
        return apiService.getMatchingPairById(id)
    }
    
    // ==================== WORD BUILDER ====================
    suspend fun getWordBuilders(
        moduleId: String? = null,
        status: String? = null,
        category: String? = null,
        difficulty: String? = null
    ): Response<List<WordBuilderResponse>> {
        return apiService.getWordBuilders(moduleId, status, category, difficulty)
    }
    
    suspend fun getWordBuilderById(id: String): Response<WordBuilderResponse> {
        return apiService.getWordBuilderById(id)
    }
    
    // ==================== TRACE AND FOLLOW ====================
    suspend fun getTraceAndFollows(
        moduleId: String? = null,
        status: String? = null,
        category: String? = null,
        difficulty: String? = null,
        traceType: String? = null
    ): Response<List<TraceAndFollowResponse>> {
        return apiService.getTraceAndFollows(moduleId, status, category, difficulty, traceType)
    }
    
    suspend fun getTraceAndFollowById(id: String): Response<TraceAndFollowResponse> {
        return apiService.getTraceAndFollowById(id)
    }
    
    suspend fun createMatchingPair(pair: CreateMatchingPairRequest): Response<MatchingPairResponse> {
        return apiService.createMatchingPair(pair)
    }
    
    suspend fun updateMatchingPair(
        id: String,
        pair: UpdateMatchingPairRequest
    ): Response<MatchingPairResponse> {
        return apiService.updateMatchingPair(id, pair)
    }
    
    // ==================== PROGRESS ====================
    // ==================== PROGRESS (Simplified path-based endpoints) ====================
    suspend fun updateProgress(studentId: String, moduleId: String, request: UpdateProgressRequest): Response<ProgressResponse> {
        // Use simplified path-based endpoint: POST /api/progress/{studentId}/{moduleId}
        return apiService.updateProgress(studentId, moduleId, request)
    }

    suspend fun getProgress(studentId: String, moduleId: String): Response<ProgressResponse> {
        // Use simplified path-based endpoint: GET /api/progress/{studentId}/{moduleId}
        return apiService.getProgress(studentId, moduleId)
    }

    suspend fun getAllProgress(studentId: String): Response<ApiResponse> {
        // Use simplified path-based endpoint: GET /api/progress/{studentId}
        return apiService.getAllProgress(studentId)
    }

    // ==================== ACTIVITY LOGS ====================
    suspend fun getActivityLogs(
        action: String? = null,
        entityType: String? = null,
        limit: Int? = null
    ): Response<List<ActivityLogResponse>> {
        return apiService.getActivityLogs(action, entityType, limit)
    }
    
    // ==================== UPLOAD ====================
    suspend fun uploadImage(
        imagePart: okhttp3.MultipartBody.Part,
        folder: String? = null
    ): Response<UploadResponse> {
        return apiService.uploadImage(imagePart, folder)
    }
    
    suspend fun uploadImages(
        imageParts: List<okhttp3.MultipartBody.Part>
    ): Response<UploadMultipleResponse> {
        return apiService.uploadImages(imageParts)
    }
}

