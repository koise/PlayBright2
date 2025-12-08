package com.example.playbright.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Interceptor for handling API errors and retries
 */
class ApiInterceptor(private val context: Context) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Check network connectivity
        if (!isNetworkAvailable(context)) {
            throw NoNetworkException("No internet connection. Please check your network settings.")
        }
        
        // Try the request with retry logic
        var response: Response? = null
        var lastException: Exception? = null
        val maxRetries = 3
        var retryCount = 0
        
        while (retryCount < maxRetries) {
            try {
                response = chain.proceed(request)
                
                // If successful or client error (4xx), return immediately
                if (response.isSuccessful || response.code in 400..499) {
                    return response
                }
                
                // For server errors (5xx), retry
                if (response.code in 500..599 && retryCount < maxRetries - 1) {
                    response.close()
                    retryCount++
                    Thread.sleep((1000 * retryCount).toLong()) // Exponential backoff
                    continue
                }
                
                return response
                
            } catch (e: SocketTimeoutException) {
                lastException = e
                if (retryCount < maxRetries - 1) {
                    retryCount++
                    Thread.sleep((1000 * retryCount).toLong())
                    continue
                }
                throw ApiTimeoutException("Request timed out. Please try again.")
                
            } catch (e: UnknownHostException) {
                throw NoNetworkException("Cannot reach server. Please check your internet connection.")
                
            } catch (e: IOException) {
                lastException = e
                if (retryCount < maxRetries - 1) {
                    retryCount++
                    Thread.sleep((1000 * retryCount).toLong())
                    continue
                }
                throw ApiException("Network error: ${e.message}")
            }
        }
        
        // If we get here, all retries failed
        throw ApiException("Failed to connect to server after $maxRetries attempts. Please try again later.")
    }
    
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
}

/**
 * Custom exceptions for API errors
 */
class ApiException(message: String) : Exception(message)
class NoNetworkException(message: String) : Exception(message)
class ApiTimeoutException(message: String) : Exception(message)
class ServerException(message: String, val code: Int) : Exception(message)
class ClientException(message: String, val code: Int) : Exception(message)

