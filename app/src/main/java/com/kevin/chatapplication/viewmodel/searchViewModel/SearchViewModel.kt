package com.kevin.chatapplication.viewmodel.searchViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.kevin.chatapplication.data.model.Chat
import com.kevin.chatapplication.data.model.Message
import com.kevin.chatapplication.data.model.SearchItem
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.data.repository.ChatRepository
import com.kevin.chatapplication.data.repository.UserRepository

enum class SearchTab { MESSAGES, CONTACTS, GROUPS }

class SearchViewModel : ViewModel() {

    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()
    private val registrations = mutableListOf<ListenerRegistration>()

    val myUid: String = userRepository.currentUid.orEmpty()

    private var chats: List<Chat> = emptyList()
    private var chatsLoaded = false
    private var contacts: List<User> = emptyList()

    /** Recent messages of every chat, loaded once on the first message search. */
    private var messageCache: Map<Chat, List<Message>>? = null
    private var loadingCache = false

    var query: String = ""
        private set
    var tab: SearchTab = SearchTab.MESSAGES
        private set

    private val _results = MutableLiveData<List<SearchItem>>(emptyList())
    val results: LiveData<List<SearchItem>> = _results

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    init {
        if (myUid.isNotEmpty()) {
            registrations += chatRepository.observeChats(
                myUid,
                onChange = { list ->
                    chats = list
                    chatsLoaded = true
                    if (tab == SearchTab.GROUPS) publish()
                    if (messageCache == null && query.isNotBlank() && tab == SearchTab.MESSAGES) {
                        publish()
                    }
                },
                onError = { chatsLoaded = true }
            )
            registrations += userRepository.observeContacts(
                onChange = { list ->
                    contacts = list
                    if (tab == SearchTab.CONTACTS) publish()
                },
                onError = { }
            )
        }
    }

    fun setQuery(text: String) {
        query = text.trim()
        publish()
    }

    fun setTab(newTab: SearchTab) {
        tab = newTab
        publish()
    }

    private fun publish() {
        if (query.isBlank()) {
            _loading.value = false
            _results.value = emptyList()
            return
        }
        when (tab) {
            SearchTab.CONTACTS -> {
                _loading.value = false
                _results.value = contacts
                    .filter {
                        it.name.contains(query, ignoreCase = true) ||
                            it.email.contains(query, ignoreCase = true)
                    }
                    .map { SearchItem.ContactHit(it) }
            }

            SearchTab.GROUPS -> {
                _loading.value = false
                _results.value = chats
                    .filter { it.isGroup && it.groupName.contains(query, ignoreCase = true) }
                    .map { SearchItem.GroupHit(it) }
            }

            SearchTab.MESSAGES -> {
                val cache = messageCache
                if (cache != null) {
                    _loading.value = false
                    _results.value = chatRepository.filterMessages(cache, query)
                } else {
                    loadCache()
                }
            }
        }
    }

    private fun loadCache() {
        _loading.value = true
        if (loadingCache || !chatsLoaded) return
        loadingCache = true
        chatRepository.loadRecentMessages(chats) { cache ->
            messageCache = cache
            loadingCache = false
            publish()
        }
    }

    override fun onCleared() {
        registrations.forEach { it.remove() }
        super.onCleared()
    }
}
