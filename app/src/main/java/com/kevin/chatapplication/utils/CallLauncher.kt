package com.kevin.chatapplication.utils

import android.content.Context
import android.widget.Toast
import com.kevin.chatapplication.R
import com.kevin.chatapplication.data.model.CallLog
import com.kevin.chatapplication.data.repository.CallRepository

/**
 * Starts a call from any screen. Voice/video calling isn't connected yet,
 * so for now this saves the attempt to the call history and says so.
 */
object CallLauncher {

    private val repository = CallRepository()

    fun call(context: Context, otherUid: String, otherName: String, video: Boolean) {
        if (otherUid.isBlank()) return
        val appContext = context.applicationContext
        repository.logCall(
            otherUid = otherUid,
            otherName = otherName,
            type = if (video) CallLog.TYPE_VIDEO else CallLog.TYPE_VOICE
        ) { ok ->
            Toast.makeText(
                appContext,
                if (ok) R.string.call_saved_not_connected else R.string.call_log_failed,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
