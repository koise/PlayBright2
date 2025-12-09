package com.example.playbright.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.playbright.R
import com.example.playbright.data.model.ModuleResponse
import com.example.playbright.data.model.ProgressData
import com.example.playbright.databinding.ItemModuleCardBinding

class ModuleAdapter(
    private val onModuleClick: (ModuleResponse) -> Unit,
    private var progressMap: Map<String, ProgressData> = emptyMap()
) : ListAdapter<ModuleResponse, ModuleAdapter.ModuleViewHolder>(ModuleDiffCallback()) {
    
    fun updateProgress(newProgressMap: Map<String, ProgressData>) {
        progressMap = newProgressMap
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val binding = ItemModuleCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ModuleViewHolder(binding, onModuleClick, progressMap)
    }
    
    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        holder.bind(getItem(position), progressMap)
    }
    
    class ModuleViewHolder(
        private val binding: ItemModuleCardBinding,
        private val onModuleClick: (ModuleResponse) -> Unit,
        private val progressMap: Map<String, ProgressData>
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(module: ModuleResponse, progressMap: Map<String, ProgressData>) {
            // Get moduleId - try direct field first, then derive from moduleNumber
            val moduleId = when {
                !module.moduleId.isNullOrEmpty() -> module.moduleId
                module.moduleNumber != null -> when (module.moduleNumber) {
                    1 -> "picture-quiz"
                    2 -> "audio-identification"
                    3 -> "sequencing-cards"
                    4 -> "word-builder"
                    5 -> "picture-label"
                    6 -> "emotion-recognition"
                    7 -> "category-selection"
                    8 -> "yes-no-questions"
                    9 -> "tap-repeat"
                    10 -> "matching-pairs"
                    11 -> "trace-follow"
                    else -> module.moduleId ?: ""
                }
                else -> module.moduleId ?: ""
            }
            
            // Normalize moduleId (handle aliases)
            val normalizedModuleId = when (moduleId) {
                "picture-labeling" -> "picture-label"
                else -> moduleId
            }
            
            // Use creative, engaging names for SPED students
            val creativeName = when (normalizedModuleId) {
                "picture-quiz" -> "Choose The Right One"
                "audio-identification" -> "Sound Detective Mission"
                "sequencing-cards" -> "Story Sequence Quest"
                "picture-label" -> "Tap to Hear Words"
                "emotion-recognition" -> "Feelings Finder Game"
                "category-selection" -> "Category Collector Challenge"
                "word-builder" -> "Word Builder Challenge"
                "yes-no-questions" -> "Yes or No Quick Quiz"
                "tap-repeat" -> "Speak & Learn Practice"
                "matching-pairs" -> "Memory Match Master"
                "trace-follow" -> "Trace & Follow Practice"
                else -> module.name ?: "Learning Adventure"
            }
            binding.tvModuleName.text = creativeName
            
            // Set description or default based on module type
            val description = when (normalizedModuleId) {
                "picture-quiz" -> "Discover words from 4 pictures!"
                "audio-identification" -> "Listen and match sounds!"
                "sequencing-cards" -> "Put stories in order!"
                "picture-label" -> "Tap to hear words!"
                "emotion-recognition" -> "Find the emotions!"
                "category-selection" -> "Group items together!"
                "word-builder" -> "Build amazing words!"
                "yes-no-questions" -> "Answer Yes or No!"
                "tap-repeat" -> "Listen and repeat!"
                "matching-pairs" -> "Match the pairs!"
                "trace-follow" -> "Trace lines and shapes!"
                else -> module.description ?: "Tap to start your adventure!"
            }
            binding.tvModuleDescription.text = description
            
            // Set icon based on module type - using custom SPED-friendly icons
            val iconRes = when (normalizedModuleId) {
                "picture-quiz" -> R.drawable.ic_module_picture_quiz
                "audio-identification" -> R.drawable.ic_module_audio
                "sequencing-cards" -> R.drawable.ic_module_sequencing
                "picture-label" -> R.drawable.ic_module_picture_label
                "word-builder" -> R.drawable.ic_module_word_builder
                "emotion-recognition" -> R.drawable.ic_module_emotion
                "category-selection" -> R.drawable.ic_module_category
                "yes-no-questions" -> R.drawable.ic_module_yesno
                "tap-repeat" -> R.drawable.ic_module_tap_repeat
                "matching-pairs" -> R.drawable.ic_module_matching
                "trace-follow" -> R.drawable.ic_module_trace_follow
                else -> R.drawable.ic_playbright_logo_clean
            }
            
            binding.ivModuleIcon.setImageResource(iconRes)
            
            // Get progress data for this module (try both normalized and original moduleId)
            val progress = progressMap[normalizedModuleId] ?: progressMap[moduleId]
            val isCompleted = progress?.completed ?: false
            val totalQuestions = progress?.totalQuestions ?: 0
            val currentQuestion = progress?.currentQuestion ?: 0
            val progressPercentage = if (totalQuestions > 0) {
                ((currentQuestion.toFloat() / totalQuestions.toFloat()) * 100).toInt()
            } else 0
            
            // Show completion badge if completed
            binding.ivCompletedBadge.visibility = if (isCompleted) View.VISIBLE else View.GONE
            
            // Show progress bar and info if there's progress
            if (progress != null && totalQuestions > 0) {
                binding.progressBarModule.visibility = View.VISIBLE
                binding.llProgressInfo.visibility = View.VISIBLE
                binding.progressBarModule.progress = progressPercentage
                binding.tvProgressText.text = "$progressPercentage%"
            } else {
                binding.progressBarModule.visibility = View.GONE
                binding.llProgressInfo.visibility = View.GONE
            }
            
            // Set card background color based on module type (pastel colors)
            val cardColor = when (normalizedModuleId) {
                "picture-quiz" -> R.color.card_orange
                "audio-identification" -> R.color.card_blue
                "sequencing-cards" -> R.color.card_purple
                "picture-label" -> R.color.card_blue
                "emotion-recognition" -> R.color.card_purple
                "category-selection" -> R.color.card_green
                "word-builder" -> R.color.card_green
                "yes-no-questions" -> R.color.card_orange
                "tap-repeat" -> R.color.card_blue
                "matching-pairs" -> R.color.card_purple
                "trace-follow" -> R.color.card_orange
                else -> R.color.card_default
            }
            binding.root.setCardBackgroundColor(itemView.context.getColor(cardColor))
            
            // Allow clicking the entire card
            binding.root.setOnClickListener {
                onModuleClick(module)
            }
        }
    }
    
    class ModuleDiffCallback : DiffUtil.ItemCallback<ModuleResponse>() {
        override fun areItemsTheSame(oldItem: ModuleResponse, newItem: ModuleResponse): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ModuleResponse, newItem: ModuleResponse): Boolean {
            return oldItem == newItem
        }
    }
}

