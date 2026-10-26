package com.kevin.chatapplication.fragments.authentication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.kevin.chatapplication.R
import com.kevin.chatapplication.databinding.FragmentCreateProfileBinding
import com.kevin.chatapplication.presentation.auth.profile.CreateProfileUiState
import com.kevin.chatapplication.screens.home.HomeActivity
import com.kevin.chatapplication.utils.AvatarUtils
import com.kevin.chatapplication.viewmodel.createProfileViewModel.CreateProfileViewModel

class CreateProfileFragment : Fragment() {

    private var _binding: FragmentCreateProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreateProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AvatarUtils.bind(binding.tvAvatar, "")

        // Avatar initials follow the name as it's typed.
        binding.etName.doAfterTextChanged { text ->
            AvatarUtils.bind(binding.tvAvatar, text?.toString().orEmpty())
            binding.tilName.error = null
        }

        binding.btnAddPhoto.setOnClickListener {
            Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }

        binding.btnContinue.setOnClickListener {
            viewModel.saveProfile(
                name = binding.etName.text?.toString().orEmpty(),
                about = binding.etAbout.text?.toString().orEmpty(),
                defaultAbout = getString(R.string.default_about)
            )
        }

        viewModel.prefill.observe(viewLifecycleOwner) { user ->
            if (user == null) return@observe
            if (binding.etName.text.isNullOrEmpty()) binding.etName.setText(user.name)
            if (binding.etAbout.text.isNullOrEmpty()) binding.etAbout.setText(user.about)
            viewModel.prefillUsed()
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->

            val saving = state == CreateProfileUiState.Saving
            binding.btnContinue.isEnabled = !saving
            binding.btnContinue.text = if (saving) "" else getString(R.string.continue_label)
            binding.progressSave.isVisible = saving
            binding.tilName.isEnabled = !saving
            binding.tilAbout.isEnabled = !saving

            when (state) {

                CreateProfileUiState.Idle, CreateProfileUiState.Saving -> Unit

                CreateProfileUiState.Saved -> {
                    startActivity(Intent(requireContext(), HomeActivity::class.java))
                    requireActivity().finish()
                }

                is CreateProfileUiState.Error -> {
                    if (state.message == CreateProfileViewModel.NAME_REQUIRED) {
                        binding.tilName.error = getString(R.string.name_required)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            R.string.profile_save_failed,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
