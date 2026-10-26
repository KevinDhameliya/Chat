package com.kevin.chatapplication.utils

import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import coil.load

/**
 * Circle avatars: initials on a coloured circle, with the profile photo on top when there is one.
 * The TextView needs android:background="@drawable/bg_avatar".
 */
object AvatarUtils {

    private val palette = listOf(
        "#0F9D8A", "#3F7BD9", "#E0803A", "#9C5BD1",
        "#D9486B", "#2E9E5B", "#C9A227", "#5A6B7D"
    ).map { Color.parseColor(it) }

    fun bind(view: TextView, name: String) {
        view.text = initials(name)
        val color = palette[Math.floorMod(name.hashCode(), palette.size)]
        view.backgroundTintList = ColorStateList.valueOf(color)
    }

    /** Initials underneath, photo (if any) in [photoView] stacked on top of them. */
    fun bind(initialsView: TextView, photoView: ImageView, name: String, photoUrl: String) {
        bind(initialsView, name)
        if (photoUrl.isBlank()) {
            photoView.isVisible = false
            photoView.setImageDrawable(null)
        } else {
            photoView.isVisible = true
            photoView.load(photoUrl) { crossfade(true) }
        }
    }

    /** Stable colour per name (used for sender names in group chats). */
    fun colorFor(name: String): Int = palette[Math.floorMod(name.hashCode(), palette.size)]

    fun initials(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
        }
    }
}
