package com.reevan.godfatherreevz.ui.addfriend

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.net.toUri
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
import com.reevan.godfatherreevz.util.PhoneNumbers
import com.reevan.godfatherreevz.util.TodayClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** What one add did: who went in, and how many were skipped as already saved. */
data class AddResult(val added: List<String>, val alreadySaved: List<String>) {

    /** "Marco Rossi added", "5 friends added · 1 already saved", "Already saved as Marco Rossi". */
    fun message(): String {
        val addedPart = when (added.size) {
            0 -> null
            1 -> "${added.single()} added"
            else -> "${added.size} friends added"
        }
        val skippedPart = when {
            alreadySaved.isEmpty() -> null
            addedPart == null && alreadySaved.size == 1 -> "Already saved as ${alreadySaved.single()}"
            else -> "${alreadySaved.size} already saved"
        }
        return listOfNotNull(addedPart, skippedPart).joinToString(" · ").ifEmpty { "Nothing to add" }
    }
}

/**
 * Splits [candidates] into friends to add and those whose number is already saved — in [existing]
 * or earlier in [candidates] itself (two contacts sharing one number). The skipped list holds the
 * names they are already saved under.
 */
fun planAdd(candidates: List<NewFriend>, existing: List<Friend>): Pair<List<NewFriend>, List<String>> {
    val toAdd = mutableListOf<NewFriend>()
    val skipped = mutableListOf<String>()
    for (candidate in candidates) {
        val saved = existing.firstOrNull { PhoneNumbers.sameNumber(it.phone, candidate.phone) }?.name
            ?: toAdd.firstOrNull { PhoneNumbers.sameNumber(it.phone, candidate.phone) }?.name
        if (saved != null) skipped += saved else toAdd += candidate
    }
    return toAdd to skipped
}

/** A friend picked from the phonebook, with their contact photo (a contacts-provider URI), if any. */
data class ContactPick(val friend: NewFriend, val contactPhoto: String?)

/** Adds friends — typed in, picked from the importer, or from the system picker. */
class AddFriendViewModel(
    private val application: Application,
    private val friendDao: FriendDao,
) : ViewModel() {

    /** Every saved friend, A–Z, for the duplicate checks; null until loaded. */
    val friends: StateFlow<List<Friend>?> = friendDao.observeAlphabetical()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val submissions = SubmissionTracker()
    private val results = mutableStateMapOf<String, AddResult>()

    fun resultOf(token: String): AddResult? = results[token]

    /**
     * Adds [candidates], skipping numbers already saved (re-checked here against the database, not
     * the screen's copy). Each new friend is shuffled into the rotation by the new-friend rule.
     */
    fun add(token: String, candidates: List<NewFriend>) = addPicks(token, candidates.map { ContactPick(it, null) })

    /**
     * Adds friends picked from the phonebook, as [add] does — each with a copy of their contact
     * photo, so it carries over.
     */
    fun addPicks(token: String, picks: List<ContactPick>) {
        viewModelScope.launch {
            val (toAdd, skipped) = planAdd(picks.map { it.friend }, friendDao.getAll())
            if (toAdd.isNotEmpty()) {
                val contactPhotos = picks.associate { it.friend to it.contactPhoto }
                val withPhotos = withContext(Dispatchers.IO) {
                    toAdd.map { friend ->
                        val contactPhoto = contactPhotos[friend]
                        if (friend.photo != null || contactPhoto == null) {
                            friend
                        } else {
                            // Without contacts access (the system picker) it usually can't be read.
                            val copy = runCatching { FriendPhotos.copyContactPhoto(application, contactPhoto.toUri()) }
                            friend.copy(photo = copy.getOrNull())
                        }
                    }
                }
                friendDao.addFriends(
                    friends = withPhotos,
                    today = TodayClock.instance.today.value,
                    createdAt = System.currentTimeMillis(),
                    random = Random.Default,
                )
            }
            results[token] = AddResult(added = toAdd.map { it.name }, alreadySaved = skipped)
            submissions.complete(token)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                AddFriendViewModel(application, GodfatherDatabase.getInstance(application).friendDao())
            }
        }
    }
}
