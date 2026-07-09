package com.kevin.chatapplication.viewmodel.signupViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kevin.chatapplication.utils.ValidationUtils
import com.kevin.chatapplication.presentation.auth.signup.SignupUiState
import com.kevin.chatapplication.viewmodel.signupViewModel.authRepository.AuthRepository

class SignupViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _uiState =
        MutableLiveData<SignupUiState>(SignupUiState.Idle)

    val uiState: LiveData<SignupUiState> = _uiState

    fun signup(
        email: String,
        password: String,
        confirmPassword: String
    ) {

        val emailResult =
            ValidationUtils.validateEmail(email)

        if (!emailResult.isSuccess) {

            _uiState.value =
                SignupUiState.Error(emailResult.message!!)

            return
        }

        val passwordResult =
            ValidationUtils.validatePassword(password)

        if (!passwordResult.isSuccess) {

            _uiState.value =
                SignupUiState.Error(passwordResult.message!!)

            return
        }

        val confirmResult =
            ValidationUtils.validateConfirmPassword(
                password,
                confirmPassword
            )

        if (!confirmResult.isSuccess) {

            _uiState.value =
                SignupUiState.Error(confirmResult.message!!)

            return
        }

        _uiState.value = SignupUiState.Loading

        repository.signup(
            email,
            password
        ) { result ->

            if (result.isSuccess) {

                _uiState.postValue(
                    SignupUiState.Success
                )

            } else {

                _uiState.postValue(
                    SignupUiState.Error(
                        result.exceptionOrNull()?.localizedMessage
                            ?: "Signup Failed"
                    )
                )

            }

        }

    }

}