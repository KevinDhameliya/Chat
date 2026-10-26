package com.kevin.chatapplication.presentation.auth.forgotpassword

sealed class ForgotPasswordUiState {

    object Idle : ForgotPasswordUiState()

    object Loading : ForgotPasswordUiState()

    data class Success(val email: String) : ForgotPasswordUiState()

    data class Error(val message: String) : ForgotPasswordUiState()
}
