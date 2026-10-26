package com.kevin.chatapplication.data.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    /** Sender's name at send time (shown above bubbles in group chats). */
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val status: String = STATUS_SENT,
    val type: String = TYPE_TEXT,
    val imageUrl: String = "",
    val replyToId: String = "",
    val replyToText: String = "",
    val replyToSender: String = "",
    /** uid -> emoji */
    val reactions: Map<String, String> = emptyMap(),
    val deleted: Boolean = false
) {
    val isImage: Boolean get() = type == TYPE_IMAGE && imageUrl.isNotBlank()
    val hasReply: Boolean get() = replyToId.isNotBlank()

    /** Short text used in the chat list, reply quotes and search results. */
    fun preview(): String = when {
        deleted -> DELETED_TEXT
        type == TYPE_IMAGE && text.isBlank() -> "📷 Photo"
        type == TYPE_IMAGE -> "📷 $text"
        else -> text
    }

    companion object {
        const val STATUS_SENT = "sent"
        const val STATUS_READ = "read"
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val DELETED_TEXT = "This message was deleted"
    }
}
