# API Network Layer

This package contains all API-related code using Retrofit for network calls.

## Structure

```
data/network/
├── ApiService.kt          # Retrofit interface with all API endpoints
├── RetrofitClient.kt      # Retrofit client configuration
├── NetworkModule.kt       # Network utility functions
└── README.md             # This file

data/repository/
└── ApiRepository.kt      # Repository class that wraps API calls

data/model/
└── ApiModels.kt          # All data models for API requests/responses
```

## Usage

### 1. Basic API Call

```kotlin
// In ViewModel or Repository
val repository = ApiRepository()

viewModelScope.launch {
    try {
        val response = repository.getStudents(status = "Active")
        if (response.isSuccessful) {
            val students = response.body()
            // Handle success
        } else {
            // Handle error
        }
    } catch (e: Exception) {
        // Handle exception
    }
}
```

### 2. Using in ViewModel

```kotlin
class StudentsViewModel : ViewModel() {
    private val repository = ApiRepository()
    
    private val _students = MutableLiveData<List<StudentResponse>>()
    val students: LiveData<List<StudentResponse>> = _students
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadStudents() {
        viewModelScope.launch {
            try {
                val response = repository.getStudents(status = "Active")
                if (response.isSuccessful) {
                    _students.value = response.body()
                } else {
                    _error.value = "Failed to load students"
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
```

### 3. Upload Image

```kotlin
val file = File(imagePath)
val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

val response = repository.uploadImage(imagePart, folder = "student-photos")
```

## API Base URL

The base URL is configured in `RetrofitClient.kt`:
- **Android Emulator**: `http://10.0.2.2:3000/`
- **Physical Device**: `http://YOUR_IP_ADDRESS:3000/`

To change it, modify the `BASE_URL` constant in `RetrofitClient.kt`.

## Available Endpoints

### Auth
- `POST /auth/login`
- `POST /auth/faculty-login`
- `POST /auth/logout`

### Students
- `GET /api/students`
- `GET /api/students/{id}`
- `POST /api/students`
- `PUT /api/students/{id}`
- `PUT /api/students/{id}/archive`
- `PUT /api/students/{id}/restore`
- `DELETE /api/students/{id}`

### Faculty
- `GET /api/faculty`
- `GET /api/faculty/{id}`
- `POST /api/faculty`
- `PUT /api/faculty/{id}`

### Modules
- `GET /api/modules`
- `GET /api/modules/{id}`
- `GET /api/modules/stats`
- `POST /api/modules`
- `PUT /api/modules/{id}`
- `DELETE /api/modules/{id}`

### Questions
- `GET /api/questions`
- `GET /api/questions/count`
- `GET /api/questions/{id}`
- `POST /api/questions`
- `PUT /api/questions/{id}`
- `PUT /api/questions/{id}/archive`
- `DELETE /api/questions/{id}`

### Learning Modules
- Audio Identifications
- Sequencing Cards
- Picture Labels
- Emotion Recognition
- Category Selection
- Yes/No Questions
- Tap Repeat
- Matching Pairs

### Other
- `GET /api/activity-logs`
- `POST /api/upload/image`
- `POST /api/upload/images`

## Error Handling

All API calls return `Response<T>` from Retrofit. Always check:
1. `response.isSuccessful` - HTTP status code 200-299
2. `response.body()` - The actual data
3. `response.errorBody()` - Error details if failed

## Network Configuration

- **Timeout**: 30 seconds for connect, read, and write
- **Logging**: Full request/response logging enabled (change level in `RetrofitClient.kt`)
- **Gson**: Configured with lenient parsing and date format support

## Testing

To test API connectivity:

```kotlin
viewModelScope.launch {
    try {
        val response = repository.getModules()
        if (response.isSuccessful) {
            Log.d("API", "Success: ${response.body()?.size} modules")
        } else {
            Log.e("API", "Error: ${response.code()} - ${response.message()}")
        }
    } catch (e: Exception) {
        Log.e("API", "Exception: ${e.message}")
    }
}
```

