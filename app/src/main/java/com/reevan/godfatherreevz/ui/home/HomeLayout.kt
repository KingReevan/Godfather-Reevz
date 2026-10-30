package com.reevan.godfatherreevz.ui.home

import com.reevan.godfatherreevz.data.Call
import com.reevan.godfatherreevz.data.FriendWithLastCall
import java.time.LocalDate

/** A faded card above today's: a recent call, dated. */
data class PastItem(val friendId: Long, val name: String, val date: LocalDate, val photo: String? = null)

/**
 * Today's card. [friendId] is null only when today's call was made to a friend who has since been
 * removed — the card then shows their saved name, done, and can't be opened.
 */
data class TodayItem(
    val friendId: Long?,
    val name: String,
    val phone: String?,
    val lastCalled: LocalDate?,
    val done: Boolean,
    val photo: String? = null,
)

/** Everyone else, in the order they come up. */
data class UpcomingItem(val friendId: Long, val name: String, val lastCalled: LocalDate?, val photo: String? = null)

/** Home's focus stack, top to bottom: past calls, today's friend, everyone else. */
data class HomeState(
    val past: List<PastItem>,
    val today: TodayItem?,
    val upcoming: List<UpcomingItem>,
) {
    val isEmpty: Boolean get() = today == null && upcoming.isEmpty()
}

/** How many recent calls the faded past section shows. */
const val PAST_CARDS = 7

/**
 * Lays out Home (decision D5) from the friends in call order, the most recent calls (newest
 * first) and today's scheduled call, if made.
 *
 * - Today's friend is the friend of today's scheduled call once it is made (they stay in focus
 *   until midnight), else the head of the rotation.
 * - Upcoming is every other friend, each once, in call order — "all the names", per the brief.
 * - The past section is call *history* — the last [PAST_CARDS] calls, one card per friend, oldest
 *   at the top — not "everyone called this pass". So nothing ever visibly resets when a pass ends:
 *   the owner can't tell when repetition starts (the brief). Removed friends and today's friend
 *   are left out.
 */
fun buildHome(
    friends: List<FriendWithLastCall>,
    recentCalls: List<Call>,
    scheduledToday: Call?,
): HomeState {
    val byId = friends.associateBy { it.friend.id }
    val today = when {
        scheduledToday != null -> {
            val friend = scheduledToday.friendId?.let(byId::get)
            TodayItem(
                friendId = friend?.friend?.id,
                name = friend?.friend?.name ?: scheduledToday.friendName,
                phone = friend?.friend?.phone,
                lastCalled = friend?.lastCalled ?: scheduledToday.date,
                done = true,
                photo = friend?.friend?.photo,
            )
        }
        friends.isNotEmpty() -> friends.first().let {
            TodayItem(it.friend.id, it.friend.name, it.friend.phone, it.lastCalled, done = false, photo = it.friend.photo)
        }
        else -> null
    }
    val upcoming = friends
        .filter { it.friend.id != today?.friendId }
        .map { UpcomingItem(it.friend.id, it.friend.name, it.lastCalled, it.friend.photo) }
    val past = recentCalls
        .filter { call -> call.friendId != null && call.friendId in byId && call.friendId != today?.friendId }
        .distinctBy { it.friendId }
        .take(PAST_CARDS)
        .map { call -> byId.getValue(call.friendId!!).friend.let { PastItem(it.id, it.name, call.date, it.photo) } }
        .reversed()
    return HomeState(past = past, today = today, upcoming = upcoming)
}
