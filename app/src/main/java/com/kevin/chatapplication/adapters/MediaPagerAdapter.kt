package com.kevin.chatapplication.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kevin.chatapplication.data.model.Message
import com.kevin.chatapplication.databinding.ItemMediaPageBinding

/** Full-screen pages for the Media Viewer. */
class MediaPagerAdapter(
    private val onTap: () -> Unit
) : ListAdapter<Message, MediaPagerAdapter.PageViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder =
        PageViewHolder(ItemMediaPageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class PageViewHolder(private val binding: ItemMediaPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.ivPhoto.load(message.imageUrl) { crossfade(true) }
            binding.ivPhoto.setOnClickListener { onTap() }
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
