package com.reevan.godfatherreevz.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reevan.godfatherreevz.GodfatherApplication
import com.reevan.godfatherreevz.data.Friend
import com.reevan.godfatherreevz.data.GodfatherDatabase
import com.reevan.godfatherreevz.data.TodayWindow
import com.reevan.godfatherreevz.photos.FriendPhotos
import com.reevan.godfatherreevz.util.TodayClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

/** Where this friend stands today. */
enum class TodayRole {
    /** Today's friend; today's call isn't made yet. */
    TO_CALL,

    /** Today's call was made, to this friend. */
    CALLED,

    /** Anyone else. */
    OTHER,
}

sealed interface DetailsState {
    /** The friend no longer exists (removed while their page was open). */
    data object Gone : DetailsState

    data class Loaded(
        val friend: Friend,
        val lastCalled: LocalDate?,
        val role: TodayRole,
        /** A call to this friend is recorded today (so Call only redials, and Undo is offered). */
        val calledToday: Boolean,
    ) : DetailsState
}

/** One friend's page. Writes go through the app-wide scope so they finish even if the page closes. */
class FriendDetailsViewModel(
    private val application: GodfatherApplication,
    private val friendId: Long,
) : ViewModel() {

    private val database = GodfatherDatabase.getInstance(application)
    private val friendDao = database.friendDao()
    private val callDao = database.callDao()

    /** Null until loaded. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DetailsState?> = TodayClock.instance.today
        .flatMapLatest { today ->
            val window = TodayWindow.of(today)
            combine(
                friendDao.observe(friendId),
                friendDao.observeInCallOrder().map { it.firstOrNull()?.friend?.id }.distinctUntilChanged(),
                callDao.observeScheduledBetween(window.start, window.endInclusive),
                callDao.observeCallOn(friendId, today),
            ) { friend, headId, scheduled, callToday ->
                if (friend == null) {
                    DetailsState.Gone
                } else {
                    val role = when {
                        scheduled != null && scheduled.friendId == friendId -> TodayRole.CALLED
                        scheduled == null && headId == friendId -> TodayRole.TO_CALL
                        else -> TodayRole.OTHER
                    }
                    DetailsState.Loaded(friend.friend, friend.lastCalled, role, calledToday = callToday != null)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Counts a call to this friend today. Idempotent: a second press the same day changes nothing
     * (FriendDao.recordCall). Today's date is read here, at the press.
     */
    fun recordCall() {
        val today = TodayClock.instance.today.value
        application.appScope.launch { friendDao.recordCall(friendId, today, Random.Default) }
    }

    /** Undoes today's call to this friend; [onUndone] gets whether there was one to undo. */
    fun undoCall(onUndone: (Boolean) -> Unit) {
        val today = TodayClock.instance.today.value
        application.appScope.launch {
            val undone = friendDao.undoCall(friendId, today, Random.Default)
            viewModelScope.launch { onUndone(undone) }
        }
    }

    fun saveNotes(notes: String) {
        application.appScope.launch { friendDao.updateNotes(friendId, notes) }
    }

    /** Sets this friend's photo (null removes it); the file it replaces is deleted. */
    fun setPhoto(photo: String?) {
        application.appScope.launch {
            val old = friendDao.replacePhoto(friendId, photo)
            if (old != photo) FriendPhotos.delete(application, old)
        }
    }
}
