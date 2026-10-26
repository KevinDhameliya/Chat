package com.kevin.chatapplication.viewmodel.statusViewerViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.kevin.chatapplication.data.model.StatusItem
import com.kevin.chatapplication.data.model.User
import com.kevin.chatapplication.data.repository.StatusRepository
import com.kevin.chatapplication.data.repository.UserRepository

class StatusViewerViewModel : ViewModel() {

    private val statusRepository = StatusRepository()
    private val userRepository = UserRepository()
    private var registration: ListenerRegistration? = null
    private var started = false

    val myUid: String = userRepository.currentUid.orEmpty()
    var ownerUid = ""
        private set
    val isMine: Boolean get() = ownerUid == myUid

    /** Index of the photo on screen; survives rotation. -1 = not chosen yet. */
    var currentIndex = -1

    private val _items = MutableLiveData<List<StatusItem>>()
    val items: LiveData<List<StatusItem>> = _items

    private val _owner = MutableLiveData<User?>()
    val owner: LiveData<User?> = _owner

    fun start(ownerUid: String) {
        if (started) return
        started = true
        this.ownerUid = ownerUid
        userRepository.getUser(ownerUid) { _owner.value = it }
        registration = statusRepository.observeItems(ownerUid) { _items.value = it }
    }

    /** Adds me to the viewers of someone else's photo (once). */
    fun markViewed(item: StatusItem) {
        if (!isMine && myUid.isNotEmpty() && myUid !in item.viewers) {
            statusRepository.markViewed(item, myUid)
        }
    }

    fun delete(item: StatusItem, onDone: (Boolean) -> Unit) = statusRepository.deleteItem(item, onDone)

    fun loadViewers(item: StatusItem, onResult: (List<User>) -> Unit) =
        userRepository.getUsers(item.viewers, onResult)

    override fun onCleared() {
        registration?.remove()
        super.onCleared()
    }
}
