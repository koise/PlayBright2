package com.example.playbright.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.IOException

/**
 * Centralized sound manager for game audio effects and background music
 * Handles success sounds, error sounds, and background music
 * 
 * Note: Sound files should be in res/raw/ folder for MediaPlayer.create() to work
 * Files: backgroundgamemusic.mp3, success_bell-6776.mp3, wrong_answer.mp3
 */
class SoundManager private constructor(context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: SoundManager? = null
        
        fun getInstance(context: Context): SoundManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SoundManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val appContext: Context = context.applicationContext
    private var backgroundMusicPlayer: MediaPlayer? = null
    private var successSoundPlayer: MediaPlayer? = null
    private var errorSoundPlayer: MediaPlayer? = null
    
    private var isBackgroundMusicEnabled = true
    private var isSoundEffectsEnabled = true
    private var backgroundMusicVolume = 0.3f // 30% volume for background
    private var soundEffectsVolume = 0.7f // 70% volume for effects
    
    /**
     * Start playing background music (loops continuously)
     */
    fun startBackgroundMusic() {
        if (!isBackgroundMusicEnabled) return
        
        try {
            stopBackgroundMusic() // Stop any existing music
            
            // Load from assets/sounds/ folder
            val assetManager = appContext.assets
            val fd = assetManager.openFd("sounds/backgroundgamemusic.mp3")
            backgroundMusicPlayer = MediaPlayer().apply {
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                prepare()
                isLooping = true
                setVolume(backgroundMusicVolume, backgroundMusicVolume)
                start()
            }
            fd.close()
            Log.d("SoundManager", "✅ Background music started")
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Error starting background music: ${e.message}", e)
            Log.e("SoundManager", "💡 Make sure files are in assets/sounds/ folder. See SOUND_SETUP_INSTRUCTIONS.md")
        }
    }
    
    /**
     * Stop background music
     */
    fun stopBackgroundMusic() {
        try {
            backgroundMusicPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            backgroundMusicPlayer = null
            Log.d("SoundManager", "🛑 Background music stopped")
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Error stopping background music: ${e.message}", e)
        }
    }
    
    /**
     * Pause background music (can be resumed)
     */
    fun pauseBackgroundMusic() {
        try {
            backgroundMusicPlayer?.pause()
            Log.d("SoundManager", "⏸️ Background music paused")
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Error pausing background music: ${e.message}", e)
        }
    }
    
    /**
     * Resume background music
     */
    fun resumeBackgroundMusic() {
        if (!isBackgroundMusicEnabled) return
        
        try {
            backgroundMusicPlayer?.apply {
                if (!isPlaying) {
                    start()
                    Log.d("SoundManager", "▶️ Background music resumed")
                }
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Error resuming background music: ${e.message}", e)
        }
    }
    
    /**
     * Play success sound effect
     */
    fun playSuccessSound() {
        if (!isSoundEffectsEnabled) return
        
        try {
            // Release previous sound if playing
            successSoundPlayer?.release()
            
            // Load from assets/sounds/ folder
            val assetManager = appContext.assets
            val fd = assetManager.openFd("sounds/success_bell-6776.mp3")
            successSoundPlayer = MediaPlayer().apply {
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                prepare()
                setVolume(soundEffectsVolume, soundEffectsVolume)
                setOnCompletionListener {
                    release()
                    successSoundPlayer = null
                }
                start()
            }
            fd.close()
            Log.d("SoundManager", "🔔 Success sound played")
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Error playing success sound: ${e.message}", e)
            Log.e("SoundManager", "💡 Make sure files are in assets/sounds/ folder. See SOUND_SETUP_INSTRUCTIONS.md")
        }
    }
    
    /**
     * Play error/wrong answer sound effect
     */
    fun playErrorSound() {
        if (!isSoundEffectsEnabled) return
        
        try {
            // Release previous sound if playing
            errorSoundPlayer?.release()
            
            // Load from assets/sounds/ folder
            val assetManager = appContext.assets
            val fd = assetManager.openFd("sounds/wrong_answer.mp3")
            errorSoundPlayer = MediaPlayer().apply {
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                prepare()
                setVolume(soundEffectsVolume, soundEffectsVolume)
                setOnCompletionListener {
                    release()
                    errorSoundPlayer = null
                }
                start()
            }
            fd.close()
            Log.d("SoundManager", "❌ Error sound played")
        } catch (e: Exception) {
            Log.e("SoundManager", "❌ Error playing error sound: ${e.message}", e)
            Log.e("SoundManager", "💡 Make sure files are in assets/sounds/ folder. See SOUND_SETUP_INSTRUCTIONS.md")
        }
    }
    
    /**
     * Set background music volume (0.0f to 1.0f)
     */
    fun setBackgroundMusicVolume(volume: Float) {
        backgroundMusicVolume = volume.coerceIn(0f, 1f)
        backgroundMusicPlayer?.setVolume(backgroundMusicVolume, backgroundMusicVolume)
    }
    
    /**
     * Set sound effects volume (0.0f to 1.0f)
     */
    fun setSoundEffectsVolume(volume: Float) {
        soundEffectsVolume = volume.coerceIn(0f, 1f)
    }
    
    /**
     * Enable/disable background music
     */
    fun setBackgroundMusicEnabled(enabled: Boolean) {
        isBackgroundMusicEnabled = enabled
        if (!enabled) {
            stopBackgroundMusic()
        } else {
            startBackgroundMusic()
        }
    }
    
    /**
     * Enable/disable sound effects
     */
    fun setSoundEffectsEnabled(enabled: Boolean) {
        isSoundEffectsEnabled = enabled
    }
    
    /**
     * Clean up all resources
     */
    fun release() {
        stopBackgroundMusic()
        successSoundPlayer?.release()
        errorSoundPlayer?.release()
        successSoundPlayer = null
        errorSoundPlayer = null
        Log.d("SoundManager", "🧹 SoundManager released")
    }
}

