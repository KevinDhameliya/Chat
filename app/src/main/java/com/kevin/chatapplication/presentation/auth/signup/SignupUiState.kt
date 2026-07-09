package com.kevin.chatapplication.presentation.auth.signup

sealed class SignupUiState {

    object Idle : SignupUiState()

    object Loading : SignupUiState()

    object Success : SignupUiState()

    data class Error(
        val message: String
    ) : SignupUiState()
}
