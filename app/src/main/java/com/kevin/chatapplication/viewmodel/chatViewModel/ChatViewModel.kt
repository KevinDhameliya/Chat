package com.kevin.chatapplication.viewmodel.chatViewModel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.kevin.chatapplication.data.model.GroupInfo
import com.kevin.chatapplication.data.model.Message
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.data.repository.ChatRepository
import com.kevin.chatapplication.data.repository.SendTarget
import com.kevin.chatapplication.data.repository.UserRepository

/** Who blocked whom in this conversation (direct chats only). */
enum class BlockState { NONE, BLOCKED_BY_ME, BLOCKED_ME }

class ChatViewModel : ViewModel() {

    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()

    private val registrations = mutableListOf<ListenerRegistration>()

    private var started = false
    private var isScreenVisible = false
    private var lastMarkedMessageId = ""

    var chatId = ""
        private set
    var isGroup = false
        private set
    private var otherUid = ""
    private var otherName = ""
    private var myName = ""

    val myUid: String = userRepository.currentUid.orEmpty()

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    /** Direct chats: the other person. */
    private val _otherUser = MutableLiveData<User>()
    val otherUser: LiveData<User> = _otherUser

    /** Group chats: group details, or null once I'm no longer a member. */
    private val _group = MutableLiveData<GroupInfo?>()
    val group: LiveData<GroupInfo?> = _group

    private val _me = MutableLiveData<User>()

    /** Recomputed whenever either profile changes. Always NONE in groups. */
    val blockState: LiveData<BlockState> = MediatorLiveData<BlockState>().apply {
        val recompute = {
            val me = _me.value
            val other = _otherUser.value
            value = when {
                isGroup -> BlockState.NONE
                me != null && otherUid in me.blockedUsers -> BlockState.BLOCKED_BY_ME
                other != null && myUid in other.blockedUsers -> BlockState.BLOCKED_ME
                else -> BlockState.NONE
            }
        }
        addSource(_me) { recompute() }
        addSource(_otherUser) { recompute() }
    }

    private val _replyTarget = MutableLiveData<Message?>(null)
    val replyTarget: LiveData<Message?> = _replyTarget

    private val _uploading = MutableLiveData(false)
    val uploading: LiveData<Boolean> = _uploading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /**
     * Direct chat: pass [otherUid]/[otherName] and an empty [groupChatId].
     * Group chat: pass the group's chat id as [groupChatId].
     */
    fun start(otherUid: String, otherName: String, groupChatId: String = "") {
        if (started || myUid.isEmpty()) return
        started = true
        this.otherUid = otherUid
        this.otherName = otherName
        isGroup = groupChatId.isNotEmpty()
        chatId = if (isGroup) groupChatId else chatRepository.chatIdFor(myUid, otherUid)

        registrations += userRepository.observeUser(myUid) { me ->
            myName = me.name
            _me.value = me
        }

        if (isGroup) {
            registrations += chatRepository.observeGroup(chatId, myUid) { info ->
                _group.value = info
            }
        } else {
            registrations += userRepository.observeUser(otherUid) { user ->
                this.otherName = user.name
                _otherUser.value = user
            }
        }

        registrations += chatRepository.observeMessages(
            chatId,
            onChange = { list ->
                _messages.value = list
                markIncomingAsRead(list)
            },
            onError = { if (!isGroup) _error.value = it }
        )
    }

    /** Call from onResume/onPause so messages are only marked read while the chat is on screen. */
    fun setScreenVisible(visible: Boolean) {
        isScreenVisible = visible
        if (visible) {
            lastMarkedMessageId = ""
            _messages.value?.let { markIncomingAsRead(it) }
        }
    }

    // ------------------------------------------------------------ sending

    private fun target(): SendTarget? =
        if (isGroup) {
            _group.value?.let { SendTarget.Group(chatId, it.participants) }
        } else {
            SendTarget.Direct(otherUid, otherName)
        }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        val target = target()
        if (trimmed.isEmpty() || target == null || !canSend()) return
        val reply = _replyTarget.value
        chatRepository.sendMessage(
            myUid = myUid,
            myName = myName.ifBlank { "User" },
            target = target,
            text = trimmed,
            replyTo = reply,
            replyToSenderName = reply?.let { senderNameOf(it) }.orEmpty(),
            onError = { _error.value = it }
        )
        _replyTarget.value = null
    }

    fun sendImage(resolver: ContentResolver, uri: Uri) {
        val target = target()
        if (target == null || !canSend()) return
        val reply = _replyTarget.value
        _uploading.value = true
        chatRepository.sendImage(
            resolver = resolver,
            uri = uri,
            myUid = myUid,
            myName = myName.ifBlank { "User" },
            target = target,
            replyTo = reply,
            replyToSenderName = reply?.let { senderNameOf(it) }.orEmpty(),
            onFinished = { _uploading.value = false },
            onError = { _error.value = it }
        )
        _replyTarget.value = null
    }

    fun forward(message: Message, to: User) {
        chatRepository.forwardMessage(
            myUid = myUid,
            myName = myName.ifBlank { "User" },
            target = SendTarget.Direct(to.uid, to.name),
            message = message,
            onError = { _error.value = it }
        )
    }

    private fun canSend(): Boolean =
        if (isGroup) {
            _group.value != null
        } else {
            val state = blockState.value
            state == null || state == BlockState.NONE
        }

    // ------------------------------------------------------------ message actions

    fun setReplyTarget(message: Message?) {
        _replyTarget.value = message
    }

    fun toggleReaction(message: Message, emoji: String) {
        chatRepository.toggleReaction(chatId, message, myUid, emoji)
    }

    fun deleteMessage(message: Message) {
        if (message.senderId != myUid) return
        val isLast = _messages.value?.lastOrNull()?.id == message.id
        chatRepository.deleteMessage(chatId, message, isLast)
        if (_replyTarget.value?.id == message.id) _replyTarget.value = null
    }

    fun unblock() {
        if (!isGroup) userRepository.setBlocked(otherUid, blocked = false)
    }

    fun leaveGroup(onDone: (Boolean) -> Unit) {
        if (!isGroup) return
        chatRepository.leaveGroup(chatId, myUid, myName.ifBlank { "Someone" }, onDone)
    }

    fun loadContacts(onResult: (List<User>) -> Unit) = userRepository.getContacts(onResult)

    fun senderNameOf(message: Message): String = when {
        message.senderId == myUid -> "You"
        isGroup -> message.senderName.ifBlank { _group.value?.nameOf(message.senderId) ?: "Member" }
        else -> otherName
    }

    fun errorShown() {
        _error.value = null
    }

    private fun markIncomingAsRead(list: List<Message>) {
        if (!isScreenVisible || list.isEmpty()) return
        if (isGroup) {
            // Only reset my unread counter, once per new message.
            val lastId = list.last().id
            if (lastId == lastMarkedMessageId) return
            lastMarkedMessageId = lastId
            chatRepository.markAsRead(chatId, myUid, emptyList())
            return
        }
        val unreadIds = list
            .filter { it.senderId == otherUid && it.status != Message.STATUS_READ }
            .map { it.id }
        if (unreadIds.isNotEmpty()) {
            chatRepository.markAsRead(chatId, myUid, unreadIds)
        }
    }

    override fun onCleared() {
        registrations.forEach { it.remove() }
        super.onCleared()
    }
}
