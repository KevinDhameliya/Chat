package com.kevin.chatapplication.viewmodel.newChatViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.data.repository.UserRepository

class NewChatViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private var registration: ListenerRegistration? = null
    private var allContacts: List<User> = emptyList()
    private var query = ""

    private val _contacts = MutableLiveData<List<User>>(emptyList())
    val contacts: LiveData<List<User>> = _contacts

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        registration = userRepository.observeContacts(
            onChange = { users ->
                allContacts = users
                _loading.value = false
                publish()
            },
            onError = { message ->
                _loading.value = false
                _error.value = message
            }
        )
    }

    fun setQuery(text: String) {
        query = text.trim()
        publish()
    }

    fun errorShown() {
        _error.value = null
    }

    private fun publish() {
        _contacts.value = if (query.isBlank()) {
            allContacts
        } else {
            allContacts.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true)
            }
        }
    }

    override fun onCleared() {
        registration?.remove()
        super.onCleared()
    }
}
