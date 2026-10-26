package com.kevin.chatapplication.utils

import android.content.Context

/** Which status photos I've already watched on this device (for the grey/teal rings). */
object StatusPrefs {

    private const val PREFS = "connectly_status_prefs"
    private const val KEY_SEEN = "seen_ids"

    fun seenIds(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_SEEN, emptySet()).orEmpty()

    fun markSeen(context: Context, itemId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Copy: the set returned by SharedPreferences must not be modified.
        val updated = prefs.getStringSet(KEY_SEEN, emptySet()).orEmpty().toMutableSet()
        if (updated.add(itemId)) {
            // Keep the list from growing forever (statuses expire after a day anyway).
            val trimmed = if (updated.size > 500) updated.toList().takeLast(300).toSet() else updated
            prefs.edit().putStringSet(KEY_SEEN, trimmed).apply()
        }
    }
}
