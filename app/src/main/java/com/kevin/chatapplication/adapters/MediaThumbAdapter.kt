package com.kevin.chatapplication.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kevin.chatapplication.data.model.Message
import com.kevin.chatapplication.databinding.ItemMediaThumbBinding

/** Small square photo thumbnails (Contact Profile strip and Media Viewer bottom bar). */
class MediaThumbAdapter(
    private val sizeDp: Int,
    private val onClick: (position: Int, message: Message) -> Unit
) : ListAdapter<Message, MediaThumbAdapter.ThumbViewHolder>(DIFF) {

    var selectedPosition: Int = RecyclerView.NO_POSITION
        set(value) {
            val old = field
            field = value
            if (old != RecyclerView.NO_POSITION) notifyItemChanged(old)
            if (value != RecyclerView.NO_POSITION) notifyItemChanged(value)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbViewHolder {
        val binding = ItemMediaThumbBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val px = (sizeDp * parent.resources.displayMetrics.density).toInt()
        binding.ivThumb.layoutParams = binding.ivThumb.layoutParams.apply {
            width = px
            height = px
        }
        return ThumbViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThumbViewHolder, position: Int) =
        holder.bind(getItem(position), position == selectedPosition)

    inner class ThumbViewHolder(private val binding: ItemMediaThumbBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message, selected: Boolean) = with(binding) {
            ivThumb.load(message.imageUrl) { crossfade(true) }
            selection.isVisible = selected
            root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onClick(position, message)
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Message, newItem: Message) =
                oldItem.imageUrl == newItem.imageUrl
        }
    }
}
