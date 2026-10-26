package com.kevin.chatapplication.viewmodel.mediaViewerViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.kevin.chatapplication.data.model.Message
import com.kevin.chatapplication.data.repository.ChatRepository

class MediaViewerViewModel : ViewModel() {

    private val chatRepository = ChatRepository()
    private var registration: ListenerRegistration? = null
    private var started = false
    private var startMessageId = ""

    /** Photos in the chat, oldest first (same order as the conversation). */
    private val _photos = MutableLiveData<List<Message>>()
    val photos: LiveData<List<Message>> = _photos

    /** Page to open first; emitted once, then cleared. */
    private val _startIndex = MutableLiveData<Int?>()
    val startIndex: LiveData<Int?> = _startIndex

    fun start(chatId: String, messageId: String) {
        if (started) return
        started = true
        startMessageId = messageId
        registration = chatRepository.observeMessages(
            chatId,
            onChange = { messages ->
                val photos = messages.filter { it.isImage && !it.deleted }
                _photos.value = photos
                if (startMessageId.isNotEmpty()) {
                    _startIndex.value = photos.indexOfFirst { it.id == startMessageId }.coerceAtLeast(0)
                    startMessageId = ""
                }
            },
            onError = { }
        )
    }

    fun startIndexUsed() {
        _startIndex.value = null
    }

    override fun onCleared() {
        registration?.remove()
        super.onCleared()
    }
}
