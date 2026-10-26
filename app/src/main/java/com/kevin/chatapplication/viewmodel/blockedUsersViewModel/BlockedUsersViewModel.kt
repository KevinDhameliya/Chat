package com.kevin.chatapplication.viewmodel.blockedUsersViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.data.repository.UserRepository

class BlockedUsersViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private var registration: ListenerRegistration? = null
    private var lastIds: List<String>? = null

    private val _blocked = MutableLiveData<List<User>>()
    val blocked: LiveData<List<User>> = _blocked

    init {
        userRepository.currentUid?.let { uid ->
            // Re-load the profiles whenever my blockedUsers list changes.
            registration = userRepository.observeUser(uid) { me ->
                if (me.blockedUsers == lastIds) return@observeUser
                lastIds = me.blockedUsers
                userRepository.getUsers(me.blockedUsers) { users -> _blocked.value = users }
            }
        } ?: run { _blocked.value = emptyList() }
    }

    fun unblock(user: User) {
        userRepository.setBlocked(user.uid, blocked = false)
    }

    override fun onCleared() {
        registration?.remove()
        super.onCleared()
    }
}
