package com.kevin.chatapplication.viewmodel.myProfileViewModel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.data.repository.UserRepository

class MyProfileViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private var registration: ListenerRegistration? = null

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    private val _uploading = MutableLiveData(false)
    val uploading: LiveData<Boolean> = _uploading

    /** One-off messages for a Toast; cleared after showing. */
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    val email: String get() = userRepository.currentEmail

    init {
        userRepository.currentUid?.let { uid ->
            registration = userRepository.observeUser(uid) { _user.value = it }
        }
    }

    fun updateName(name: String) {
        val current = _user.value ?: return
        val clean = name.trim()
        if (clean.isEmpty() || clean == current.name) return
        userRepository.updateProfile(clean, current.about) { }
    }

    fun updateAbout(about: String) {
        val current = _user.value ?: return
        val clean = about.trim()
        if (clean == current.about) return
        userRepository.updateProfile(current.name, clean) { }
    }

    fun uploadPhoto(resolver: ContentResolver, uri: Uri, successText: String, failText: (String) -> String) {
        _uploading.value = true
        userRepository.uploadProfilePhoto(resolver, uri) { ok, error ->
            _uploading.value = false
            _message.value = if (ok) successText else failText(error ?: "unknown error")
        }
    }

    fun removePhoto() {
        userRepository.removeProfilePhoto()
    }

    fun sendPasswordReset(onResult: (Boolean, String) -> Unit) =
        userRepository.sendPasswordReset(onResult)

    fun messageShown() {
        _message.value = null
    }

    override fun onCleared() {
        registration?.remove()
        super.onCleared()
    }
}
