package com.kevin.chatapplication.adapters

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kevin.chatapplication.R
import com.kevin.chatapplication.data.model.Message
import com.kevin.chatapplication.databinding.ItemMessageReceivedBinding
import com.kevin.chatapplication.databinding.ItemMessageSentBinding
import com.kevin.chatapplication.utils.AvatarUtils
import com.kevin.chatapplication.utils.TimeUtils

class MessageAdapter(
    private val myUid: String,
    /** Group chats show who wrote each received message. */
    private val showSenderNames: Boolean,
    private val onLongPress: (Message, View) -> Unit,
    private val onImageClick: (Message) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).senderId == myUid) TYPE_SENT else TYPE_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SENT) {
            SentViewHolder(ItemMessageSentBinding.inflate(inflater, parent, false))
        } else {
            ReceivedViewHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentViewHolder -> holder.bind(message)
            is ReceivedViewHolder -> holder.bind(message)
        }
    }

    /** Views shared by both bubble layouts. */
    private class BubbleViews(
        val bubble: LinearLayout,
        val replyQuote: LinearLayout,
        val tvReplySender: TextView,
        val tvReplyText: TextView,
        val ivImage: ImageView,
        val tvMessage: TextView,
        val tvTime: TextView,
        val tvReactions: TextView
    )

    private fun bindCommon(views: BubbleViews, message: Message) = with(views) {
        tvTime.text = TimeUtils.formatTime(message.timestamp)

        // Deleted placeholder
        if (message.deleted) {
            replyQuote.isVisible = false
            ivImage.isVisible = false
            tvMessage.isVisible = true
            tvMessage.text = "🚫 ${Message.DELETED_TEXT}"
            tvMessage.setTypeface(null, Typeface.ITALIC)
            tvMessage.setTextColor(ContextCompat.getColor(tvMessage.context, R.color.text_secondary))
            tvReactions.isVisible = false
            bubble.setOnLongClickListener(null)
            return@with
        }

        tvMessage.setTypeface(null, Typeface.NORMAL)
        tvMessage.setTextColor(ContextCompat.getColor(tvMessage.context, R.color.text_primary))

        // Reply quote
        replyQuote.isVisible = message.hasReply
        if (message.hasReply) {
            tvReplySender.text = message.replyToSender
            tvReplyText.text = message.replyToText
        }

        // Photo
        ivImage.isVisible = message.isImage
        if (message.isImage) {
            ivImage.load(message.imageUrl) { crossfade(true) }
            ivImage.setOnClickListener { onImageClick(message) }
            ivImage.setOnLongClickListener { onLongPress(message, bubble); true }
        } else {
            ivImage.setImageDrawable(null)
            ivImage.setOnClickListener(null)
        }

        // Text (photos can have none)
        tvMessage.isVisible = message.text.isNotBlank()
        tvMessage.text = message.text

        // Reactions: "❤️👍 2"
        tvReactions.isVisible = message.reactions.isNotEmpty()
        if (message.reactions.isNotEmpty()) {
            val emojis = message.reactions.values.distinct().joinToString("")
            val count = message.reactions.size
            tvReactions.text = if (count > 1) "$emojis $count" else emojis
        }

        bubble.setOnLongClickListener { onLongPress(message, bubble); true }
    }

    inner class SentViewHolder(private val binding: ItemMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) = with(binding) {
            bindCommon(
                BubbleViews(
                    bubble, replyQuote, tvReplySender, tvReplyText,
                    ivImage, tvMessage, tvTime, tvReactions
                ),
                message
            )

            ivStatus.isVisible = !message.deleted
            val isRead = message.status == Message.STATUS_READ
            ivStatus.setImageResource(if (isRead) R.drawable.ic_double_tick else R.drawable.ic_tick)
            ImageViewCompat.setImageTintList(
                ivStatus,
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        root.context,
                        if (isRead) R.color.tick_read else R.color.tick_sent
                    )
                )
            )
        }
    }

    inner class ReceivedViewHolder(private val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) = with(binding) {
            bindCommon(
                BubbleViews(
                    bubble, replyQuote, tvReplySender, tvReplyText,
                    ivImage, tvMessage, tvTime, tvReactions
                ),
                message
            )

            val showName = showSenderNames && message.senderName.isNotBlank()
            tvSender.isVisible = showName
            if (showName) {
                tvSender.text = message.senderName
                tvSender.setTextColor(AvatarUtils.colorFor(message.senderName))
            }
        }
    }

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2

        private val DIFF = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
        }
    }
}
