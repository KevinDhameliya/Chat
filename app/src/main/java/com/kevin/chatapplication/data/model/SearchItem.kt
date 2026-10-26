package com.kevin.chatapplication.data.model

/** One row on the Search screen. */
sealed class SearchItem {
    data class MessageHit(val chat: Chat, val message: Message) : SearchItem()
    data class ContactHit(val user: User) : SearchItem()
    data class GroupHit(val chat: Chat) : SearchItem()
}
