package com.kevin.chatapplication.fragments.home

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.kevin.chatapplication.R
import com.kevin.chatapplication.adapters.SearchAdapter
import com.kevin.chatapplication.data.model.SearchItem
import com.kevin.chatapplication.databinding.FragmentSearchBinding
import com.kevin.chatapplication.viewmodel.searchViewModel.SearchTab
import com.kevin.chatapplication.viewmodel.searchViewModel.SearchViewModel

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: SearchAdapter

    private val debounceHandler = Handler(Looper.getMainLooper())
    private var pendingSearch: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SearchAdapter(viewModel.myUid) { item -> openChat(item) }
        binding.rvResults.adapter = adapter

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnClear.setOnClickListener { binding.etQuery.text?.clear() }

        binding.etQuery.doAfterTextChanged { text ->
            val query = text?.toString().orEmpty()
            binding.btnClear.isVisible = query.isNotEmpty()
            pendingSearch?.let { debounceHandler.removeCallbacks(it) }
            pendingSearch = Runnable { viewModel.setQuery(query) }.also {
                debounceHandler.postDelayed(it, 250)
            }
        }

        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            viewModel.setTab(
                when (checkedIds.firstOrNull()) {
                    R.id.chipContacts -> SearchTab.CONTACTS
                    R.id.chipGroups -> SearchTab.GROUPS
                    else -> SearchTab.MESSAGES
                }
            )
        }

        viewModel.results.observe(viewLifecycleOwner) { results ->
            adapter.query = viewModel.query
            adapter.submitList(results)
            updateEmptyState()
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progress.isVisible = loading
            updateEmptyState()
        }

        // Open the keyboard straight away.
        binding.etQuery.requestFocus()
        binding.etQuery.post {
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(_binding?.etQuery, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun updateEmptyState() {
        val b = _binding ?: return
        val loading = viewModel.loading.value == true
        val empty = viewModel.results.value.isNullOrEmpty()
        val query = viewModel.query
        b.tvEmpty.isVisible = empty && !loading
        b.tvEmpty.text = when {
            query.isBlank() -> getString(R.string.search_start)
            else -> getString(R.string.search_no_results, query)
        }
    }

    private fun openChat(item: SearchItem) {
        val directions = when (item) {
            is SearchItem.MessageHit -> SearchFragmentDirections.actionSearchFragmentToChatFragment(
                otherUid = item.chat.otherUid,
                otherName = item.chat.displayName,
                chatId = item.chat.id,
                isGroup = item.chat.isGroup
            )
            is SearchItem.GroupHit -> SearchFragmentDirections.actionSearchFragmentToChatFragment(
                otherUid = "",
                otherName = item.chat.displayName,
                chatId = item.chat.id,
                isGroup = true
            )
            is SearchItem.ContactHit -> SearchFragmentDirections.actionSearchFragmentToChatFragment(
                otherUid = item.user.uid,
                otherName = item.user.name
            )
        }
        findNavController().navigate(directions)
    }

    override fun onDestroyView() {
        pendingSearch?.let { debounceHandler.removeCallbacks(it) }
        super.onDestroyView()
        _binding = null
    }
}
