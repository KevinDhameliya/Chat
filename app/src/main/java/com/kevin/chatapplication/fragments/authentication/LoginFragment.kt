package com.kevin.chatapplication.fragments.authentication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        binding.txtSignup.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToSignupFragment("Kevin")
            findNavController().navigate(action)
        }

        binding.btLogin.setOnClickListener {

            val email =
                binding.etEmail.text.toString().trim()

            val password =
                binding.editTextTextPassword.text.toString()

            viewModel.login(
                email,
                password
            )

        }

        binding.txtForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->

            when (state) {

                LoginUiState.Loading -> {

                    // Show ProgressBar

                }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}