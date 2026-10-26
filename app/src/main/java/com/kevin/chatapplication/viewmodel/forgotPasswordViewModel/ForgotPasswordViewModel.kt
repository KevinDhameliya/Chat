package com.kevin.chatapplication.viewmodel.forgotPasswordViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kevin.chatapplication.presentation.auth.forgotpassword.ForgotPasswordUiState
import com.kevin.chatapplication.utils.ValidationUtils
import com.kevin.chatapplication.viewmodel.forgotPasswordViewModel.authRepository.AuthRepository

class ForgotPasswordViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _uiState =
        MutableLiveData<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)

    val uiState: LiveData<ForgotPasswordUiState> = _uiState

    fun sendResetLink(email: String) {

        val emailResult = ValidationUtils.validateEmail(email)

        if (!emailResult.isSuccess) {
            _uiState.value = ForgotPasswordUiState.Error(emailResult.message!!)
            return
        }

        _uiState.value = ForgotPasswordUiState.Loading

        repository.sendPasswordReset(email) { result ->
            _uiState.postValue(
                if (result.isSuccess) {
                    ForgotPasswordUiState.Success(email)
                } else {
                    ForgotPasswordUiState.Error(
                        result.exceptionOrNull()?.localizedMessage
                            ?: "Could not send the reset link"
                    )
                }
            )
        }
    }
}
