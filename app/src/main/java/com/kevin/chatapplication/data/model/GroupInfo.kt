package com.kevin.chatapplication.data.model

/** Details of a group chat (the chats/{chatId} document of a group). */
data class GroupInfo(
    val id: String = "",
    val name: String = "",
    val participants: List<String> = emptyList(),
    /** uid -> display name */
    val participantNames: Map<String, String> = emptyMap(),
    val admins: List<String> = emptyList(),
    val createdBy: String = ""
) {
    fun nameOf(uid: String): String = participantNames[uid].orEmpty().ifBlank { "Member" }
}
