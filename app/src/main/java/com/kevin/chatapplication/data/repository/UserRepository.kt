package com.kevin.chatapplication.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.utils.ImageCompressor
import java.util.concurrent.Executors

/**
 * Reads and writes user profiles in the Firestore "users" collection.
 * Document id = Firebase Auth uid.
 */
class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val users = FirebaseFirestore.getInstance().collection(USERS)

    val currentUid: String?
        get() = auth.currentUser?.uid

    val currentEmail: String
        get() = auth.currentUser?.email.orEmpty()

    /**
     * Makes sure the signed-in user has a profile document.
     * Accounts created before Firestore was added get one the first time they open Home.
     */
    fun ensureProfile(onComplete: (User?) -> Unit = {}) {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            onComplete(null)
            return
        }
        val ref = users.document(firebaseUser.uid)
        ref.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    onComplete(snapshot.toUser())
                } else {
                    val email = firebaseUser.email.orEmpty()
                    val user = User(
                        uid = firebaseUser.uid,
                        name = defaultNameFrom(email),
                        email = email,
                        online = true,
                        lastSeen = System.currentTimeMillis()
                    )
                    ref.set(user.toMap())
                        .addOnSuccessListener { onComplete(user) }
                        .addOnFailureListener { onComplete(null) }
                }
            }
            .addOnFailureListener { onComplete(null) }
    }

    fun setOnline(online: Boolean) {
        val uid = currentUid ?: return
        users.document(uid).set(
            mapOf("online" to online, "lastSeen" to System.currentTimeMillis()),
            SetOptions.merge()
        )
    }

    fun getUser(uid: String, onResult: (User?) -> Unit) {
        users.document(uid).get()
            .addOnSuccessListener { onResult(if (it.exists()) it.toUser() else null) }
            .addOnFailureListener { onResult(null) }
    }

    fun observeUser(uid: String, onChange: (User) -> Unit): ListenerRegistration =
        users.document(uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) onChange(snapshot.toUser())
        }

    /** Every registered user except the current one, sorted by name. */
    fun observeContacts(
        onChange: (List<User>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration =
        users.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.localizedMessage ?: "Could not load contacts")
                return@addSnapshotListener
            }
            val me = currentUid
            val contacts = snapshot?.documents.orEmpty()
                .map { it.toUser() }
                .filter { it.uid != me }
                .sortedBy { it.name.lowercase() }
            onChange(contacts)
        }

    fun updateProfile(name: String, about: String, onComplete: (Boolean) -> Unit) {
        val firebaseUser = auth.currentUser ?: return onComplete(false)
        users.document(firebaseUser.uid)
            .set(
                mapOf(
                    "uid" to firebaseUser.uid,
                    "email" to firebaseUser.email.orEmpty(),
                    "name" to name,
                    "about" to about
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    /** One-off list of contacts (used by Forward). */
    fun getContacts(onResult: (List<User>) -> Unit) {
        users.get()
            .addOnSuccessListener { snapshot ->
                val me = currentUid
                onResult(
                    snapshot.documents
                        .map { it.toUser() }
                        .filter { it.uid != me }
                        .sortedBy { it.name.lowercase() }
                )
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    /** Adds or removes [otherUid] from my blockedUsers list. */
    fun setBlocked(otherUid: String, blocked: Boolean, onComplete: (Boolean) -> Unit = {}) {
        val uid = currentUid ?: return onComplete(false)
        val change = if (blocked) FieldValue.arrayUnion(otherUid) else FieldValue.arrayRemove(otherUid)
        users.document(uid)
            .set(mapOf("blockedUsers" to change), SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    /** Several users by uid (e.g. the Blocked Users list). Missing ones are skipped. */
    fun getUsers(uids: List<String>, onResult: (List<User>) -> Unit) {
        if (uids.isEmpty()) {
            onResult(emptyList())
            return
        }
        val tasks = uids.map { users.document(it).get() }
        Tasks.whenAllComplete(tasks).addOnCompleteListener {
            onResult(
                tasks.filter { it.isSuccessful && it.result.exists() }
                    .map { it.result.toUser() }
                    .sortedBy { it.name.lowercase() }
            )
        }
    }

    /** Compresses, uploads to profile_images/<uid>.jpg and saves the URL on my profile. */
    fun uploadProfilePhoto(
        resolver: ContentResolver,
        uri: Uri,
        onComplete: (success: Boolean, message: String?) -> Unit
    ) {
        val uid = currentUid ?: return onComplete(false, null)
        ioExecutor.execute {
            val bytes = ImageCompressor.compress(resolver, uri, maxSize = 800, quality = 85)
            mainHandler.post {
                if (bytes == null) {
                    onComplete(false, "Could not read that photo")
                    return@post
                }
                val ref = FirebaseStorage.getInstance().reference.child("profile_images/$uid.jpg")
                val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
                ref.putBytes(bytes, metadata)
                    .continueWithTask { task ->
                        task.exception?.let { throw it }
                        ref.downloadUrl
                    }
                    .continueWithTask { task ->
                        task.exception?.let { throw it }
                        users.document(uid).set(
                            mapOf("photoUrl" to task.result.toString()),
                            SetOptions.merge()
                        )
                    }
                    .addOnSuccessListener { onComplete(true, null) }
                    .addOnFailureListener { onComplete(false, it.localizedMessage) }
            }
        }
    }

    fun removeProfilePhoto(onComplete: (Boolean) -> Unit = {}) {
        val uid = currentUid ?: return onComplete(false)
        users.document(uid)
            .set(mapOf("photoUrl" to ""), SetOptions.merge())
            .addOnSuccessListener {
                FirebaseStorage.getInstance().reference.child("profile_images/$uid.jpg").delete()
                onComplete(true)
            }
            .addOnFailureListener { onComplete(false) }
    }

    /** Emails the signed-in user a link to set a new password. */
    fun sendPasswordReset(onComplete: (success: Boolean, email: String) -> Unit) {
        val email = auth.currentUser?.email.orEmpty()
        if (email.isEmpty()) return onComplete(false, email)
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onComplete(true, email) }
            .addOnFailureListener { onComplete(false, email) }
    }

    fun logout() {
        setOnline(false)
        auth.signOut()
    }

    private fun DocumentSnapshot.toUser() = User(
        uid = id,
        name = getString("name").orEmpty(),
        email = getString("email").orEmpty(),
        about = getString("about") ?: User().about,
        photoUrl = getString("photoUrl").orEmpty(),
        online = getBoolean("online") ?: false,
        lastSeen = getLong("lastSeen") ?: 0L,
        blockedUsers = (get("blockedUsers") as? List<*>)?.filterIsInstance<String>().orEmpty()
    )

    private fun User.toMap() = mapOf(
        "uid" to uid,
        "name" to name,
        "email" to email,
        "about" to about,
        "online" to online,
        "lastSeen" to lastSeen
    )

    private fun defaultNameFrom(email: String): String =
        email.substringBefore("@")
            .replace('.', ' ')
            .replace('_', ' ')
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
            .ifBlank { "User" }

    companion object {
        const val USERS = "users"
        private val ioExecutor = Executors.newSingleThreadExecutor()
        private val mainHandler = Handler(Looper.getMainLooper())
    }
}
