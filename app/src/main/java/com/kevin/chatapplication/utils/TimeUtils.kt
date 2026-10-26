package com.kevin.chatapplication.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {

    /** "10:24 AM" */
    fun formatTime(millis: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))

    /** Chat list: "10:24 AM", "Yesterday", "Mon" or "12/03/26". */
    fun formatChatListTime(millis: Long): String {
        if (millis <= 0L) return ""
        val then = Calendar.getInstance().apply { timeInMillis = millis }
        val now = Calendar.getInstance()
        val daysAgo = daysBetween(then, now)
        return when {
            daysAgo == 0 -> formatTime(millis)
            daysAgo == 1 -> "Yesterday"
            daysAgo < 7 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(millis))
            else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(millis))
        }
    }

    /** Chat header: "Online", "last seen today at 10:24 AM", ... */
    fun formatPresence(online: Boolean, lastSeen: Long): String {
        if (online) return "Online"
        if (lastSeen <= 0L) return ""
        val then = Calendar.getInstance().apply { timeInMillis = lastSeen }
        val daysAgo = daysBetween(then, Calendar.getInstance())
        return when {
            daysAgo == 0 -> "last seen today at ${formatTime(lastSeen)}"
            daysAgo == 1 -> "last seen yesterday at ${formatTime(lastSeen)}"
            else -> "last seen " +
                SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(lastSeen))
        }
    }

    /** Status times: "Just now", "5 minutes ago", "2 hours ago", "Yesterday". */
    fun formatAgo(millis: Long): String {
        val minutes = (System.currentTimeMillis() - millis) / 60_000
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
            minutes < 24 * 60 -> {
                val hours = minutes / 60
                if (hours == 1L) "1 hour ago" else "$hours hours ago"
            }
            else -> "Yesterday"
        }
    }

    private fun daysBetween(earlier: Calendar, later: Calendar): Int {
        val a = earlier.clone() as Calendar
        val b = later.clone() as Calendar
        listOf(a, b).forEach {
            it.set(Calendar.HOUR_OF_DAY, 0)
            it.set(Calendar.MINUTE, 0)
            it.set(Calendar.SECOND, 0)
            it.set(Calendar.MILLISECOND, 0)
        }
        return ((b.timeInMillis - a.timeInMillis) / (24L * 60 * 60 * 1000)).toInt()
    }
}
