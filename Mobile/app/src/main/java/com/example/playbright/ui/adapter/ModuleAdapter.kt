package com.example.playbright.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.playbright.R
import com.example.playbright.data.model.ModuleResponse
import com.example.playbright.databinding.ItemModuleCardBinding

class ModuleAdapter(
    private val onModuleClick: (ModuleResponse) -> Unit
) : ListAdapter<ModuleResponse, ModuleAdapter.ModuleViewHolder>(ModuleDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val binding = ItemModuleCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ModuleViewHolder(binding, onModuleClick)
    }
    
    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ModuleViewHolder(
        private val binding: ItemModuleCardBinding,
        private val onModuleClick: (ModuleResponse) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(module: ModuleResponse) {
            // Use creative, engaging names for SPED students
            val moduleId = module.moduleId ?: ""
            val creativeName = when (moduleId) {
                "picture-quiz" -> "Choose The Right One"
                "audio-identification" -> "Sound Detective Mission"
                "sequencing-cards" -> "Story Sequence Quest"
                "word-builder" -> "Word Builder Challenge"
                "picture-label" -> "Picture Talk & Learn"
                "emotion-recognition" -> "Feelings Finder Game"
                "category-selection" -> "Category Collector Challenge"
                "yes-no-questions" -> "Yes or No Quick Quiz"
                "tap-repeat" -> "Speak & Learn Practice"
                "matching-pairs" -> "Memory Match Master"
                else -> module.name ?: "Learning Adventure"
            }
            binding.tvModuleName.text = creativeName
            
            // Set description or default based on module type
            val description = when (moduleId) {
                "picture-quiz" -> "Discover words from 4 pictures!"
                "audio-identification" -> "Listen and match sounds!"
                "sequencing-cards" -> "Put stories in order!"
                "word-builder" -> "Build amazing words!"
                "picture-label" -> "Tap to hear words!"
                "emotion-recognition" -> "Find the emotions!"
                "category-selection" -> "Group items together!"
                "yes-no-questions" -> "Answer Yes or No!"
                "tap-repeat" -> "Listen and repeat!"
                "matching-pairs" -> "Match the pairs!"
                else -> module.description ?: "Tap to start your adventure!"
            }
            binding.tvModuleDescription.text = description
            
            // Set icon based on module type
            val iconRes = when (moduleId) {
                "picture-quiz" -> android.R.drawable.ic_menu_gallery
                "audio-identification" -> android.R.drawable.ic_media_play
                "sequencing-cards" -> android.R.drawable.ic_menu_sort_by_size
                "word-builder" -> android.R.drawable.ic_menu_sort_alphabetically
                "picture-label" -> android.R.drawable.ic_menu_camera
                "emotion-recognition" -> android.R.drawable.ic_menu_help
                "category-selection" -> android.R.drawable.ic_menu_view
                "yes-no-questions" -> android.R.drawable.ic_menu_help
                "tap-repeat" -> android.R.drawable.ic_menu_myplaces
                "matching-pairs" -> android.R.drawable.ic_menu_compass
                else -> android.R.drawable.ic_menu_help
            }
            
            binding.ivModuleIcon.setImageResource(iconRes)
            
            // Set card background color based on module type (pastel colors)
            val cardColor = when (moduleId) {
                "picture-quiz" -> R.color.card_orange
                "audio-identification" -> R.color.card_blue
                "sequencing-cards" -> R.color.card_purple
                "word-builder" -> R.color.card_green
                "picture-label" -> R.color.card_blue
                "emotion-recognition" -> R.color.card_purple
                "category-selection" -> R.color.card_green
                "yes-no-questions" -> R.color.card_orange
                "tap-repeat" -> R.color.card_blue
                "matching-pairs" -> R.color.card_purple
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

