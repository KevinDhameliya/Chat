package com.kevin.chatapplication.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.databinding.ItemSelectContactBinding
import com.kevin.chatapplication.utils.AvatarUtils

/** Contact rows with a checkbox (Group Creation). */
class SelectContactAdapter(
    private val onToggle: (User) -> Unit
) : ListAdapter<User, SelectContactAdapter.SelectViewHolder>(DIFF) {

    var selectedIds: Set<String> = emptySet()
        set(value) {
            field = value
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectViewHolder =
        SelectViewHolder(
            ItemSelectContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: SelectViewHolder, position: Int) =
        holder.bind(getItem(position))

    override fun onBindViewHolder(holder: SelectViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            holder.bindSelection(getItem(position))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class SelectViewHolder(private val binding: ItemSelectContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) = with(binding) {
            AvatarUtils.bind(tvAvatar, ivPhoto, user.name, user.photoUrl)
            tvName.text = user.name
            tvAbout.text = user.about
            root.setOnClickListener { onToggle(user) }
            bindSelection(user)
        }

        fun bindSelection(user: User) {
            binding.checkbox.isChecked = user.uid in selectedIds
        }
    }

    companion object {
        private const val PAYLOAD_SELECTION = "selection"

        private val DIFF = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.uid == newItem.uid
            override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
        }
    }
}
