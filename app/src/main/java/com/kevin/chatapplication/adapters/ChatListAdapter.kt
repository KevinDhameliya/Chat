package com.kevin.chatapplication.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kevin.chatapplication.R
import com.kevin.chatapplication.data.model.Chat
import com.kevin.chatapplication.databinding.ItemChatBinding
import com.kevin.chatapplication.utils.AvatarUtils
import com.kevin.chatapplication.utils.TimeUtils

class ChatListAdapter(
    private val myUid: String,
    private val onClick: (Chat) -> Unit
) : ListAdapter<Chat, ChatListAdapter.ChatViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder =
        ChatViewHolder(
            ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class ChatViewHolder(private val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: Chat) = with(binding) {
            val context = root.context
            AvatarUtils.bind(tvAvatar, chat.displayName)
            tvName.text = chat.displayName
            // Groups: "Sarah: See you soon" (not for my own messages or system lines)
            val showSender = chat.isGroup && chat.lastSenderId.isNotEmpty() &&
                chat.lastSenderId != myUid && chat.lastSenderName.isNotBlank()
            tvLastMessage.text =
                if (showSender) "${chat.lastSenderName.substringBefore(" ")}: ${chat.lastMessage}"
                else chat.lastMessage
            tvTime.text = TimeUtils.formatChatListTime(chat.lastMessageTime)

            // Tick before my own last message.
            ivTick.isVisible = chat.lastSenderId == myUid

            val hasUnread = chat.unreadCount > 0
            tvUnread.isVisible = hasUnread
            tvUnread.text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString()
            tvTime.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (hasUnread) R.color.brand_primary else R.color.text_secondary
                )
            )

            root.setOnClickListener { onClick(chat) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Chat>() {
            override fun areItemsTheSame(oldItem: Chat, newItem: Chat) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Chat, newItem: Chat) = oldItem == newItem
        }
    }
}
