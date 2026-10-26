package com.kevin.chatapplication.viewmodel.createProfileViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.data.repository.UserRepository
import com.kevin.chatapplication.presentation.auth.profile.CreateProfileUiState

class CreateProfileViewModel : ViewModel() {

    private val userRepository = UserRepository()

    private val _uiState =
        MutableLiveData<CreateProfileUiState>(CreateProfileUiState.Idle)
    val uiState: LiveData<CreateProfileUiState> = _uiState

    /** Starting values for the form (a name made from the email). Emitted once. */
    private val _prefill = MutableLiveData<User?>()
    val prefill: LiveData<User?> = _prefill

    init {
        userRepository.ensureProfile { user -> _prefill.value = user }
    }

    fun prefillUsed() {
        _prefill.value = null
    }

    fun saveProfile(name: String, about: String, defaultAbout: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            _uiState.value = CreateProfileUiState.Error(NAME_REQUIRED)
            return
        }

        _uiState.value = CreateProfileUiState.Saving
        userRepository.updateProfile(cleanName, about.trim().ifEmpty { defaultAbout }) { ok ->
            _uiState.value =
                if (ok) CreateProfileUiState.Saved else CreateProfileUiState.Error(SAVE_FAILED)
        }
    }

    companion object {
        const val NAME_REQUIRED = "name_required"
        const val SAVE_FAILED = "save_failed"
    }
}
