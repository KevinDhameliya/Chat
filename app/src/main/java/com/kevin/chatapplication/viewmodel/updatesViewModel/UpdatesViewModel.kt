package com.kevin.chatapplication.viewmodel.updatesViewModel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.data.model.UserStatus
import com.kevin.chatapplication.data.repository.StatusRepository
import com.kevin.chatapplication.data.repository.UserRepository

class UpdatesViewModel : ViewModel() {

    private val statusRepository = StatusRepository()
    private val userRepository = UserRepository()
    private val registrations = mutableListOf<ListenerRegistration>()

    val myUid: String = userRepository.currentUid.orEmpty()
    private var all: List<UserStatus> = emptyList()

    private val _me = MutableLiveData<User>()
    val me: LiveData<User> = _me

    private val _myStatus = MutableLiveData<UserStatus?>(null)
    val myStatus: LiveData<UserStatus?> = _myStatus

    /** Other people's statuses, newest first. */
    private val _others = MutableLiveData<List<UserStatus>>()
    val others: LiveData<List<UserStatus>> = _others

    private val _posting = MutableLiveData(false)
    val posting: LiveData<Boolean> = _posting

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    init {
        if (myUid.isNotEmpty()) {
            registrations += userRepository.observeUser(myUid) { me ->
                _me.value = me
                publish()
            }
            registrations += statusRepository.observeRecent { list ->
                all = list
                publish()
            }
        }
    }

    private fun publish() {
        val blocked = _me.value?.blockedUsers.orEmpty()
        _myStatus.value = all.firstOrNull { it.uid == myUid }
        _others.value = all
            .filter { it.uid != myUid && it.uid !in blocked }
            .sortedByDescending { it.latest }
    }

    fun post(resolver: ContentResolver, uri: Uri, caption: String, successText: String, failText: (String) -> String) {
        val me = _me.value ?: return
        _posting.value = true
        statusRepository.postStatus(resolver, uri, caption.trim(), me) { ok, error ->
            _posting.value = false
            _message.value = if (ok) successText else failText(error ?: "unknown error")
        }
    }

    fun messageShown() {
        _message.value = null
    }

    override fun onCleared() {
        registrations.forEach { it.remove() }
        super.onCleared()
    }
}
