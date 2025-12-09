package com.example.playbright.ui.activity

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.playbright.R
import com.example.playbright.databinding.ActivitySettingsBinding
import com.example.playbright.utils.SoundManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("PlayBrightSettings", Context.MODE_PRIVATE)
        soundManager = SoundManager.getInstance(this)

        setupToolbar()
        setupSettings()
        loadSettings()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Settings"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSettings() {
        // Background Music Toggle
        binding.switchBackgroundMusic.setOnCheckedChangeListener { _, isChecked ->
            soundManager.setBackgroundMusicEnabled(isChecked)
            sharedPreferences.edit().putBoolean("background_music_enabled", isChecked).apply()
            if (isChecked) {
                soundManager.startBackgroundMusic()
            } else {
                soundManager.stopBackgroundMusic()
            }
            // Show/hide volume slider
            binding.layoutMusicVolume.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Sound Effects Toggle
        binding.switchSoundEffects.setOnCheckedChangeListener { _, isChecked ->
            soundManager.setSoundEffectsEnabled(isChecked)
            sharedPreferences.edit().putBoolean("sound_effects_enabled", isChecked).apply()
        }

        // Background Music Volume
        binding.seekBarMusicVolume.setOnSeekBarChangeListener(
            object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val volume = progress / 100f
                        soundManager.setBackgroundMusicVolume(volume)
                        binding.tvMusicVolume.text = "$progress%"
                        sharedPreferences.edit().putFloat("background_music_volume", volume).apply()
                    }
                }

                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            }
        )

        // Sound Effects Volume
        binding.seekBarEffectsVolume.setOnSeekBarChangeListener(
            object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val volume = progress / 100f
                        soundManager.setSoundEffectsVolume(volume)
                        binding.tvEffectsVolume.text = "$progress%"
                        sharedPreferences.edit().putFloat("sound_effects_volume", volume).apply()
                    }
                }

                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            }
        )

        // Dark Mode Toggle
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            sharedPreferences.edit().putBoolean("dark_mode_enabled", isChecked).apply()
        }

        // About Section
        binding.cardAbout.setOnClickListener {
            Toast.makeText(this, "PlayBright v1.0\nEducational Games for Kids", Toast.LENGTH_LONG).show()
        }

        // Reset Settings
        binding.btnResetSettings.setOnClickListener {
            resetSettings()
        }
    }

    private fun loadSettings() {
        // Load background music setting
        val musicEnabled = sharedPreferences.getBoolean("background_music_enabled", true)
        binding.switchBackgroundMusic.isChecked = musicEnabled
        soundManager.setBackgroundMusicEnabled(musicEnabled)
        binding.layoutMusicVolume.visibility = if (musicEnabled) View.VISIBLE else View.GONE

        // Load sound effects setting
        val effectsEnabled = sharedPreferences.getBoolean("sound_effects_enabled", true)
        binding.switchSoundEffects.isChecked = effectsEnabled
        soundManager.setSoundEffectsEnabled(effectsEnabled)

        // Load music volume
        val musicVolume = sharedPreferences.getFloat("background_music_volume", 0.3f)
        val musicVolumePercent = (musicVolume * 100).toInt()
        binding.seekBarMusicVolume.progress = musicVolumePercent
        binding.tvMusicVolume.text = "$musicVolumePercent%"
        soundManager.setBackgroundMusicVolume(musicVolume)

        // Load effects volume
        val effectsVolume = sharedPreferences.getFloat("sound_effects_volume", 0.7f)
        val effectsVolumePercent = (effectsVolume * 100).toInt()
        binding.seekBarEffectsVolume.progress = effectsVolumePercent
        binding.tvEffectsVolume.text = "$effectsVolumePercent%"
        soundManager.setSoundEffectsVolume(effectsVolume)

        // Load dark mode setting
        val darkModeEnabled = sharedPreferences.getBoolean("dark_mode_enabled", false)
        binding.switchDarkMode.isChecked = darkModeEnabled
        if (darkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun resetSettings() {
        // Reset to defaults
        binding.switchBackgroundMusic.isChecked = true
        binding.switchSoundEffects.isChecked = true
        binding.seekBarMusicVolume.progress = 30
        binding.seekBarEffectsVolume.progress = 70
        binding.switchDarkMode.isChecked = false

        // Apply defaults
        soundManager.setBackgroundMusicEnabled(true)
        soundManager.setSoundEffectsEnabled(true)
        soundManager.setBackgroundMusicVolume(0.3f)
        soundManager.setSoundEffectsVolume(0.7f)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Save to preferences
        sharedPreferences.edit().apply {
            putBoolean("background_music_enabled", true)
            putBoolean("sound_effects_enabled", true)
            putFloat("background_music_volume", 0.3f)
            putFloat("sound_effects_volume", 0.7f)
            putBoolean("dark_mode_enabled", false)
            apply()
        }

        binding.tvMusicVolume.text = "30%"
        binding.tvEffectsVolume.text = "70%"

        Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
    }
}

