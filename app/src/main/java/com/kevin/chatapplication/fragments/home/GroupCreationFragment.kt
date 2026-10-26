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
import com.kevin.chatapplication.adapters.SelectContactAdapter
import com.kevin.chatapplication.databinding.FragmentGroupCreationBinding
import com.kevin.chatapplication.viewmodel.groupCreationViewModel.GroupCreationState
import com.kevin.chatapplication.viewmodel.groupCreationViewModel.GroupCreationViewModel

class GroupCreationFragment : Fragment() {

    private var _binding: FragmentGroupCreationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GroupCreationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupCreationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnGroupIcon.setOnClickListener {
            Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }

        val adapter = SelectContactAdapter { user -> viewModel.toggle(user) }
        binding.rvContacts.adapter = adapter

        binding.etSearch.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }
        binding.etGroupName.doAfterTextChanged {
            binding.tilGroupName.error = null
            updateCreateButton()
        }

        binding.btnCreate.setOnClickListener {
            val name = binding.etGroupName.text?.toString()?.trim().orEmpty()
            when {
                name.isEmpty() -> binding.tilGroupName.error = getString(R.string.group_name_required)
                viewModel.selected.value.isNullOrEmpty() ->
                    Toast.makeText(requireContext(), R.string.group_members_required, Toast.LENGTH_SHORT).show()
                else -> viewModel.create(name)
            }
        }

        viewModel.contacts.observe(viewLifecycleOwner) { contacts ->
            binding.progress.isVisible = false
            adapter.submitList(contacts)
            binding.tvEmpty.isVisible = contacts.isEmpty()
        }

        viewModel.selected.observe(viewLifecycleOwner) { selected ->
            adapter.selectedIds = selected
            binding.tvSelectedCount.text =
                if (selected.isEmpty()) "" else getString(R.string.members_selected, selected.size)
            updateCreateButton()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            val creating = state == GroupCreationState.Creating
            binding.progressCreate.isVisible = creating
            binding.btnCreate.visibility = if (creating) View.INVISIBLE else View.VISIBLE

            when (state) {
                is GroupCreationState.Created -> {
                    viewModel.stateHandled()
                    findNavController().navigate(
                        GroupCreationFragmentDirections.actionGroupCreationFragmentToChatFragment(
                            otherUid = "",
                            otherName = state.name,
                            chatId = state.chatId,
                            isGroup = true
                        )
                    )
                }
                is GroupCreationState.Error -> {
                    viewModel.stateHandled()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.group_create_failed, state.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> Unit
            }
        }
    }

    private fun updateCreateButton() {
        val b = _binding ?: return
        val hasName = !b.etGroupName.text.isNullOrBlank()
        val hasMembers = !viewModel.selected.value.isNullOrEmpty()
        b.btnCreate.isEnabled = hasName && hasMembers
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
