package com.kevin.chatapplication.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.kevin.chatapplication.data.model.StatusItem
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.data.model.UserStatus
import com.kevin.chatapplication.utils.ImageCompressor
import java.util.Date
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Firestore layout:
 *
 * statuses/{uid}                    one doc per person
 *     uid, name, photoUrl, lastUpdated, itemsChangedAt
 * statuses/{uid}/items/{itemId}     one doc per photo
 *     imageUrl, caption, createdAt, viewers: [uid]
 *
 * Storage: statuses/{uid}/{random}.jpg
 *
 * Items older than 24 hours are simply ignored by the app.
 */
class StatusRepository {

    private val db = FirebaseFirestore.getInstance()
    private val statuses = db.collection(STATUSES)

    private fun cutoff() = Timestamp(Date(System.currentTimeMillis() - DAY_MS))

    /** Everyone with at least one status from the last 24 hours (me included). */
    fun observeRecent(onChange: (List<UserStatus>) -> Unit): ListenerRegistration =
        statuses.whereGreaterThan("lastUpdated", cutoff())
            .addSnapshotListener { snapshot, error ->
                val docs = snapshot?.documents.orEmpty()
                if (error != null || docs.isEmpty()) {
                    onChange(emptyList())
                    return@addSnapshotListener
                }
                val tasks = docs.map { doc ->
                    doc.reference.collection(ITEMS)
                        .whereGreaterThan("createdAt", cutoff())
                        .orderBy("createdAt", Query.Direction.ASCENDING)
                        .get()
                }
                Tasks.whenAllComplete(tasks).addOnCompleteListener {
                    val result = docs.indices.mapNotNull { i ->
                        val task = tasks[i]
                        if (!task.isSuccessful) return@mapNotNull null
                        val owner = docs[i]
                        val items = task.result.documents.map { it.toItem(owner.id) }
                        if (items.isEmpty()) {
                            null
                        } else {
                            UserStatus(
                                uid = owner.id,
                                name = owner.getString("name").orEmpty(),
                                photoUrl = owner.getString("photoUrl").orEmpty(),
                                items = items
                            )
                        }
                    }
                    onChange(result)
                }
            }

    /** Live list of one person's items (used by the viewer, so view counts update). */
    fun observeItems(ownerUid: String, onChange: (List<StatusItem>) -> Unit): ListenerRegistration =
        statuses.document(ownerUid).collection(ITEMS)
            .whereGreaterThan("createdAt", cutoff())
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                onChange(snapshot?.documents.orEmpty().map { it.toItem(ownerUid) })
            }

    /** Compresses and uploads the photo, then publishes the status. */
    fun postStatus(
        resolver: ContentResolver,
        uri: Uri,
        caption: String,
        me: User,
        onComplete: (success: Boolean, error: String?) -> Unit
    ) {
        ioExecutor.execute {
            val bytes = ImageCompressor.compress(resolver, uri)
            mainHandler.post {
                if (bytes == null) {
                    onComplete(false, "Could not read that photo")
                    return@post
                }
                val ref = FirebaseStorage.getInstance().reference
                    .child("$STATUSES/${me.uid}/${UUID.randomUUID()}.jpg")
                val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()

                ref.putBytes(bytes, metadata)
                    .continueWithTask { task ->
                        task.exception?.let { throw it }
                        ref.downloadUrl
                    }
                    .continueWithTask { task ->
                        task.exception?.let { throw it }
                        val ownerRef = statuses.document(me.uid)
                        db.batch()
                            .set(
                                ownerRef.collection(ITEMS).document(),
                                mapOf(
                                    "imageUrl" to task.result.toString(),
                                    "caption" to caption,
                                    "createdAt" to FieldValue.serverTimestamp(),
                                    "viewers" to emptyList<String>()
                                )
                            )
                            .set(
                                ownerRef,
                                mapOf(
                                    "uid" to me.uid,
                                    "name" to me.name,
                                    "photoUrl" to me.photoUrl,
                                    "lastUpdated" to FieldValue.serverTimestamp(),
                                    "itemsChangedAt" to FieldValue.serverTimestamp()
                                ),
                                SetOptions.merge()
                            )
                            .commit()
                    }
                    .addOnSuccessListener { onComplete(true, null) }
                    .addOnFailureListener { onComplete(false, it.localizedMessage) }
            }
        }
    }

    fun markViewed(item: StatusItem, myUid: String) {
        statuses.document(item.ownerUid).collection(ITEMS).document(item.id)
            .update("viewers", FieldValue.arrayUnion(myUid))
    }

    fun deleteItem(item: StatusItem, onDone: (Boolean) -> Unit) {
        val ownerRef = statuses.document(item.ownerUid)
        db.batch()
            .delete(ownerRef.collection(ITEMS).document(item.id))
            // Touch the owner doc so everyone's Updates list refreshes.
            .set(ownerRef, mapOf("itemsChangedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
            .commit()
            .addOnSuccessListener {
                runCatching { FirebaseStorage.getInstance().getReferenceFromUrl(item.imageUrl).delete() }
                onDone(true)
            }
            .addOnFailureListener { onDone(false) }
    }

    private fun DocumentSnapshot.toItem(ownerUid: String) = StatusItem(
        id = id,
        ownerUid = ownerUid,
        imageUrl = getString("imageUrl").orEmpty(),
        caption = getString("caption").orEmpty(),
        createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: System.currentTimeMillis(),
        viewers = (get("viewers") as? List<*>)?.filterIsInstance<String>().orEmpty()
    )

    companion object {
        const val STATUSES = "statuses"
        private const val ITEMS = "items"
        const val DAY_MS = 24L * 60 * 60 * 1000

        private val ioExecutor = Executors.newSingleThreadExecutor()
        private val mainHandler = Handler(Looper.getMainLooper())
    }
}
