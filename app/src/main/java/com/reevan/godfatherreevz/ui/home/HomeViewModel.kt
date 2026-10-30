package com.reevan.godfatherreevz.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.godfatherreevz.data.CallDao
import com.reevan.godfatherreevz.data.FriendDao
import com.reevan.godfatherreevz.data.GodfatherDatabase
import com.reevan.godfatherreevz.data.TodayWindow
import com.reevan.godfatherreevz.util.TodayClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** Home's focus stack, rebuilt whenever friends, calls or "today" change. */
class HomeViewModel(
    friendDao: FriendDao,
    callDao: CallDao,
) : ViewModel() {

    /** Null until the first read, so Home never flashes its empty state. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeState?> = TodayClock.instance.today
        .flatMapLatest { today ->
            val window = TodayWindow.of(today)
            combine(
                friendDao.observeInCallOrder(),
                // Enough history to find PAST_CARDS distinct friends even with repeats.
                callDao.observeRecent(today, limit = PAST_CARDS * 6),
                callDao.observeScheduledBetween(window.start, window.endInclusive),
            ) { friends, recent, scheduled -> buildHome(friends, recent, scheduled) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val database = GodfatherDatabase.getInstance(application)
                HomeViewModel(database.friendDao(), database.callDao())
            }
        }
    }
}
