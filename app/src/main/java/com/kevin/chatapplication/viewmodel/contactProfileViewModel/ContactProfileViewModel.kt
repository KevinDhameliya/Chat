package com.kevin.chatapplication.viewmodel.contactProfileViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.kevin.chatapplication.data.model.Message
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.data.repository.ChatRepository
import com.kevin.chatapplication.data.repository.UserRepository

class ContactProfileViewModel : ViewModel() {

    private val userRepository = UserRepository()
    private val chatRepository = ChatRepository()
    private val registrations = mutableListOf<ListenerRegistration>()
    private var started = false
    private var otherUid = ""

    val myUid: String = userRepository.currentUid.orEmpty()
    var chatId = ""
        private set

    private val _contact = MutableLiveData<User>()
    val contact: LiveData<User> = _contact

    private val _isBlocked = MutableLiveData(false)
    val isBlocked: LiveData<Boolean> = _isBlocked

    /** Photos shared in this chat, newest first. */
    private val _media = MutableLiveData<List<Message>>(emptyList())
    val media: LiveData<List<Message>> = _media

    fun start(otherUid: String) {
        if (started || myUid.isEmpty()) return
        started = true
        this.otherUid = otherUid
        chatId = chatRepository.chatIdFor(myUid, otherUid)

        registrations += userRepository.observeUser(otherUid) { _contact.value = it }
        registrations += userRepository.observeUser(myUid) { me ->
            _isBlocked.value = otherUid in me.blockedUsers
        }
        registrations += chatRepository.observeMessages(
            chatId,
            onChange = { messages ->
                _media.value = messages.filter { it.isImage && !it.deleted }.reversed()
            },
            onError = { }
        )
    }

    fun setBlocked(blocked: Boolean) {
        userRepository.setBlocked(otherUid, blocked)
    }

    override fun onCleared() {
        registrations.forEach { it.remove() }
        super.onCleared()
    }
}
