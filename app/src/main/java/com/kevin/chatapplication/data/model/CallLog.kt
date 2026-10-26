package com.kevin.chatapplication.data.model

/** One entry in the call history (calls/{callId}). */
data class CallLog(
    val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val calleeId: String = "",
    val calleeName: String = "",
    val type: String = TYPE_VOICE,
    val status: String = STATUS_NOT_ANSWERED,
    val startedAt: Long = 0L,
    val durationSec: Long = 0L
) {
    val isVideo: Boolean get() = type == TYPE_VIDEO

    fun isOutgoing(myUid: String) = callerId == myUid
    fun otherUid(myUid: String) = if (isOutgoing(myUid)) calleeId else callerId
    fun otherName(myUid: String) = if (isOutgoing(myUid)) calleeName else callerName

    /** Incoming and never picked up. */
    fun isMissed(myUid: String) = !isOutgoing(myUid) && status != STATUS_COMPLETED

    companion object {
        const val TYPE_VOICE = "voice"
        const val TYPE_VIDEO = "video"
        const val STATUS_COMPLETED = "completed"
        /** Calling isn't connected yet, so every call is saved with this status for now. */
        const val STATUS_NOT_ANSWERED = "not_answered"
    }
}
