package com.reevan.godfatherreevz.ui.editfriend

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.godfatherreevz.data.Friend
import com.reevan.godfatherreevz.data.FriendDao
import com.reevan.godfatherreevz.data.GodfatherDatabase
import com.reevan.godfatherreevz.data.NewFriend
import com.reevan.godfatherreevz.photos.FriendPhotos
import com.reevan.godfatherreevz.ui.common.SubmissionTracker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Edit Friend: the list, and the editor's load and save. */
class EditFriendViewModel(
    private val application: Application,
    private val friendDao: FriendDao,
) : ViewModel() {

    /** Every friend A–Z; null until loaded. Also the editor's duplicate-number check. */
    val friends: StateFlow<List<Friend>?> = friendDao.observeAlphabetical()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val submissions = SubmissionTracker()

    suspend fun load(id: Long): Friend? = friendDao.get(id)

    /**
     * Saves [original]'s new photo, name and number — an in-place UPDATE, so their place in the
     * rotation, call history and notes stay exactly as they were. A replaced photo is deleted.
     */
    fun save(token: String, original: Friend, edited: NewFriend) {
        viewModelScope.launch {
            friendDao.updateDetails(original.id, edited.name, edited.phone, edited.photo)
            if (edited.photo != original.photo) FriendPhotos.delete(application, original.photo)
            submissions.complete(token)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                EditFriendViewModel(application, GodfatherDatabase.getInstance(application).friendDao())
            }
        }
    }
}
