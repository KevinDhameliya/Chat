package com.kevin.chatapplication.adapters

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kevin.chatapplication.R
import com.kevin.chatapplication.data.model.SearchItem
import com.kevin.chatapplication.databinding.ItemSearchResultBinding
import com.kevin.chatapplication.utils.AvatarUtils
import com.kevin.chatapplication.utils.TimeUtils

class SearchAdapter(
    private val myUid: String,
    private val onClick: (SearchItem) -> Unit
) : ListAdapter<SearchItem, SearchAdapter.ResultViewHolder>(DIFF) {

    /** Text to highlight in results. */
    var query: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder =
        ResultViewHolder(
            ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class ResultViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SearchItem) = with(binding) {
            when (item) {
                is SearchItem.MessageHit -> {
                    AvatarUtils.bind(tvAvatar, item.chat.displayName)
                    tvTitle.text = item.chat.displayName
                    tvTime.isVisible = true
                    tvTime.text = TimeUtils.formatChatListTime(item.message.timestamp)
                    val prefix = when {
                        item.message.senderId == myUid -> "You: "
                        item.chat.isGroup && item.message.senderName.isNotBlank() ->
                            item.message.senderName.substringBefore(" ") + ": "
                        else -> ""
                    }
                    tvSubtitle.text = highlight(prefix, item.message.text)
                }

                is SearchItem.ContactHit -> {
                    AvatarUtils.bind(tvAvatar, item.user.name)
                    tvTitle.text = highlight("", item.user.name)
                    tvTime.isVisible = false
                    tvSubtitle.text = item.user.about
                }

                is SearchItem.GroupHit -> {
                    AvatarUtils.bind(tvAvatar, item.chat.displayName)
                    tvTitle.text = highlight("", item.chat.displayName)
                    tvTime.isVisible = false
                    tvSubtitle.text = itemView.context.getString(
                        R.string.group_members_count,
                        item.chat.participants.size
                    )
                }
            }
            root.setOnClickListener { onClick(item) }
        }

        /** Bold + teal for the matched part; long texts are trimmed around the match. */
        private fun highlight(prefix: String, text: String): CharSequence {
            val index = text.indexOf(query, ignoreCase = true)
            if (query.isBlank() || index < 0) return prefix + text

            val start = if (index > 40) index - 20 else 0
            val shown = (if (start > 0) "…" else "") + text.substring(start)
            val matchStart = prefix.length + shown.indexOf(query, ignoreCase = true)
            val spannable = SpannableString(prefix + shown)
            if (matchStart >= prefix.length) {
                val matchEnd = matchStart + query.length
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD), matchStart, matchEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(itemView.context, R.color.brand_primary)),
                    matchStart, matchEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannable
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SearchItem>() {
            override fun areItemsTheSame(oldItem: SearchItem, newItem: SearchItem): Boolean =
                when {
                    oldItem is SearchItem.MessageHit && newItem is SearchItem.MessageHit ->
                        oldItem.message.id == newItem.message.id
                    oldItem is SearchItem.ContactHit && newItem is SearchItem.ContactHit ->
                        oldItem.user.uid == newItem.user.uid
                    oldItem is SearchItem.GroupHit && newItem is SearchItem.GroupHit ->
                        oldItem.chat.id == newItem.chat.id
                    else -> false
                }

            override fun areContentsTheSame(oldItem: SearchItem, newItem: SearchItem) =
                oldItem == newItem
        }
    }
}
