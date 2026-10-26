package com.kevin.chatapplication.utils

import android.content.Context

/** Per-chat settings kept on this device (e.g. muted chats). */
object ChatPrefs {

    private const val PREFS = "connectly_chat_prefs"

    fun isMuted(context: Context, chatId: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("muted_$chatId", false)

    fun setMuted(context: Context, chatId: String, muted: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("muted_$chatId", muted)
            .apply()
    }
}
