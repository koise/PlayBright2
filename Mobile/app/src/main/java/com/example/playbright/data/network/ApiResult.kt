package com.example.playbright.data.network

/**
 * Sealed class for API results with success and error states
 */
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Exception, val message: String) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

/**
 * Extension function to convert Retrofit Response to ApiResult
 */
fun <T> retrofit2.Response<T>.toApiResult(): ApiResult<T> {
    return try {
        if (this.isSuccessful && this.body() != null) {
            ApiResult.Success(this.body()!!)
        } else {
            val errorMessage = this.errorBody()?.string() 
                ?: "Unknown error occurred"
            ApiResult.Error(
                Exception(errorMessage),
                "Request failed: ${this.code()}"
            )
        }
    } catch (e: Exception) {
        ApiResult.Error(e, e.message ?: "Unknown error")
    }
}

/**
 * Extension function to handle exceptions and convert to ApiResult
 */
fun <T> Result<T>.toApiResult(): ApiResult<T> {
    return try {
        val value = this.getOrThrow()
        ApiResult.Success(value)
    } catch (e: Exception) {
        val errorMessage = when (e) {
            is NoNetworkException -> "No internet connection. Please check your network."
            is ApiTimeoutException -> "Request timed out. Please try again."
            is ServerException -> "Server error: ${e.message}"
            is ClientException -> "Error: ${e.message}"
            is ApiException -> e.message ?: "An error occurred"
            else -> "An unexpected error occurred: ${e.message}"
        }
        ApiResult.Error(e, errorMessage)
    }
}

