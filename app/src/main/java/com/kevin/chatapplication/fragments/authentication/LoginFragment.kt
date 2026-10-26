package com.kevin.chatapplication.fragments.authentication

import android.content.Intent
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
import com.kevin.chatapplication.databinding.FragmentLoginBinding
import com.kevin.chatapplication.presentation.auth.login.LoginUiState
import com.kevin.chatapplication.screens.home.HomeActivity
import com.kevin.chatapplication.viewmodel.loginViewModel.LoginViewModel

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCreateAccount.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToSignupFragment()
            findNavController().navigate(action)
        }

        binding.btLogin.setOnClickListener { submit() }

        // "Done" on the keyboard submits the form.
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }

        binding.txtForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->

            setLoading(state == LoginUiState.Loading)

            when (state) {

                LoginUiState.Loading -> Unit

                LoginUiState.Success -> {

                    startActivity(
                        Intent(requireContext(), HomeActivity::class.java)
                    )

                    requireActivity().finish()

                }

                is LoginUiState.Error -> {

                    Toast.makeText(
                        requireContext(),
                        state.message,
                        Toast.LENGTH_SHORT
                    ).show()

                }

                LoginUiState.Idle -> Unit

            }

        }

    }

    private fun submit() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        viewModel.login(email, password)
    }

    private fun setLoading(loading: Boolean) {
        binding.btLogin.isEnabled = !loading
        binding.btLogin.text = if (loading) "" else getString(R.string.continue_label)
        binding.progressLogin.isVisible = loading
        binding.tilEmail.isEnabled = !loading
        binding.tilPassword.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
