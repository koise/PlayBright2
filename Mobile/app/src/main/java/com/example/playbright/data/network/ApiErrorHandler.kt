package com.example.playbright.data.network

import android.util.Log
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Helper class for handling API errors consistently
 */
object ApiErrorHandler {
    
    private const val TAG = "ApiErrorHandler"
    
    /**
     * Handle API errors and return user-friendly message
     */
    fun handleError(throwable: Throwable): String {
        return when (throwable) {
            is HttpException -> {
                handleHttpError(throwable)
            }
            is SocketTimeoutException -> {
                "Request timed out. Please check your connection and try again."
            }
            is UnknownHostException -> {
                "Cannot reach server. Please check your internet connection."
            }
            is IOException -> {
                "Network error: ${throwable.message ?: "Unable to connect to server"}"
            }
            is NoNetworkException -> {
                throwable.message ?: "No internet connection available."
            }
            is ApiTimeoutException -> {
                throwable.message ?: "Request timed out."
            }
            is ApiException -> {
                throwable.message ?: "An error occurred."
            }
            else -> {
                Log.e(TAG, "Unexpected error", throwable)
                "An unexpected error occurred: ${throwable.message ?: "Unknown error"}"
            }
        }
    }
    
    private fun handleHttpError(httpException: HttpException): String {
        return when (httpException.code()) {
            400 -> "Invalid request. Please check your input."
            401 -> "Unauthorized. Please login again."
            403 -> "Access denied. You don't have permission."
            404 -> "Resource not found."
            408 -> "Request timed out. Please try again."
            429 -> "Too many requests. Please wait a moment."
            500 -> "Server error. Please try again later."
            502 -> "Bad gateway. The server is temporarily unavailable."
            503 -> "Service unavailable. Please try again later."
            504 -> "Gateway timeout. Please try again."
            else -> "Error ${httpException.code()}: ${httpException.message()}"
        }
    }
    
    /**
     * Check if error is network-related
     */
    fun isNetworkError(throwable: Throwable): Boolean {
        return throwable is IOException ||
               throwable is SocketTimeoutException ||
               throwable is UnknownHostException ||
               throwable is NoNetworkException
    }
    
    /**
     * Check if error is retryable
     */
    fun isRetryable(throwable: Throwable): Boolean {
        return when (throwable) {
            is HttpException -> {
                throwable.code() in 500..599 || throwable.code() == 408
            }
            is SocketTimeoutException -> true
            is UnknownHostException -> true
            is NoNetworkException -> true
            is ApiTimeoutException -> true
            else -> false
        }
    }
}

