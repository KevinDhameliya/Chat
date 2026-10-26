package com.kevin.chatapplication.viewmodel.callsViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.kevin.chatapplication.data.model.CallLog
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.data.repository.CallRepository
import com.kevin.chatapplication.data.repository.UserRepository

/** A call entry resolved for display (the other person's current name and photo). */
data class CallRow(
    val log: CallLog,
    val otherUid: String,
    val otherName: String,
    val photoUrl: String,
    val outgoing: Boolean,
    val missed: Boolean
)

class CallsViewModel : ViewModel() {

    private val callRepository = CallRepository()
    private val userRepository = UserRepository()
    private val registrations = mutableListOf<ListenerRegistration>()

    val myUid: String = userRepository.currentUid.orEmpty()

    private var logs: List<CallLog> = emptyList()
    private var contactsById: Map<String, User> = emptyMap()
    private var missedOnly = false
    private var query = ""

    private val _rows = MutableLiveData<List<CallRow>>()
    val rows: LiveData<List<CallRow>> = _rows

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    val isMissedFilter: Boolean get() = missedOnly
    val contacts: List<User> get() = contactsById.values.sortedBy { it.name.lowercase() }

    init {
        if (myUid.isEmpty()) {
            _loading.value = false
        } else {
            registrations += callRepository.observeCalls(
                myUid,
                onChange = { list ->
                    logs = list
                    _loading.value = false
                    publish()
                },
                onError = {
                    _loading.value = false
                    publish()
                }
            )
            registrations += userRepository.observeContacts(
                onChange = { users ->
                    contactsById = users.associateBy { it.uid }
                    publish()
                },
                onError = { }
            )
        }
    }

    fun setMissedOnly(value: Boolean) {
        missedOnly = value
        publish()
    }

    fun setQuery(text: String) {
        query = text.trim()
        publish()
    }

    fun remove(row: CallRow) {
        callRepository.hideCall(row.log.id, myUid)
    }

    private fun publish() {
        _rows.value = logs
            .map { log ->
                val otherUid = log.otherUid(myUid)
                val contact = contactsById[otherUid]
                CallRow(
                    log = log,
                    otherUid = otherUid,
                    otherName = contact?.name ?: log.otherName(myUid),
                    photoUrl = contact?.photoUrl.orEmpty(),
                    outgoing = log.isOutgoing(myUid),
                    missed = log.isMissed(myUid)
                )
            }
            .filter { !missedOnly || it.missed }
            .filter { query.isBlank() || it.otherName.contains(query, ignoreCase = true) }
    }

    override fun onCleared() {
        registrations.forEach { it.remove() }
        super.onCleared()
    }
}
