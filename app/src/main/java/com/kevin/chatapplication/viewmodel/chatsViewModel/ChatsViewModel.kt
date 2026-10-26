package com.kevin.chatapplication.viewmodel.chatsViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.kevin.chatapplication.data.model.Chat
import com.kevin.chatapplication.data.repository.ChatRepository
import com.kevin.chatapplication.data.repository.UserRepository

enum class ChatFilter { ALL, UNREAD, GROUPS, FAVORITES }

class ChatsViewModel : ViewModel() {

    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()
    private var registration: ListenerRegistration? = null

    private var allChats: List<Chat> = emptyList()
    private var query = ""
    private var filter = ChatFilter.ALL

    private val _chats = MutableLiveData<List<Chat>>(emptyList())
    val chats: LiveData<List<Chat>> = _chats

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    val currentUid: String
        get() = userRepository.currentUid.orEmpty()

    /** True only when the user has no chats at all (not just none matching the filter). */
    val hasNoChatsAtAll: Boolean
        get() = allChats.isEmpty() && query.isBlank() && filter == ChatFilter.ALL

    init {
        val uid = userRepository.currentUid
        if (uid == null) {
            _loading.value = false
        } else {
            registration = chatRepository.observeChats(
                uid,
                onChange = { list ->
                    allChats = list
                    _loading.value = false
                    publish()
                },
                onError = { message ->
                    _loading.value = false
                    _error.value = message
                }
            )
        }
    }

    fun setQuery(text: String) {
        query = text.trim()
        publish()
    }

    fun setFilter(newFilter: ChatFilter) {
        filter = newFilter
        publish()
    }

    fun errorShown() {
        _error.value = null
    }

    private fun publish() {
        _chats.value = allChats
            .filter { chat ->
                when (filter) {
                    ChatFilter.ALL -> true
                    ChatFilter.UNREAD -> chat.unreadCount > 0
                    ChatFilter.GROUPS -> chat.isGroup
                    // Favourites aren't built yet.
                    ChatFilter.FAVORITES -> false
                }
            }
            .filter { chat ->
                query.isBlank() ||
                    chat.displayName.contains(query, ignoreCase = true) ||
                    chat.lastMessage.contains(query, ignoreCase = true)
            }
    }

    override fun onCleared() {
        registration?.remove()
        super.onCleared()
    }
}
