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
import com.kevin.chatapplication.databinding.FragmentSignupBinding
import com.kevin.chatapplication.presentation.auth.signup.SignupUiState
import com.kevin.chatapplication.viewmodel.signupViewModel.SignupViewModel

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SignupViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.txtLogin.setOnClickListener { findNavController().navigateUp() }

        binding.btnSignup.setOnClickListener { submit() }

        binding.etConfirmPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->

            setLoading(state == SignupUiState.Loading)

            when (state) {

                SignupUiState.Idle, SignupUiState.Loading -> Unit

                SignupUiState.Success -> {

                    Toast.makeText(
                        requireContext(),
                        R.string.account_created,
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigate(
                        R.id.action_signupFragment_to_createProfileFragment
                    )

                }

                is SignupUiState.Error -> {

                    Toast.makeText(
                        requireContext(),
                        state.message,
                        Toast.LENGTH_SHORT
                    ).show()

                }

            }

        }
    }

    private fun submit() {
        viewModel.signup(
            binding.etEmail.text?.toString()?.trim().orEmpty(),
            binding.etPassword.text?.toString().orEmpty(),
            binding.etConfirmPassword.text?.toString().orEmpty()
        )
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSignup.isEnabled = !loading
        binding.btnSignup.text = if (loading) "" else getString(R.string.sign_up)
        binding.progressSignup.isVisible = loading
        binding.tilEmail.isEnabled = !loading
        binding.tilPassword.isEnabled = !loading
        binding.tilConfirmPassword.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
