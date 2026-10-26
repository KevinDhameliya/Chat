package com.kevin.chatapplication.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.kevin.chatapplication.R
import com.kevin.chatapplication.adapters.ChatListAdapter
import com.kevin.chatapplication.databinding.FragmentChatsBinding
import com.kevin.chatapplication.viewmodel.chatsViewModel.ChatFilter
import com.kevin.chatapplication.viewmodel.chatsViewModel.ChatsViewModel

class ChatsFragment : Fragment() {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ChatListAdapter(viewModel.currentUid) { chat ->
            findNavController().navigate(
                ChatsFragmentDirections.actionChatsFragmentToChatFragment(
                    otherUid = chat.otherUid,
                    otherName = chat.displayName,
                    chatId = chat.id,
                    isGroup = chat.isGroup
                )
            )
        }
        binding.rvChats.adapter = adapter

        binding.etSearch.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }

        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipUnread -> ChatFilter.UNREAD
                R.id.chipGroups -> ChatFilter.GROUPS
                R.id.chipFavorites -> ChatFilter.FAVORITES
                else -> ChatFilter.ALL
            }
            viewModel.setFilter(filter)
        }

        val openNewChat = View.OnClickListener {
            findNavController().navigate(R.id.action_chatsFragment_to_newChatFragment)
        }
        binding.fabNewChat.setOnClickListener(openNewChat)
        binding.btnSearch.setOnClickListener {
            findNavController().navigate(R.id.action_chatsFragment_to_searchFragment)
        }
        binding.btnStartChat.setOnClickListener(openNewChat)

        viewModel.chats.observe(viewLifecycleOwner) { chats ->
            adapter.submitList(chats)
            updateEmptyState(chats.isEmpty())
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progress.isVisible = loading
            updateEmptyState(viewModel.chats.value.isNullOrEmpty())
        }

        viewModel.error.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.errorShown()
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        val loading = viewModel.loading.value == true
        binding.emptyState.isVisible = isEmpty && !loading
        val noChatsAtAll = viewModel.hasNoChatsAtAll
        binding.tvEmptyTitle.setText(
            if (noChatsAtAll) R.string.empty_chats_title else R.string.empty_filter_title
        )
        binding.tvEmptySubtitle.setText(
            if (noChatsAtAll) R.string.empty_chats_subtitle else R.string.empty_filter_subtitle
        )
        binding.btnStartChat.isVisible = noChatsAtAll
        binding.fabNewChat.isVisible = !(isEmpty && noChatsAtAll && !loading)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
