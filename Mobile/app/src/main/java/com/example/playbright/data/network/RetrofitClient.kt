package com.example.playbright.data.network

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    // Base URL - Change this to your backend URL
    // For Android emulator: http://10.0.2.2:3000
    // For physical device: http://YOUR_IP:3000
    private const val BASE_URL = "http://10.0.2.2:3000/"
    
    private var context: Context? = null
    
    fun initialize(context: Context) {
        this.context = context.applicationContext
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Enable logging in debug mode
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
        
        // Add API interceptor for error handling if context is available
        context?.let {
            builder.addInterceptor(ApiInterceptor(it))
        }
        
        return builder.build()
    }
    
    private val okHttpClient = createOkHttpClient()
    
    private val gson = GsonBuilder()
        .setLenient()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}

