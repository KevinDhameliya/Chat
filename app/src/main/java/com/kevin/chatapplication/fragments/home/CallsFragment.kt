package com.kevin.chatapplication.fragments.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kevin.chatapplication.R
import com.kevin.chatapplication.adapters.CallLogAdapter
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.databinding.FragmentCallsBinding
import com.kevin.chatapplication.utils.CallLauncher
import com.kevin.chatapplication.viewmodel.callsViewModel.CallsViewModel

class CallsFragment : Fragment() {

    private var _binding: FragmentCallsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CallsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCallsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = CallLogAdapter(
            onClick = { row ->
                findNavController().navigate(
                    CallsFragmentDirections.actionCallsFragmentToContactProfileFragment(
                        otherUid = row.otherUid,
                        otherName = row.otherName
                    )
                )
            },
            onCallBack = { row ->
                CallLauncher.call(requireContext(), row.otherUid, row.otherName, row.log.isVideo)
            },
            onLongPress = { row ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.remove_from_history)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.remove) { _, _ -> viewModel.remove(row) }
                    .show()
            }
        )
        binding.rvCalls.adapter = adapter

        binding.btnSearch.setOnClickListener { toggleSearch() }
        binding.etSearch.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }

        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            viewModel.setMissedOnly(checkedIds.firstOrNull() == R.id.chipMissed)
        }

        binding.fabNewCall.setOnClickListener { pickContactToCall() }

        viewModel.rows.observe(viewLifecycleOwner) { rows ->
            adapter.submitList(rows)
            updateEmptyState(rows.isEmpty())
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progress.isVisible = loading
            updateEmptyState(viewModel.rows.value.isNullOrEmpty())
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        val b = _binding ?: return
        val loading = viewModel.loading.value == true
        b.emptyState.isVisible = isEmpty && !loading
        if (viewModel.isMissedFilter) {
            b.tvEmptyTitle.setText(R.string.calls_empty_missed)
            b.tvEmptyBody.isVisible = false
        } else {
            b.tvEmptyTitle.setText(R.string.calls_empty_title)
            b.tvEmptyBody.isVisible = true
        }
    }

    private fun toggleSearch() {
        val show = !binding.etSearch.isVisible
        binding.etSearch.isVisible = show
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (show) {
            binding.etSearch.requestFocus()
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        } else {
            binding.etSearch.text?.clear()
            imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        }
    }

    // ------------------------------------------------------------ new call

    private fun pickContactToCall() {
        val contacts = viewModel.contacts
        if (contacts.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_contacts, Toast.LENGTH_SHORT).show()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.new_call)
            .setItems(contacts.map { it.name }.toTypedArray()) { _, which ->
                chooseCallType(contacts[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun chooseCallType(contact: User) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.choose_call_type, contact.name))
            .setItems(arrayOf(getString(R.string.call_back_voice), getString(R.string.call_back_video))) { _, which ->
                CallLauncher.call(requireContext(), contact.uid, contact.name, video = which == 1)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
