package com.example.playbright.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.playbright.databinding.ItemSyllableButtonBinding

class SyllableAdapter(
    private val syllables: List<String>,
    private val onSyllableClick: (String) -> Unit
) : RecyclerView.Adapter<SyllableAdapter.SyllableViewHolder>() {

    private val usedSyllables = mutableSetOf<Int>()

    fun resetUsedSyllables() {
        usedSyllables.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SyllableViewHolder {
        val binding = ItemSyllableButtonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SyllableViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SyllableViewHolder, position: Int) {
        holder.bind(syllables[position], position)
    }

    override fun getItemCount(): Int = syllables.size

    inner class SyllableViewHolder(
        private val binding: ItemSyllableButtonBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(syllable: String, position: Int) {
            binding.root.text = syllable
            binding.root.isEnabled = !usedSyllables.contains(position)
            
            if (usedSyllables.contains(position)) {
                binding.root.alpha = 0.5f
                binding.root.isClickable = false
            } else {
                binding.root.alpha = 1.0f
                binding.root.isClickable = true
            }

            binding.root.setOnClickListener {
                if (!usedSyllables.contains(position)) {
                    usedSyllables.add(position)
                    notifyItemChanged(position)
                    onSyllableClick(syllable)
                }
            }
        }
    }
}






