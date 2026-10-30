package com.reevan.godfatherreevz.ui.home

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reevan.godfatherreevz.R
import com.reevan.godfatherreevz.ui.common.FriendCard
import com.reevan.godfatherreevz.ui.common.PastCard
import com.reevan.godfatherreevz.ui.common.TodayCard
import com.reevan.godfatherreevz.ui.theme.GodfatherTheme

/**
 * Home's scroll position, kept by the app shell: Home leaves composition while another screen is
 * on top, and coming back from a friend's page should land where the owner was, not jump.
 * [centeredFor] remembers which "today" the list was last centred on.
 */
class HomeScroll {
    val list = LazyListState()
    var centeredFor by mutableStateOf<String?>(null)
}

/** Test tag on today's card. */
const val TODAY_CARD_TAG = "today-card"

/**
 * Home: the focus stack (decision D5). Faded recent calls above, today's friend in a large
 * highlighted card, then everyone else in the order they come up. The list opens — and re-opens
 * each time today's friend changes, e.g. at midnight — with today's card centred.
 */
@Composable
fun HomeScreen(
    state: HomeState?,
    scroll: HomeScroll,
    onOpenFriend: (Long) -> Unit,
    onAddFirstFriend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val home = state ?: return
    if (home.isEmpty) {
        EmptyHome(onAddFirstFriend, modifier)
        return
    }
    val haptics = LocalHapticFeedback.current
    val open: (Long) -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        onOpenFriend(it)
    }

    // Centre today's card whenever the friend in focus (or their done state) changes.
    val todayIndex = home.past.size
    val focusKey = home.today?.let { "${it.friendId}:${it.name}:${it.done}" }
    LaunchedEffect(focusKey) {
        if (focusKey == null || scroll.centeredFor == focusKey) return@LaunchedEffect
        scroll.list.scrollToItem(todayIndex)
        val info = scroll.list.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == todayIndex }
        if (item != null) {
            val viewportCentre = (info.viewportStartOffset + info.viewportEndOffset) / 2
            scroll.list.scrollBy((item.offset + item.size / 2 - viewportCentre).toFloat())
        }
        scroll.centeredFor = focusKey
    }

    LazyColumn(
        state = scroll.list,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        itemsIndexed(home.past, key = { _, item -> "past-${item.friendId}" }) { _, item ->
            PastCard(
                name = item.name,
                date = item.date,
                onClick = { open(item.friendId) },
                modifier = Modifier
                    .animateItem()
                    .alpha(0.8f),
            )
        }
        home.today?.let { today ->
            item(key = "today") {
                TodayCard(
                    name = today.name,
                    phone = today.phone,
                    lastCalled = today.lastCalled,
                    done = today.done,
                    onClick = today.friendId?.let { id -> { open(id) } },
                    modifier = Modifier
                        .animateItem()
                        .padding(vertical = 6.dp)
                        .testTag(TODAY_CARD_TAG),
                )
            }
        }
        itemsIndexed(home.upcoming, key = { _, item -> "friend-${item.friendId}" }) { index, item ->
            FriendCard(
                name = item.name,
                lastCalled = item.lastCalled,
                onClick = { open(item.friendId) },
                label = if (index == 0) "Up next" else null,
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun EmptyHome(onAddFirstFriend: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_group),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = "No friends yet",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = GodfatherTheme.displayFont,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Add the people you want to keep in touch with. Each day, one of them is " +
                "today's call — shuffled, so nobody can guess who's next.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAddFirstFriend, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Add your first friend")
        }
    }
}
