package com.example.playbright.ui.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.playbright.R
import com.example.playbright.databinding.ActivityProfileBinding
import com.example.playbright.ui.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var selectedPhotoUrl: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupObservers()
        setupClickListeners()
        
        // Load student profile
        viewModel.loadStudentProfile()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupObservers() {
        viewModel.student.observe(this) { student ->
            student?.let {
                populateForm(it)
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
        
        viewModel.updateSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                // Reload profile to get updated data
                viewModel.loadStudentProfile()
            }
        }
    }
    
    private fun setupClickListeners() {
        // Birthday picker
        binding.etBirthday.setOnClickListener {
            showDatePicker()
        }
        
        // Change photo button
        binding.btnChangePhoto.setOnClickListener {
            // TODO: Implement photo picker and upload
            Toast.makeText(this, "Photo upload feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Save button
        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }
    
    private fun populateForm(student: com.example.playbright.data.model.StudentResponse) {
        // Personal Information
        binding.etFirstName.setText(student.firstName)
        binding.etLastName.setText(student.lastName)
        binding.etEmail.setText(student.email)
        binding.etStudentNumber.setText(student.studentNumber)
        binding.etBirthday.setText(student.birthday)
        
        // Profile Photo
        selectedPhotoUrl = student.photoUrl
        if (student.photoUrl != null && student.photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(student.photoUrl)
                .placeholder(R.drawable.ic_playbright_logo_clean)
                .error(R.drawable.ic_playbright_logo_clean)
                .circleCrop()
                .into(binding.ivProfilePhoto)
        }
        
        // Parent Information
        student.parent?.let { parent ->
            binding.etParentFirstName.setText(parent.firstName)
            binding.etParentLastName.setText(parent.lastName)
            binding.etParentEmail.setText(parent.email)
            binding.etParentContact.setText(parent.contactNumber)
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        var year = calendar.get(Calendar.YEAR)
        var month = calendar.get(Calendar.MONTH)
        var day = calendar.get(Calendar.DAY_OF_MONTH)
        
        // Try to parse existing date
        binding.etBirthday.text?.toString()?.let { dateString ->
            if (dateString.isNotEmpty()) {
                try {
                    val date = dateFormat.parse(dateString)
                    if (date != null) {
                        calendar.time = date
                        year = calendar.get(Calendar.YEAR)
                        month = calendar.get(Calendar.MONTH)
                        day = calendar.get(Calendar.DAY_OF_MONTH)
                    }
                } catch (e: Exception) {
                    // Use current date if parsing fails
                }
            }
        }
        
        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                binding.etBirthday.setText(dateFormat.format(calendar.time))
            },
            year,
            month,
            day
        ).show()
    }
    
    private fun saveProfile() {
        val firstName = binding.etFirstName.text?.toString()?.trim() ?: ""
        val lastName = binding.etLastName.text?.toString()?.trim() ?: ""
        val studentNumber = binding.etStudentNumber.text?.toString()?.trim() ?: ""
        val birthday = binding.etBirthday.text?.toString()?.trim() ?: ""
        val parentFirstName = binding.etParentFirstName.text?.toString()?.trim() ?: ""
        val parentLastName = binding.etParentLastName.text?.toString()?.trim() ?: ""
        val parentEmail = binding.etParentEmail.text?.toString()?.trim() ?: ""
        val parentContact = binding.etParentContact.text?.toString()?.trim() ?: ""
        
        // Validation
        if (firstName.isEmpty()) {
            binding.etFirstName.error = "First name is required"
            return
        }
        
        if (lastName.isEmpty()) {
            binding.etLastName.error = "Last name is required"
            return
        }
        
        if (studentNumber.isEmpty()) {
            binding.etStudentNumber.error = "Student number is required"
            return
        }
        
        if (birthday.isEmpty()) {
            binding.etBirthday.error = "Birthday is required"
            return
        }
        
        if (parentFirstName.isEmpty()) {
            binding.etParentFirstName.error = "Parent first name is required"
            return
        }
        
        if (parentLastName.isEmpty()) {
            binding.etParentLastName.error = "Parent last name is required"
            return
        }
        
        if (parentEmail.isEmpty()) {
            binding.etParentEmail.error = "Parent email is required"
            return
        }
        
        if (parentContact.isEmpty()) {
            binding.etParentContact.error = "Parent contact number is required"
            return
        }
        
        // Update profile
        viewModel.updateStudentProfile(
            firstName = firstName,
            lastName = lastName,
            birthday = birthday,
            studentNumber = studentNumber,
            photoUrl = selectedPhotoUrl,
            parentFirstName = parentFirstName,
            parentLastName = parentLastName,
            parentEmail = parentEmail,
            parentContact = parentContact
        )
    }
}

