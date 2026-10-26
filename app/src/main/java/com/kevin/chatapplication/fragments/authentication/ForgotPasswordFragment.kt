package com.kevin.chatapplication.fragments.authentication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.kevin.chatapplication.R
import com.kevin.chatapplication.databinding.FragmentForgotPasswordBinding
import com.kevin.chatapplication.presentation.auth.forgotpassword.ForgotPasswordUiState
import com.kevin.chatapplication.viewmodel.forgotPasswordViewModel.ForgotPasswordViewModel

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.txtBackToLogin.setOnClickListener { findNavController().navigateUp() }

        binding.btResetPassword.setOnClickListener { submit() }
        binding.etEmail.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->

            val loading = state == ForgotPasswordUiState.Loading
            binding.btResetPassword.isEnabled = !loading
            binding.progressReset.isVisible = loading
            binding.tilEmail.isEnabled = !loading

            when (state) {

                ForgotPasswordUiState.Idle -> {
                    binding.btResetPassword.setText(R.string.send_reset_link)
                }

                ForgotPasswordUiState.Loading -> {
                    binding.btResetPassword.text = ""
                }

                is ForgotPasswordUiState.Success -> {
                    binding.successCard.isVisible = true
                    binding.tvSuccessMessage.text =
                        getString(R.string.reset_sent_message, state.email)
                    binding.btResetPassword.setText(R.string.resend_link)
                }

                is ForgotPasswordUiState.Error -> {
                    binding.btResetPassword.setText(
                        if (binding.successCard.isVisible) R.string.resend_link
                        else R.string.send_reset_link
                    )
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }

            }

        }
    }

    private fun submit() {
        viewModel.sendResetLink(binding.etEmail.text?.toString()?.trim().orEmpty())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
