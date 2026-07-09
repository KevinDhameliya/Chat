package com.kevin.chatapplication.viewmodel.loginViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kevin.chatapplication.presentation.auth.login.LoginUiState
import com.kevin.chatapplication.utils.ValidationUtils
import com.kevin.chatapplication.viewmodel.loginViewModel.authRepository.AuthRepository

class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _uiState =
        MutableLiveData<LoginUiState>(LoginUiState.Idle)

    val uiState: LiveData<LoginUiState> = _uiState

    fun login(
        email: String,
        password: String
    ) {

        val emailResult = ValidationUtils.validateEmail(email)

        if (!emailResult.isSuccess) {
            _uiState.value =
                LoginUiState.Error(emailResult.message!!)
            return
        }

        val passwordResult =
            ValidationUtils.validatePassword(password)

        if (!passwordResult.isSuccess) {
            _uiState.value =
                LoginUiState.Error(passwordResult.message!!)
            return
        }

        _uiState.value = LoginUiState.Loading

        repository.loginUser(email, password) { result ->

            if (result.isSuccess) {

                _uiState.postValue(LoginUiState.Success)

            } else {

                _uiState.postValue(
                    LoginUiState.Error(
                        result.exceptionOrNull()?.localizedMessage
                            ?: "Login Failed"
                    )
                )

            }

        }

    }

}