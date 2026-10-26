package com.kevin.chatapplication.data.model

/**
 * One row in the chat list, already resolved from the current user's point of view.
 * Direct chats use otherUid/otherName; group chats use groupName/participants.
 */
data class Chat(
    val id: String = "",
    val otherUid: String = "",
    val otherName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastSenderId: String = "",
    val lastSenderName: String = "",
    val unreadCount: Long = 0L,
    val isGroup: Boolean = false,
    val groupName: String = "",
    val participants: List<String> = emptyList()
) {
    val displayName: String
        get() = if (isGroup) groupName.ifBlank { "Group" } else otherName
}
