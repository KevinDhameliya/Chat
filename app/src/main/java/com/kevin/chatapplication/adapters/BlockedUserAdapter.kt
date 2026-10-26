package com.kevin.chatapplication.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.databinding.ItemBlockedUserBinding
import com.kevin.chatapplication.utils.AvatarUtils

class BlockedUserAdapter(
    private val onUnblock: (User) -> Unit
) : ListAdapter<User, BlockedUserAdapter.BlockedViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockedViewHolder =
        BlockedViewHolder(
            ItemBlockedUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: BlockedViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class BlockedViewHolder(private val binding: ItemBlockedUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) = with(binding) {
            AvatarUtils.bind(tvAvatar, ivPhoto, user.name, user.photoUrl)
            tvName.text = user.name
            tvAbout.text = user.about
            btnUnblock.setOnClickListener { onUnblock(user) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.uid == newItem.uid
            override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
        }
    }
}
