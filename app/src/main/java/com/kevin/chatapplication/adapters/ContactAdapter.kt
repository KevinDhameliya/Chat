package com.kevin.chatapplication.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.databinding.ItemContactBinding
import com.kevin.chatapplication.utils.AvatarUtils
import com.kevin.chatapplication.utils.TimeUtils

class ContactAdapter(
    private val onClick: (User) -> Unit
) : ListAdapter<User, ContactAdapter.ContactViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder =
        ContactViewHolder(
            ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) = with(binding) {
            AvatarUtils.bind(tvAvatar, ivPhoto, user.name, user.photoUrl)
            tvName.text = user.name
            onlineDot.isVisible = user.online
            tvStatus.text = TimeUtils.formatPresence(user.online, user.lastSeen)
                .replaceFirstChar { it.uppercase() }
                .ifBlank { user.about }
            root.setOnClickListener { onClick(user) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.uid == newItem.uid
            override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
        }
    }
}
