package com.kevin.chatapplication.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kevin.chatapplication.data.model.CallLog

/**
 * Firestore layout:
 *
 * calls/{callId}
 *     callerId, callerName, calleeId, calleeName,
 *     participants: [callerId, calleeId],
 *     type ("voice" | "video"), status ("completed" | "not_answered"),
 *     startedAt, durationSec,
 *     hiddenFor: [uid]        people who removed it from their own history
 *
 * Real calling (Agora/WebRTC) will update status and durationSec later.
 */
class CallRepository {

    private val db = FirebaseFirestore.getInstance()
    private val calls = db.collection(CALLS)
    private val userRepository = UserRepository()

    /** My call history, newest first (sorted on the device; no index needed). */
    fun observeCalls(
        myUid: String,
        onChange: (List<CallLog>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration =
        calls.whereArrayContains("participants", myUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Could not load calls")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents.orEmpty()
                    .filter { doc ->
                        val hidden = (doc.get("hiddenFor") as? List<*>).orEmpty()
                        myUid !in hidden
                    }
                    .map { it.toCallLog() }
                    .sortedByDescending { it.startedAt }
                onChange(list)
            }

    /**
     * Saves a call attempt from me to [otherUid].
     * For now nothing rings: the entry shows as "Outgoing" for me and "Missed" for them.
     */
    fun logCall(
        otherUid: String,
        otherName: String,
        type: String,
        onComplete: (Boolean) -> Unit
    ) {
        val myUid = userRepository.currentUid ?: return onComplete(false)
        userRepository.getUser(myUid) { me ->
            calls.add(
                mapOf(
                    "callerId" to myUid,
                    "callerName" to (me?.name ?: "Someone"),
                    "calleeId" to otherUid,
                    "calleeName" to otherName,
                    "participants" to listOf(myUid, otherUid),
                    "type" to type,
                    "status" to CallLog.STATUS_NOT_ANSWERED,
                    "startedAt" to FieldValue.serverTimestamp(),
                    "durationSec" to 0L,
                    "hiddenFor" to emptyList<String>()
                )
            )
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    /** Removes a call from my history only; the other person still sees it. */
    fun hideCall(callId: String, myUid: String) {
        calls.document(callId).update("hiddenFor", FieldValue.arrayUnion(myUid))
    }

    private fun DocumentSnapshot.toCallLog() = CallLog(
        id = id,
        callerId = getString("callerId").orEmpty(),
        callerName = getString("callerName").orEmpty(),
        calleeId = getString("calleeId").orEmpty(),
        calleeName = getString("calleeName").orEmpty(),
        type = getString("type") ?: CallLog.TYPE_VOICE,
        status = getString("status") ?: CallLog.STATUS_NOT_ANSWERED,
        startedAt = getTimestamp("startedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: System.currentTimeMillis(),
        durationSec = getLong("durationSec") ?: 0L
    )

    companion object {
        const val CALLS = "calls"
    }
}
