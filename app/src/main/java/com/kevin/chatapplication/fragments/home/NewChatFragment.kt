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
import com.kevin.chatapplication.adapters.ContactAdapter
import com.kevin.chatapplication.databinding.FragmentNewChatBinding
import com.kevin.chatapplication.viewmodel.newChatViewModel.NewChatViewModel

class NewChatFragment : Fragment() {

    private var _binding: FragmentNewChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NewChatViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.rowNewGroup.setOnClickListener {
            findNavController().navigate(R.id.action_newChatFragment_to_groupCreationFragment)
        }

        val adapter = ContactAdapter { user ->
            findNavController().navigate(
                NewChatFragmentDirections.actionNewChatFragmentToChatFragment(
                    otherUid = user.uid,
                    otherName = user.name
                )
            )
        }
        binding.rvContacts.adapter = adapter

        binding.etSearch.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }

        viewModel.contacts.observe(viewLifecycleOwner) { contacts ->
            adapter.submitList(contacts)
            binding.tvEmpty.isVisible = contacts.isEmpty() && viewModel.loading.value == false
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progress.isVisible = loading
            binding.tvEmpty.isVisible = !loading && viewModel.contacts.value.isNullOrEmpty()
        }

        viewModel.error.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.errorShown()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
