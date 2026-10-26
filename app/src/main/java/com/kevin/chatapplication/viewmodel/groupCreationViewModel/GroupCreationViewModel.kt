package com.kevin.chatapplication.viewmodel.groupCreationViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.data.repository.ChatRepository
import com.kevin.chatapplication.data.repository.UserRepository

sealed class GroupCreationState {
    object Idle : GroupCreationState()
    object Creating : GroupCreationState()
    data class Created(val chatId: String, val name: String) : GroupCreationState()
    data class Error(val message: String) : GroupCreationState()
}

class GroupCreationViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val chatRepository = ChatRepository()
    private var registration: ListenerRegistration? = null

    private var allContacts: List<User> = emptyList()
    private var query = ""
    private var myName = ""

    private val _contacts = MutableLiveData<List<User>>()
    val contacts: LiveData<List<User>> = _contacts

    private val _selected = MutableLiveData<Set<String>>(emptySet())
    val selected: LiveData<Set<String>> = _selected

    private val _state = MutableLiveData<GroupCreationState>(GroupCreationState.Idle)
    val state: LiveData<GroupCreationState> = _state

    init {
        registration = userRepository.observeContacts(
            onChange = { users ->
                allContacts = users
                publish()
            },
            onError = { _contacts.value = emptyList() }
        )
        userRepository.currentUid?.let { uid ->
            userRepository.getUser(uid) { me -> myName = me?.name.orEmpty() }
        }
    }

    fun setQuery(text: String) {
        query = text.trim()
        publish()
    }

    fun toggle(user: User) {
        val current = _selected.value.orEmpty()
        _selected.value = if (user.uid in current) current - user.uid else current + user.uid
    }

    fun create(name: String) {
        val uid = userRepository.currentUid ?: return
        val cleanName = name.trim()
        val members = allContacts.filter { it.uid in _selected.value.orEmpty() }
        if (cleanName.isEmpty() || members.isEmpty()) return

        _state.value = GroupCreationState.Creating
        chatRepository.createGroup(
            name = cleanName,
            myUid = uid,
            myName = myName.ifBlank { "Someone" },
            members = members,
            onSuccess = { chatId -> _state.value = GroupCreationState.Created(chatId, cleanName) },
            onError = { _state.value = GroupCreationState.Error(it) }
        )
    }

    fun stateHandled() {
        _state.value = GroupCreationState.Idle
    }

    private fun publish() {
        _contacts.value = if (query.isBlank()) {
            allContacts
        } else {
            allContacts.filter {
                it.name.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true)
            }
        }
    }

    override fun onCleared() {
        registration?.remove()
        super.onCleared()
    }
}
