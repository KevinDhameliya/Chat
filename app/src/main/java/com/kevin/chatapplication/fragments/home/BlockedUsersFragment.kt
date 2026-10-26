package com.kevin.chatapplication.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.kevin.chatapplication.R
import com.kevin.chatapplication.adapters.BlockedUserAdapter
import com.kevin.chatapplication.databinding.FragmentBlockedUsersBinding
import com.kevin.chatapplication.viewmodel.blockedUsersViewModel.BlockedUsersViewModel

class BlockedUsersFragment : Fragment() {

    private var _binding: FragmentBlockedUsersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BlockedUsersViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBlockedUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        val adapter = BlockedUserAdapter { user ->
            viewModel.unblock(user)
            Toast.makeText(
                requireContext(),
                getString(R.string.unblocked, user.name),
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.rvBlocked.adapter = adapter

        viewModel.blocked.observe(viewLifecycleOwner) { users ->
            binding.progress.isVisible = false
            adapter.submitList(users)
            binding.emptyState.isVisible = users.isEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
