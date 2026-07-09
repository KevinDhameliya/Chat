package com.kevin.chatapplication.fragments.authentication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kevin.chatapplication.R
import com.kevin.chatapplication.databinding.FragmentSignupBinding
import com.kevin.chatapplication.presentation.auth.signup.SignupUiState
import com.kevin.chatapplication.viewmodel.signupViewModel.SignupViewModel
import kotlin.getValue

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private val args: SignupFragmentArgs by navArgs()
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

        val userName = args.userName
        Log.d("User Name: ", userName)

        binding.txtLogin.setOnClickListener {
            findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
        }

        binding.button.setOnClickListener {

            val email =
                binding.etEmail.text.toString().trim()

            val password =
                binding.etPassword.text.toString()

            val confirmPassword =
                binding.etReEnterPassword.text.toString()

            viewModel.signup(
                email,
                password,
                confirmPassword
            )

        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->

            when (state) {

                SignupUiState.Idle -> Unit

                SignupUiState.Loading -> {

                    // Show ProgressBar

                }

                SignupUiState.Success -> {

                    Toast.makeText(
                        requireContext(),
                        "Account Created Successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigate(
                        R.id.action_signupFragment_to_loginFragment
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}