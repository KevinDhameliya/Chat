package com.kevin.chatapplication.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val about: String = "Hey there! I'm using Connectly.",
    val photoUrl: String = "",
    val online: Boolean = false,
    val lastSeen: Long = 0L,
    /** uids this user has blocked */
    val blockedUsers: List<String> = emptyList()
)
