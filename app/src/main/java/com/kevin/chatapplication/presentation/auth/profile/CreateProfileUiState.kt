package com.kevin.chatapplication.presentation.auth.profile

sealed class CreateProfileUiState {

    object Idle : CreateProfileUiState()

    object Saving : CreateProfileUiState()

    object Saved : CreateProfileUiState()

    data class Error(val message: String) : CreateProfileUiState()
}
