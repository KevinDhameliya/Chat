package com.kevin.chatapplication.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kevin.chatapplication.R
import com.kevin.chatapplication.data.model.UserStatus
import com.kevin.chatapplication.databinding.ItemStatusBinding
import com.kevin.chatapplication.utils.AvatarUtils
import com.kevin.chatapplication.utils.TimeUtils

class StatusAdapter(
    private val onClick: (UserStatus) -> Unit
) : ListAdapter<UserStatus, StatusAdapter.StatusViewHolder>(DIFF) {

    /** Item ids I've already watched (decides teal vs grey ring). */
    var seenIds: Set<String> = emptySet()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder =
        StatusViewHolder(ItemStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class StatusViewHolder(private val binding: ItemStatusBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(status: UserStatus) = with(binding) {
            AvatarUtils.bind(tvAvatar, ivPhoto, status.name, status.photoUrl)
            tvName.text = status.name
            tvTime.text = TimeUtils.formatAgo(status.latest)
            val allSeen = status.items.all { it.id in seenIds }
            ring.setBackgroundResource(
                if (allSeen) R.drawable.bg_status_ring_seen else R.drawable.bg_status_ring_unseen
            )
            root.setOnClickListener { onClick(status) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<UserStatus>() {
            override fun areItemsTheSame(oldItem: UserStatus, newItem: UserStatus) = oldItem.uid == newItem.uid
            override fun areContentsTheSame(oldItem: UserStatus, newItem: UserStatus) = oldItem == newItem
        }
    }
}
