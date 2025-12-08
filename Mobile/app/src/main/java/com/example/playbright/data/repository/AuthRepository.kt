package com.example.playbright.data.repository

import com.example.playbright.data.model.User
import com.example.playbright.data.model.StudentLoginResponse
import com.example.playbright.data.model.FacultyLoginResponse
import com.example.playbright.data.model.LoginResponse
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val apiRepository = ApiRepository()
    
    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        
        // Try to get stored student ID from SharedPreferences
        try {
            val context = com.example.playbright.PlayBrightApplication.getInstance()
            val sharedPrefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
            val studentId = sharedPrefs.getString("student_id", null)
            
            return User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "",
                photoUrl = firebaseUser.photoUrl?.toString(),
                studentId = studentId
            )
        } catch (e: Exception) {
            // Fallback if Application instance not available
            return User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: "",
                photoUrl = firebaseUser.photoUrl?.toString(),
                studentId = null
            )
        }
    }
    
    fun saveStudentId(studentId: String) {
        try {
            val context = com.example.playbright.PlayBrightApplication.getInstance()
            val sharedPrefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("student_id", studentId).apply()
        } catch (e: Exception) {
            // Silently fail if Application instance not available
        }
    }
    
    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            // First, try student login
            val studentResponse = apiRepository.studentLogin(email, password)
            
            if (studentResponse.isSuccessful && studentResponse.body()?.status == "success") {
                val loginData = studentResponse.body()!!
                val customToken = loginData.customToken
                
                if (customToken != null) {
                    // Sign in to Firebase with custom token
                    val credential = com.google.firebase.auth.FirebaseAuth.getInstance()
                        .signInWithCustomToken(customToken).await()
                    
                    val user = credential.user
                    if (user != null) {
                        val studentUser = loginData.user
                        val userData = User(
                            id = user.uid,
                            email = user.email ?: email,
                            displayName = studentUser?.name ?: "",
                            photoUrl = studentUser?.photoUrl,
                            studentId = studentUser?.id // Store the actual student document ID
                        )
                        // Save student ID to SharedPreferences for later retrieval
                        studentUser?.id?.let { saveStudentId(it) }
                        return Result.success(userData)
                    }
                }
            }
            
            // If student login fails, try faculty login
            val facultyResponse = apiRepository.facultyLogin(email, password)
            
            if (facultyResponse.isSuccessful && facultyResponse.body()?.status == "success") {
                val loginData = facultyResponse.body()!!
                val customToken = loginData.customToken
                
                if (customToken != null) {
                    // Sign in to Firebase with custom token
                    val credential = com.google.firebase.auth.FirebaseAuth.getInstance()
                        .signInWithCustomToken(customToken).await()
                    
                    val user = credential.user
                    if (user != null) {
                        val facultyUser = loginData.user
                        val userData = User(
                            id = user.uid,
                            email = user.email ?: email,
                            displayName = facultyUser?.name ?: "",
                            photoUrl = null // UserData doesn't have photoUrl
                        )
                        return Result.success(userData)
                    }
                }
            }
            
            // If both fail, return error
            val errorMessage = studentResponse.body()?.message 
                ?: facultyResponse.body()?.message 
                ?: "Invalid email or password"
            Result.failure(Exception(errorMessage))
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<User> {
        return try {
            // Get ID token from Google account
            val idToken = account.idToken ?: return Result.failure(Exception("No ID token from Google"))
            
            // Sign in to Firebase first to get Firebase ID token
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val firebaseResult = auth.signInWithCredential(credential).await()
            val firebaseUser = firebaseResult.user
            
            if (firebaseUser == null) {
                return Result.failure(Exception("Firebase sign-in failed"))
            }
            
            // Get Firebase ID token
            val firebaseIdToken = firebaseUser.getIdToken(false).await().token
                ?: return Result.failure(Exception("Failed to get Firebase ID token"))
            
            // Call backend API with Firebase ID token
            val response = apiRepository.login(firebaseIdToken)
            
            if (response.isSuccessful && response.body()?.status == "success") {
                val userData = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
                
                // Save user to Firestore
                saveUserToFirestore(userData)
                
                Result.success(userData)
            } else {
                // Sign out from Firebase if backend login fails
                auth.signOut()
                val errorMessage = response.body()?.message ?: "Authentication failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signUp(email: String, password: String, displayName: String): Result<User> {
        return try {
            // Create user in Firebase Auth
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            
            if (user != null) {
                // Update profile
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates).await()
                
                val userData = User(
                    id = user.uid,
                    email = user.email ?: "",
                    displayName = displayName,
                    photoUrl = user.photoUrl?.toString()
                )
                
                // Save user to Firestore
                saveUserToFirestore(userData)
                
                Result.success(userData)
            } else {
                Result.failure(Exception("User is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun saveUserToFirestore(user: User) {
        try {
            firestore.collection("users")
                .document(user.id)
                .set(user)
                .await()
        } catch (e: Exception) {
            // Log error but don't fail the sign-in
            e.printStackTrace()
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
    
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}
