package com.example.playbright.data.network

import com.example.playbright.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API Service Interface
 * Contains all API endpoints for the PlayBright backend
 */
interface ApiService {
    
    // ==================== AUTH ====================
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @POST("auth/faculty-login")
    suspend fun facultyLogin(@Body request: FacultyLoginRequest): Response<FacultyLoginResponse>
    
    @POST("auth/student-login")
    suspend fun studentLogin(@Body request: StudentLoginRequest): Response<StudentLoginResponse>
    
    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse>
    
    // ==================== STUDENTS ====================
    @GET("api/students")
    suspend fun getStudents(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): Response<List<StudentResponse>>
    
    @GET("api/students/{id}")
    suspend fun getStudentById(@Path("id") id: String): Response<StudentResponse>
    
    @POST("api/students")
    suspend fun createStudent(@Body student: CreateStudentRequest): Response<StudentResponse>
    
    @PUT("api/students/{id}")
    suspend fun updateStudent(
        @Path("id") id: String,
        @Body student: UpdateStudentRequest
    ): Response<StudentResponse>
    
    @PUT("api/students/{id}/archive")
    suspend fun archiveStudent(@Path("id") id: String): Response<StudentResponse>
    
    @PUT("api/students/{id}/restore")
    suspend fun restoreStudent(@Path("id") id: String): Response<StudentResponse>
    
    @DELETE("api/students/{id}")
    suspend fun deleteStudent(@Path("id") id: String): Response<ApiResponse>
    
    // ==================== FACULTY ====================
    @GET("api/faculty")
    suspend fun getFaculty(
        @Query("status") status: String? = null,
        @Query("role") role: String? = null
    ): Response<List<FacultyResponse>>
    
    @GET("api/faculty/{id}")
    suspend fun getFacultyById(@Path("id") id: String): Response<FacultyResponse>
    
    @POST("api/faculty")
    suspend fun createFaculty(@Body faculty: CreateFacultyRequest): Response<FacultyResponse>
    
    @PUT("api/faculty/{id}")
    suspend fun updateFaculty(
        @Path("id") id: String,
        @Body faculty: UpdateFacultyRequest
    ): Response<FacultyResponse>
    
    // ==================== MODULES ====================
    @GET("api/modules")
    suspend fun getModules(): Response<List<ModuleResponse>>
    
    @GET("api/modules/{id}")
    suspend fun getModuleById(@Path("id") id: String): Response<ModuleResponse>
    
    @GET("api/modules/stats")
    suspend fun getModuleStats(): Response<ModuleStatsResponse>
    
    @POST("api/modules")
    suspend fun createModule(@Body module: CreateModuleRequest): Response<ModuleResponse>
    
    @PUT("api/modules/{id}")
    suspend fun updateModule(
        @Path("id") id: String,
        @Body module: UpdateModuleRequest
    ): Response<ModuleResponse>
    
    @DELETE("api/modules/{id}")
    suspend fun deleteModule(@Path("id") id: String): Response<ApiResponse>
    
    // ==================== QUESTIONS ====================
    @GET("api/questions")
    suspend fun getQuestions(
        @Query("moduleId") moduleId: String? = null,
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): Response<List<QuestionResponse>>
    
    @GET("api/questions/count")
    suspend fun getQuestionCount(
        @Query("moduleId") moduleId: String? = null
    ): Response<CountResponse>
    
    @GET("api/questions/{id}")
    suspend fun getQuestionById(@Path("id") id: String): Response<QuestionResponse>
    
    @POST("api/questions")
    suspend fun createQuestion(@Body question: CreateQuestionRequest): Response<QuestionResponse>
    
    @PUT("api/questions/{id}")
    suspend fun updateQuestion(
        @Path("id") id: String,
        @Body question: UpdateQuestionRequest
    ): Response<QuestionResponse>
    
    @PUT("api/questions/{id}/archive")
    suspend fun archiveQuestion(@Path("id") id: String): Response<QuestionResponse>
    
    @DELETE("api/questions/{id}")
    suspend fun deleteQuestion(@Path("id") id: String): Response<ApiResponse>
    
    // ==================== AUDIO IDENTIFICATION ====================
    @GET("api/audio-identifications")
    suspend fun getAudioIdentifications(
        @Query("moduleId") moduleId: String? = null,
        @Query("status") status: String? = null
    ): Response<List<AudioIdentificationResponse>>
    
    @GET("api/audio-identifications/{id}")
    suspend fun getAudioIdentificationById(@Path("id") id: String): Response<AudioIdentificationResponse>
    
    @POST("api/audio-identifications")
    suspend fun createAudioIdentification(@Body audio: CreateAudioIdentificationRequest): Response<AudioIdentificationResponse>
    
    @PUT("api/audio-identifications/{id}")
    suspend fun updateAudioIdentification(
        @Path("id") id: String,
        @Body audio: UpdateAudioIdentificationRequest
    ): Response<AudioIdentificationResponse>
    
    // ==================== SEQUENCING CARDS ====================
    @GET("api/sequencing-cards")
    suspend fun getSequencingCards(
        @Query("moduleId") moduleId: String? = null,
        @Query("status") status: String? = null
    ): Response<List<SequencingCardResponse>>
    
    @GET("api/sequencing-cards/{id}")
    suspend fun getSequencingCardById(@Path("id") id: String): Response<SequencingCardResponse>
    
    @POST("api/sequencing-cards")
    suspend fun createSequencingCard(@Body card: CreateSequencingCardRequest): Response<SequencingCardResponse>
    
    @PUT("api/sequencing-cards/{id}")
    suspend fun updateSequencingCard(
        @Path("id") id: String,
        @Body card: UpdateSequencingCardRequest
    ): Response<SequencingCardResponse>
    
    // ==================== PICTURE LABELS ====================
    @GET("api/picture-labels")
    suspend fun getPictureLabels(
        @Query("moduleId") moduleId: String? = null,
        @Query("status") status: String? = null
    ): Response<List<PictureLabelResponse>>
    
    @GET("api/picture-labels/{id}")
    suspend fun getPictureLabelById(@Path("id") id: String): Response<PictureLabelResponse>
    
    @POST("api/picture-labels")
    suspend fun createPictureLabel(@Body label: CreatePictureLabelRequest): Response<PictureLabelResponse>
    
    @PUT("api/picture-labels/{id}")
    suspend fun updatePictureLabel(
        @Path("id") id: String,
        @Body label: UpdatePictureLabelRequest
    ): Response<PictureLabelResponse>
    
    // ==================== EMOTION RECOGNITION ====================
    @GET("api/emotion-recognition")
    suspend fun getEmotionRecognitionQuestions(
        @Query("moduleId") moduleId: String? = null,
        @Query("status") status: String? = null
    ): Response<List<EmotionRecognitionResponse>>
    
    @GET("api/emotion-recognition/count")
    suspend fun getEmotionRecognitionCount(
        @Query("moduleId") moduleId: String? = null
    ): Response<CountResponse>
    
    @GET("api/emotion-recognition/{id}")
    suspend fun getEmotionRecognitionById(@Path("id") id: String): Response<EmotionRecognitionResponse>
    
    @POST("api/emotion-recognition")
    suspend fun createEmotionRecognition(@Body emotion: CreateEmotionRecognitionRequest): Response<EmotionRecognitionResponse>
    
    @PUT("api/emotion-recognition/{id}")
    suspend fun updateEmotionRecognition(
        @Path("id") id: String,
        @Body emotion: UpdateEmotionRecognitionRequest
    ): Response<EmotionRecognitionResponse>
    
    // ==================== CATEGORY SELECTION ====================
    @GET("api/category-selections")
    suspend fun getCategorySelections(
        @Query("moduleId") moduleId: String? = null,
        @Query("status") status: String? = null
    ): Response<List<CategorySelectionResponse>>
    
    @GET("api/category-selections/{id}")
    suspend fun getCategorySelectionById(@Path("id") id: String): Response<CategorySelectionResponse>
    
    @POST("api/category-selections")
    suspend fun createCategorySelection(@Body category: CreateCategorySelectionRequest): Response<CategorySelectionResponse>
    
    @PUT("api/category-selections/{id}")
    suspend fun updateCategorySelection(
        @Path("id") id: String,
        @Body category: UpdateCategorySelectionRequest
    ): Response<CategorySelectionResponse>
    
    // ==================== YES/NO QUESTIONS ====================
    @GET("api/yes-no-questions")
    suspend fun getYesNoQuestions(
        @Query("moduleId") moduleId: String? = null,
        @Query("status") status: String? = null
    ): Response<List<YesNoQuestionResponse>>
    
    @GET("api/yes-no-questions/{id}")
    suspend fun getYesNoQuestionById(@Path("id") id: String): Response<YesNoQuestionResponse>
    
    @POST("api/yes-no-questions")
    suspend fun createYesNoQuestion(@Body question: CreateYesNoQuestionRequest): Response<YesNoQuestionResponse>
    
    @PUT("api/yes-no-questions/{id}")
    suspend fun updateYesNoQuestion(
        @Path("id") id: String,
        @Body question: UpdateYesNoQuestionRequest
    ): Response<YesNoQuestionResponse>
    
    // ==================== TAP REPEAT ====================
    @GET("api/tap-repeats")
    suspend fun getTapRepeats(
        @Query("moduleId") moduleId: String? = null,
        @Query("status") status: String? = null
    ): Response<List<TapRepeatResponse>>
    
    @GET("api/tap-repeats/{id}")
    suspend fun getTapRepeatById(@Path("id") id: String): Response<TapRepeatResponse>
    
    @POST("api/tap-repeats")
    suspend fun createTapRepeat(@Body tapRepeat: CreateTapRepeatRequest): Response<TapRepeatResponse>
    
    @PUT("api/tap-repeats/{id}")
    suspend fun updateTapRepeat(
        @Path("id") id: String,
        @Body tapRepeat: UpdateTapRepeatRequest
    ): Response<TapRepeatResponse>
    
    // ==================== MATCHING PAIRS ====================
    @GET("api/matching-pairs")
    suspend fun getMatchingPairs(
        @Query("moduleId") moduleId: String? = null,
        @Query("status") status: String? = null,
        @Query("pairType") pairType: String? = null
    ): Response<List<MatchingPairResponse>>
    
    @GET("api/matching-pairs/{id}")
    suspend fun getMatchingPairById(@Path("id") id: String): Response<MatchingPairResponse>
    
    @POST("api/matching-pairs")
    suspend fun createMatchingPair(@Body pair: CreateMatchingPairRequest): Response<MatchingPairResponse>
    
    @PUT("api/matching-pairs/{id}")
    suspend fun updateMatchingPair(
        @Path("id") id: String,
        @Body pair: UpdateMatchingPairRequest
    ): Response<MatchingPairResponse>
    
    // ==================== PROGRESS ====================
    @POST("api/progress/update")
    suspend fun updateProgress(@Body request: UpdateProgressRequest): Response<ProgressResponse>

    @GET("api/progress/get")
    suspend fun getProgress(
        @Query("studentId") studentId: String,
        @Query("moduleId") moduleId: String
    ): Response<ProgressResponse>

    @GET("api/progress/all")
    suspend fun getAllProgress(@Query("studentId") studentId: String): Response<ApiResponse>

    // ==================== ACTIVITY LOGS ====================
    @GET("api/activity-logs")
    suspend fun getActivityLogs(
        @Query("action") action: String? = null,
        @Query("entityType") entityType: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<ActivityLogResponse>>
    
    // ==================== UPLOAD ====================
    @Multipart
    @POST("api/upload/image")
    suspend fun uploadImage(
        @Part image: okhttp3.MultipartBody.Part,
        @Part("folder") folder: String? = null
    ): Response<UploadResponse>
    
    @Multipart
    @POST("api/upload/images")
    suspend fun uploadImages(
        @Part images: List<okhttp3.MultipartBody.Part>
    ): Response<UploadMultipleResponse>
}

