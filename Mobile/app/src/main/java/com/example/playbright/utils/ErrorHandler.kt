package com.example.playbright.utils

import com.example.playbright.data.network.*

/**
 * Utility class for handling and formatting API errors
 */
object ErrorHandler {
    
    /**
     * Get user-friendly error message from exception
     */
    fun getErrorMessage(exception: Exception): String {
        return when (exception) {
            is NoNetworkException -> {
                "No internet connection. Please check your network settings and try again."
            }
            is ApiTimeoutException -> {
                "Request timed out. The server is taking too long to respond. Please try again."
            }
            is ServerException -> {
                when (exception.code) {
                    500 -> "Server error. Please try again later."
                    503 -> "Service unavailable. The server is temporarily down."
                    502 -> "Bad gateway. Please try again later."
                    else -> "Server error (${exception.code}): ${exception.message}"
                }
            }
            is ClientException -> {
                when (exception.code) {
                    401 -> "Unauthorized. Please check your credentials."
                    403 -> "Access denied. You don't have permission to perform this action."
                    404 -> "Resource not found."
                    400 -> "Invalid request: ${exception.message}"
                    else -> "Error (${exception.code}): ${exception.message}"
                }
            }
            is ApiException -> {
                exception.message ?: "An error occurred. Please try again."
            }
            else -> {
                exception.message ?: "An unexpected error occurred. Please try again."
            }
        }
    }
    
    /**
     * Check if error is retryable
     */
    fun isRetryable(exception: Exception): Boolean {
        return when (exception) {
            is NoNetworkException -> true
            is ApiTimeoutException -> true
            is ServerException -> exception.code in 500..599
            else -> false
        }
    }
    
    /**
     * Get retry delay in milliseconds
     */
    fun getRetryDelay(attempt: Int): Long {
        // Exponential backoff: 1s, 2s, 4s
        return (1000 * (1 shl (attempt - 1))).toLong()
    }
}

