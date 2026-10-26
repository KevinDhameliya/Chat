package com.kevin.chatapplication.data.model

/** One photo in someone's status (lives for 24 hours). */
data class StatusItem(
    val id: String = "",
    val ownerUid: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val createdAt: Long = 0L,
    /** uids of people who have seen it */
    val viewers: List<String> = emptyList()
)

/** Everything one person posted in the last 24 hours, oldest first. */
data class UserStatus(
    val uid: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val items: List<StatusItem> = emptyList()
) {
    val latest: Long get() = items.maxOfOrNull { it.createdAt } ?: 0L
}
