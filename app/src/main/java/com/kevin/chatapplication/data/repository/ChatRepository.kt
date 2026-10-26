package com.kevin.chatapplication.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.kevin.chatapplication.data.model.Chat
import com.kevin.chatapplication.data.model.GroupInfo
import com.kevin.chatapplication.data.model.Message
import com.kevin.chatapplication.data.model.SearchItem
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.utils.ImageCompressor
import java.util.UUID
import java.util.concurrent.Executors

/** Where a message goes: a one-to-one chat or a group. */
sealed class SendTarget {
    data class Direct(val otherUid: String, val otherName: String) : SendTarget()
    data class Group(val chatId: String, val memberIds: List<String>) : SendTarget()
}

/**
 * Firestore layout:
 *
 * chats/{chatId}
 *   Direct chat: chatId = both uids sorted and joined with "_"
 *   Group chat:  chatId = random id, isGroup = true, groupName, admins, createdBy
 *     participants:      [uid, ...]
 *     participantNames:  { uid: "Name" }
 *     lastMessage, lastMessageTime, lastSenderId, lastSenderName
 *     unreadCount:       { uid: 3 }
 *
 * chats/{chatId}/messages/{messageId}
 *     senderId, senderName, text, timestamp, status ("sent" | "read"),
 *     type ("text" | "image"), imageUrl,
 *     replyToId, replyToText, replyToSender,
 *     reactions: { uid: "❤️" }, deleted
 *
 * Storage: chat_images/{chatId}/{random}.jpg
 */
class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val chats = db.collection(CHATS)

    fun chatIdFor(uidA: String, uidB: String): String =
        listOf(uidA, uidB).sorted().joinToString("_")

    private fun chatIdOf(myUid: String, target: SendTarget): String = when (target) {
        is SendTarget.Direct -> chatIdFor(myUid, target.otherUid)
        is SendTarget.Group -> target.chatId
    }

    // ---------------------------------------------------------------- reading

    /**
     * All chats (direct and group) the user takes part in, newest first.
     * Sorted on the device so Firestore doesn't need a composite index.
     */
    fun observeChats(
        myUid: String,
        onChange: (List<Chat>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration =
        chats.whereArrayContains(FIELD_PARTICIPANTS, myUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Could not load chats")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toChat(myUid) }
                    .sortedByDescending { it.lastMessageTime }
                onChange(list)
            }

    fun observeMessages(
        chatId: String,
        onChange: (List<Message>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration =
        chats.document(chatId).collection(MESSAGES)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Could not load messages")
                    return@addSnapshotListener
                }
                onChange(snapshot?.documents.orEmpty().map { it.toMessage() })
            }

    /** Group details. Emits null if the group is gone or I'm no longer a member. */
    fun observeGroup(chatId: String, myUid: String, onChange: (GroupInfo?) -> Unit): ListenerRegistration =
        chats.document(chatId).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                onChange(null)
                return@addSnapshotListener
            }
            val info = snapshot.toGroupInfo()
            onChange(if (myUid in info.participants) info else null)
        }

    // ---------------------------------------------------------------- groups

    /** Creates a group with me as admin. Returns the new chat id. */
    fun createGroup(
        name: String,
        myUid: String,
        myName: String,
        members: List<User>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val ref = chats.document()
        val ids = listOf(myUid) + members.map { it.uid }
        val names = mapOf(myUid to myName) + members.associate { it.uid to it.name }
        val group = mapOf(
            "isGroup" to true,
            "groupName" to name,
            FIELD_PARTICIPANTS to ids,
            "participantNames" to names,
            "admins" to listOf(myUid),
            "createdBy" to myUid,
            "createdAt" to FieldValue.serverTimestamp(),
            // Shown in the chat list until someone writes; no sender prefix.
            "lastMessage" to "$myName created the group",
            "lastMessageTime" to FieldValue.serverTimestamp(),
            "lastSenderId" to "",
            "lastSenderName" to "",
            "unreadCount" to ids.filter { it != myUid }.associateWith { 1L }
        )
        ref.set(group)
            .addOnSuccessListener { onSuccess(ref.id) }
            .addOnFailureListener { onError(it.localizedMessage ?: "Could not create the group") }
    }

    fun leaveGroup(chatId: String, myUid: String, myName: String, onDone: (Boolean) -> Unit) {
        chats.document(chatId)
            .update(
                mapOf(
                    FIELD_PARTICIPANTS to FieldValue.arrayRemove(myUid),
                    "admins" to FieldValue.arrayRemove(myUid),
                    "lastMessage" to "$myName left",
                    "lastMessageTime" to FieldValue.serverTimestamp(),
                    "lastSenderId" to "",
                    "lastSenderName" to ""
                )
            )
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    // ---------------------------------------------------------------- sending

    fun sendMessage(
        myUid: String,
        myName: String,
        target: SendTarget,
        text: String,
        replyTo: Message?,
        replyToSenderName: String,
        onError: (String) -> Unit
    ) {
        val fields = mutableMapOf<String, Any>(
            "type" to Message.TYPE_TEXT,
            "text" to text
        )
        fields.putAll(replyFields(replyTo, replyToSenderName))
        writeMessage(myUid, myName, target, fields, preview = text, onError = onError)
    }

    /**
     * Compresses the photo, uploads it to Storage, then sends an image message.
     * [onFinished] is called on the main thread with true when the message was written.
     */
    fun sendImage(
        resolver: ContentResolver,
        uri: Uri,
        myUid: String,
        myName: String,
        target: SendTarget,
        replyTo: Message?,
        replyToSenderName: String,
        onFinished: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        val chatId = chatIdOf(myUid, target)
        ioExecutor.execute {
            val bytes = ImageCompressor.compress(resolver, uri)
            mainHandler.post {
                if (bytes == null) {
                    onError("Could not read that photo")
                    onFinished(false)
                    return@post
                }
                val ref = FirebaseStorage.getInstance().reference
                    .child("$STORAGE_CHAT_IMAGES/$chatId/${UUID.randomUUID()}.jpg")
                val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()

                ref.putBytes(bytes, metadata)
                    .continueWithTask { task ->
                        task.exception?.let { throw it }
                        ref.downloadUrl
                    }
                    .addOnSuccessListener { downloadUri ->
                        val fields = mutableMapOf<String, Any>(
                            "type" to Message.TYPE_IMAGE,
                            "text" to "",
                            "imageUrl" to downloadUri.toString()
                        )
                        fields.putAll(replyFields(replyTo, replyToSenderName))
                        writeMessage(myUid, myName, target, fields, preview = "📷 Photo", onError = onError)
                        onFinished(true)
                    }
                    .addOnFailureListener {
                        onError(it.localizedMessage ?: "Photo upload failed")
                        onFinished(false)
                    }
            }
        }
    }

    /** Sends a copy of [message] (text or photo) to another person or group. */
    fun forwardMessage(
        myUid: String,
        myName: String,
        target: SendTarget,
        message: Message,
        onError: (String) -> Unit
    ) {
        val fields = mutableMapOf<String, Any>(
            "type" to message.type,
            "text" to message.text,
            "imageUrl" to message.imageUrl
        )
        writeMessage(myUid, myName, target, fields, message.preview(), onError)
    }

    private fun writeMessage(
        myUid: String,
        myName: String,
        target: SendTarget,
        fields: Map<String, Any>,
        preview: String,
        onError: (String) -> Unit
    ) {
        val chatRef = chats.document(chatIdOf(myUid, target))
        val messageRef = chatRef.collection(MESSAGES).document()

        val message = HashMap<String, Any>(fields).apply {
            put("senderId", myUid)
            put("senderName", myName)
            put(FIELD_TIMESTAMP, FieldValue.serverTimestamp())
            put("status", Message.STATUS_SENT)
        }

        val common = mapOf(
            "lastMessage" to preview,
            "lastMessageTime" to FieldValue.serverTimestamp(),
            "lastSenderId" to myUid,
            "lastSenderName" to myName
        )
        val chatUpdate: Map<String, Any> = when (target) {
            is SendTarget.Direct -> common + mapOf(
                FIELD_PARTICIPANTS to listOf(myUid, target.otherUid),
                "participantNames" to mapOf(myUid to myName, target.otherUid to target.otherName),
                "unreadCount" to mapOf(target.otherUid to FieldValue.increment(1))
            )
            is SendTarget.Group -> common + mapOf(
                "unreadCount" to target.memberIds
                    .filter { it != myUid }
                    .associateWith { FieldValue.increment(1) }
            )
        }

        db.batch()
            .set(messageRef, message)
            .set(chatRef, chatUpdate, SetOptions.merge())
            .commit()
            .addOnFailureListener { onError(it.localizedMessage ?: "Message not sent") }
    }

    private fun replyFields(replyTo: Message?, senderName: String): Map<String, Any> =
        if (replyTo == null) {
            emptyMap()
        } else {
            mapOf(
                "replyToId" to replyTo.id,
                "replyToText" to replyTo.preview(),
                "replyToSender" to senderName
            )
        }

    // ---------------------------------------------------------------- actions

    /**
     * Clears my unread counter. In direct chats it also marks the other person's
     * messages as read (blue ticks); groups don't track per-person read state.
     */
    fun markAsRead(chatId: String, myUid: String, unreadMessageIds: List<String>) {
        val chatRef = chats.document(chatId)
        val batch = db.batch()
        unreadMessageIds.forEach { id ->
            batch.update(chatRef.collection(MESSAGES).document(id), "status", Message.STATUS_READ)
        }
        batch.update(chatRef, "unreadCount.$myUid", 0)
        batch.commit()
    }

    /** Sets my reaction, or removes it when I tap the same emoji again. */
    fun toggleReaction(chatId: String, message: Message, myUid: String, emoji: String) {
        val value: Any = if (message.reactions[myUid] == emoji) FieldValue.delete() else emoji
        chats.document(chatId).collection(MESSAGES).document(message.id)
            .update("reactions.$myUid", value)
    }

    /** "Delete for everyone": keeps a placeholder bubble, clears the content. */
    fun deleteMessage(chatId: String, message: Message, isLastMessage: Boolean) {
        val chatRef = chats.document(chatId)
        val batch = db.batch()
        batch.update(
            chatRef.collection(MESSAGES).document(message.id),
            mapOf(
                "deleted" to true,
                "text" to "",
                "imageUrl" to "",
                "reactions" to FieldValue.delete()
            )
        )
        if (isLastMessage) batch.update(chatRef, "lastMessage", "🚫 ${Message.DELETED_TEXT}")
        batch.commit()

        // Remove the stored photo too.
        if (message.isImage) {
            runCatching { FirebaseStorage.getInstance().getReferenceFromUrl(message.imageUrl).delete() }
        }
    }

    // ---------------------------------------------------------------- search

    /**
     * Loads the latest [perChatLimit] messages of every chat once, so Search can filter
     * them on the device (Firestore has no full-text search).
     */
    fun loadRecentMessages(
        chatsToSearch: List<Chat>,
        perChatLimit: Long = 300,
        onResult: (Map<Chat, List<Message>>) -> Unit
    ) {
        if (chatsToSearch.isEmpty()) {
            onResult(emptyMap())
            return
        }
        val tasks = chatsToSearch.map { chat ->
            chats.document(chat.id).collection(MESSAGES)
                .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                .limit(perChatLimit)
                .get()
        }
        Tasks.whenAllComplete(tasks).addOnCompleteListener {
            val result = chatsToSearch.indices.associate { index ->
                val task = tasks[index]
                val messages = if (task.isSuccessful) {
                    (task.result as QuerySnapshot).documents.map { it.toMessage() }
                } else {
                    emptyList()
                }
                chatsToSearch[index] to messages
            }
            onResult(result)
        }
    }

    fun filterMessages(cache: Map<Chat, List<Message>>, query: String): List<SearchItem> =
        cache.flatMap { (chat, messages) ->
            messages
                .filter { !it.deleted && it.text.contains(query, ignoreCase = true) }
                .map { SearchItem.MessageHit(chat, it) }
        }.sortedByDescending { it.message.timestamp }

    // ---------------------------------------------------------------- mapping

    private fun DocumentSnapshot.toChat(myUid: String): Chat? {
        @Suppress("UNCHECKED_CAST")
        val participants = get(FIELD_PARTICIPANTS) as? List<String> ?: return null

        @Suppress("UNCHECKED_CAST")
        val names = get("participantNames") as? Map<String, String> ?: emptyMap()

        @Suppress("UNCHECKED_CAST")
        val unread = get("unreadCount") as? Map<String, Number> ?: emptyMap()

        val isGroup = getBoolean("isGroup") == true
        val otherUid = if (isGroup) "" else participants.firstOrNull { it != myUid } ?: return null

        return Chat(
            id = id,
            otherUid = otherUid,
            otherName = if (isGroup) "" else names[otherUid].orEmpty().ifBlank { "Unknown" },
            lastMessage = getString("lastMessage").orEmpty(),
            lastMessageTime = timestampMillis("lastMessageTime"),
            lastSenderId = getString("lastSenderId").orEmpty(),
            lastSenderName = getString("lastSenderName").orEmpty(),
            unreadCount = unread[myUid]?.toLong() ?: 0L,
            isGroup = isGroup,
            groupName = getString("groupName").orEmpty(),
            participants = participants
        )
    }

    private fun DocumentSnapshot.toGroupInfo(): GroupInfo {
        @Suppress("UNCHECKED_CAST")
        val names = get("participantNames") as? Map<String, String> ?: emptyMap()
        return GroupInfo(
            id = id,
            name = getString("groupName").orEmpty(),
            participants = (get(FIELD_PARTICIPANTS) as? List<*>)?.filterIsInstance<String>().orEmpty(),
            participantNames = names,
            admins = (get("admins") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            createdBy = getString("createdBy").orEmpty()
        )
    }

    private fun DocumentSnapshot.toMessage(): Message {
        @Suppress("UNCHECKED_CAST")
        val reactions = (get("reactions") as? Map<String, Any?>)
            ?.mapNotNull { (uid, emoji) -> (emoji as? String)?.let { uid to it } }
            ?.toMap()
            .orEmpty()

        return Message(
            id = id,
            senderId = getString("senderId").orEmpty(),
            senderName = getString("senderName").orEmpty(),
            text = getString("text").orEmpty(),
            timestamp = timestampMillis(FIELD_TIMESTAMP),
            status = getString("status") ?: Message.STATUS_SENT,
            type = getString("type") ?: Message.TYPE_TEXT,
            imageUrl = getString("imageUrl").orEmpty(),
            replyToId = getString("replyToId").orEmpty(),
            replyToText = getString("replyToText").orEmpty(),
            replyToSender = getString("replyToSender").orEmpty(),
            reactions = reactions,
            deleted = getBoolean("deleted") ?: false
        )
    }

    /** Uses the local estimate while the server timestamp is still pending. */
    private fun DocumentSnapshot.timestampMillis(field: String): Long =
        getTimestamp(field, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time
            ?: System.currentTimeMillis()

    companion object {
        const val CHATS = "chats"
        const val MESSAGES = "messages"
        private const val STORAGE_CHAT_IMAGES = "chat_images"
        private const val FIELD_PARTICIPANTS = "participants"
        private const val FIELD_TIMESTAMP = "timestamp"

        private val ioExecutor = Executors.newSingleThreadExecutor()
        private val mainHandler = Handler(Looper.getMainLooper())
    }
}
