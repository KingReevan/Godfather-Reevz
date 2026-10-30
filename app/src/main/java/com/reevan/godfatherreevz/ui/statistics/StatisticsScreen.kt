package com.reevan.godfatherreevz.ui.statistics

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.godfatherreevz.R
import com.reevan.godfatherreevz.data.GodfatherDatabase
import com.reevan.godfatherreevz.ui.theme.GodfatherTheme
import com.reevan.godfatherreevz.util.TodayClock
import com.reevan.godfatherreevz.util.formatCallDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

class StatisticsViewModel(application: Application) : ViewModel() {
    private val database = GodfatherDatabase.getInstance(application)

    val stats: StateFlow<CallStats?> = combine(
        TodayClock.instance.today,
        database.friendDao().observeInCallOrder(),
        database.callDao().observeAll(),
    ) { today, friends, calls -> callStats(calls.map { it.date }, friends, today) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StatisticsViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
            }
        }
    }
}

/** Simple stats (the brief): totals, this month, streaks, who's waiting longest, recent months. */
@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.Factory),
) {
    val loaded by viewModel.stats.collectAsStateWithLifecycle()
    val stats = loaded ?: return

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("Friends", stats.friends.toString(), R.drawable.ic_group, Modifier.weight(1f))
            Tile("Calls made", stats.totalCalls.toString(), R.drawable.ic_call, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile(
                label = "Current streak",
                value = days(stats.currentStreak),
                icon = R.drawable.ic_streak,
                modifier = Modifier.weight(1f),
                highlight = stats.currentStreak > 0,
            )
            Tile("Longest streak", days(stats.longestStreak), R.drawable.ic_history, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile(
                label = "This month",
                value = "${stats.daysCalledThisMonth} / ${stats.daysSoFarThisMonth}",
                icon = R.drawable.ic_calendar,
                modifier = Modifier.weight(1f),
                caption = "days with a call",
            )
            Tile("Never called", stats.neverCalled.toString(), R.drawable.ic_hourglass, Modifier.weight(1f))
        }
        MonthCard(stats)
        if (stats.waitingLongest.isNotEmpty()) WaitingCard(stats.waitingLongest)
        MonthsCard(stats.lastSixMonths)
    }
}

private fun days(n: Int) = if (n == 1) "1 day" else "$n days"

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = GodfatherTheme.displayFont,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
private fun Tile(
    label: String,
    value: String,
    icon: Int,
    modifier: Modifier = Modifier,
    caption: String? = null,
    highlight: Boolean = false,
) {
    val colors = if (highlight) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = colors,
        modifier = modifier
            .heightIn(min = 104.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = GodfatherTheme.displayFont,
                fontWeight = FontWeight.Bold,
            )
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** This month as a little calendar: filled days had a call, outlined days were missed. */
@Composable
private fun MonthCard(stats: CallStats) {
    val locale = Locale.getDefault()
    val title = stats.month.month.getDisplayName(TextStyle.FULL, locale) + " " + stats.month.year
    Section(title) {
        val leading = stats.month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
        val cells: List<DayMark?> = List(leading) { null } + stats.monthDays
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { dow ->
                Text(
                    text = dow.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(7).forEachIndexed { weekIndex, week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEachIndexed { i, mark ->
                    val dayOfMonth = weekIndex * 7 + i - leading + 1
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (mark != null) DayDot(mark, dayOfMonth)
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Legend()
    }
}

/** A day of the month strip; [dayOfMonth] null for the small legend dots. */
@Composable
private fun DayDot(mark: DayMark, dayOfMonth: Int? = null) {
    val scheme = MaterialTheme.colorScheme
    val description = when (mark) {
        DayMark.CALLED -> "called"
        DayMark.MISSED -> "no call"
        DayMark.TODAY_PENDING -> "today, not called yet"
        DayMark.FUTURE -> "still to come"
        DayMark.BEFORE_START -> "before you started"
    }
    val base = Modifier
        .size(if (dayOfMonth == null) 14.dp else 32.dp)
        .clearAndSetSemantics { contentDescription = dayOfMonth?.let { "$it, $description" } ?: description }
    val (shape, numberColor) = when (mark) {
        DayMark.CALLED -> base.background(scheme.primary, CircleShape) to scheme.onPrimary
        DayMark.MISSED -> base.border(1.5.dp, scheme.outline, CircleShape) to scheme.onSurfaceVariant
        DayMark.TODAY_PENDING -> base.border(2.5.dp, scheme.primary, CircleShape) to scheme.primary
        DayMark.FUTURE, DayMark.BEFORE_START -> base to scheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    Box(shape, contentAlignment = Alignment.Center) {
        if (dayOfMonth != null) {
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (mark == DayMark.FUTURE || mark == DayMark.BEFORE_START) FontWeight.Normal else FontWeight.Bold,
                color = numberColor,
            )
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(DayMark.CALLED, "Called")
        LegendItem(DayMark.MISSED, "No call")
        LegendItem(DayMark.TODAY_PENDING, "Today")
    }
}

@Composable
private fun LegendItem(mark: DayMark, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DayDot(mark)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WaitingCard(waiting: List<Waiting>) {
    Section("Longest since a call") {
        waiting.forEach { friend ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${formatCallDate(friend.lastCalled)} · ${ago(friend.daysAgo)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun ago(days: Long) = when (days) {
    0L -> "today"
    1L -> "yesterday"
    else -> "$days days ago"
}

/** Calls per month for the last six months, as simple bars. */
@Composable
private fun MonthsCard(months: List<MonthCount>) {
    val locale = Locale.getDefault()
    val most = months.maxOf { it.calls }.coerceAtLeast(1)
    Section("Last six months") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            months.forEach { month ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics(mergeDescendants = true) {},
                ) {
                    Text(month.calls.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.75f * month.calls / most)
                            .heightIn(min = 3.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = month.month.month.getDisplayName(TextStyle.SHORT, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
