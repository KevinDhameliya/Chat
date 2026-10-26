package com.kevin.chatapplication.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.content.res.ColorStateList
import com.kevin.chatapplication.R
import com.kevin.chatapplication.databinding.ItemCallBinding
import com.kevin.chatapplication.utils.AvatarUtils
import com.kevin.chatapplication.utils.TimeUtils
import com.kevin.chatapplication.viewmodel.callsViewModel.CallRow

class CallLogAdapter(
    private val onClick: (CallRow) -> Unit,
    private val onCallBack: (CallRow) -> Unit,
    private val onLongPress: (CallRow) -> Unit
) : ListAdapter<CallRow, CallLogAdapter.CallViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder =
        CallViewHolder(ItemCallBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: CallViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class CallViewHolder(private val binding: ItemCallBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(row: CallRow) = with(binding) {
            val context = root.context
            AvatarUtils.bind(tvAvatar, ivPhoto, row.otherName, row.photoUrl)
            tvName.text = row.otherName

            // ↗ Outgoing (teal) · ↙ Incoming (teal) · ✕ Missed (red)
            val (icon, label, colorRes) = when {
                row.outgoing -> Triple(R.drawable.ic_call_made, R.string.call_outgoing, R.color.brand_primary)
                row.missed -> Triple(R.drawable.ic_call_missed, R.string.call_missed, R.color.danger)
                else -> Triple(R.drawable.ic_call_received, R.string.call_incoming, R.color.brand_primary)
            }
            ivDirection.setImageResource(icon)
            ImageViewCompat.setImageTintList(
                ivDirection,
                ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
            )
            tvSubtitle.text = context.getString(
                R.string.call_subtitle,
                context.getString(label),
                TimeUtils.formatChatListTime(row.log.startedAt)
            )
            tvSubtitle.setTextColor(
                ContextCompat.getColor(context, if (row.missed) R.color.danger else R.color.text_secondary)
            )
            tvName.setTextColor(
                ContextCompat.getColor(context, if (row.missed) R.color.danger else R.color.text_primary)
            )

            // Call back with the same type.
            btnCallBack.setImageResource(if (row.log.isVideo) R.drawable.ic_videocam else R.drawable.ic_call)
            btnCallBack.contentDescription = context.getString(
                if (row.log.isVideo) R.string.call_back_video else R.string.call_back_voice
            )
            btnCallBack.setOnClickListener { onCallBack(row) }

            root.setOnClickListener { onClick(row) }
            root.setOnLongClickListener { onLongPress(row); true }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CallRow>() {
            override fun areItemsTheSame(oldItem: CallRow, newItem: CallRow) = oldItem.log.id == newItem.log.id
            override fun areContentsTheSame(oldItem: CallRow, newItem: CallRow) = oldItem == newItem
        }
    }
}
