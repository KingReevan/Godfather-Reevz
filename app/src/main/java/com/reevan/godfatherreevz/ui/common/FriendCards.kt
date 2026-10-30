package com.reevan.godfatherreevz.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reevan.godfatherreevz.R
import com.reevan.godfatherreevz.data.AppTheme
import com.reevan.godfatherreevz.ui.theme.GodfatherReevzTheme
import com.reevan.godfatherreevz.ui.theme.GodfatherTheme
import com.reevan.godfatherreevz.util.formatCallDate
import com.reevan.godfatherreevz.util.formatLastCalled
import java.time.LocalDate

/*
 * Home's three kinds of card — all wide "horizontal" cards stacked on top of each other, each with
 * a date in its bottom-right corner (the brief):
 * - TodayCard: today's friend, larger, in the theme's gradient, "highlighted and in focus".
 * - FriendCard: everyone else, in upcoming order, with their last-called date or "-".
 * - PastCard: the faded record of a recent call, with that call's date.
 */

private val CardShape = RoundedCornerShape(18.dp)

/**
 * Today's friend. [done] once today's call is made: the pulse stops and it reads "Called today".
 * [onClick] null for a friend who has since been removed (only their saved name is left).
 */
@Composable
fun TodayCard(
    name: String,
    phone: String?,
    lastCalled: LocalDate?,
    done: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = GodfatherTheme.colors
    val glow = if (done) {
        0f
    } else {
        rememberInfiniteTransition(label = "today glow").animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
            label = "today glow alpha",
        ).value
    }
    val dateLabel = formatLastCalled(lastCalled)
    val shape = RoundedCornerShape(24.dp)
    val content: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(colors.focusStart, colors.focusEnd)))
                .fillMaxWidth()
                .heightIn(min = 168.dp)
                .padding(20.dp),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Chip(
                    text = if (done) "✓  CALLED TODAY" else "TODAY",
                    container = colors.onFocus,
                    content = colors.focusStart.takeIf { !done } ?: colors.focusEnd,
                )
                Spacer(Modifier.heightIn(min = 14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!done) {
                        Icon(
                            painter = painterResource(R.drawable.ic_call),
                            contentDescription = null,
                            tint = colors.onFocus,
                            modifier = Modifier.size(26.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = GodfatherTheme.displayFont,
                        fontWeight = FontWeight.Bold,
                        color = colors.onFocus,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (phone != null) {
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onFocus,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .alpha(0.85f),
                    )
                }
                Spacer(Modifier.heightIn(min = 18.dp))
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onFocus,
                    modifier = Modifier
                        .align(Alignment.End)
                        .semantics {
                            contentDescription =
                                if (lastCalled == null) "Never called" else "Last called $dateLabel"
                        },
                )
            }
        }
    }
    val border = BorderStroke(3.dp, colors.focusGlow.copy(alpha = glow))
    if (onClick != null) {
        Card(
            onClick = onClick,
            shape = shape,
            border = if (done) null else border,
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = modifier.fillMaxWidth(),
        ) { content() }
    } else {
        Card(
            shape = shape,
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = modifier.fillMaxWidth(),
        ) { content() }
    }
}

/** Anyone who isn't today's friend, in upcoming order. [label] marks the next one up. */
@Composable
fun FriendCard(
    name: String,
    lastCalled: LocalDate?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val dateLabel = formatLastCalled(lastCalled)
    Card(
        onClick = onClick,
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 76.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Initials(name)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (label != null) {
                    Chip(
                        text = label.uppercase(),
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = GodfatherTheme.displayFont,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.End)
                        .semantics {
                            contentDescription =
                                if (lastCalled == null) "Never called" else "Last called $dateLabel"
                        },
                )
            }
        }
    }
}

/** A recent call, faded: who and when. */
@Composable
fun PastCard(
    name: String,
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GodfatherTheme.colors
    val dateLabel = formatCallDate(date)
    Card(
        onClick = onClick,
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = colors.pastCard, contentColor = colors.onPastCard),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = GodfatherTheme.displayFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .semantics { contentDescription = "Called $dateLabel" },
            )
        }
    }
}

/** A round badge with the friend's initials. */
@Composable
fun Initials(name: String, modifier: Modifier = Modifier, size: Int = 44) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.size(size.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initialsOf(name),
                fontFamily = GodfatherTheme.displayFont,
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.38f).sp,
            )
        }
    }
}

/** "Marco Rossi" → "MR", "cher" → "C", "" → "?". */
fun initialsOf(name: String): String {
    val letters = name.trim().split(Regex("\\s+"))
        .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() } }
    return when {
        letters.isEmpty() -> "?"
        letters.size == 1 -> letters.first().uppercase()
        else -> (letters.first().toString() + letters.last()).uppercase()
    }
}

@Composable
private fun Chip(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = content,
        modifier = modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Preview(widthDp = 380)
@Composable
private fun CardsPreview() {
    GodfatherReevzTheme {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PastCard("Neha Joseph", LocalDate.of(2026, 10, 27), onClick = {})
                TodayCard("Marco Rossi", "+91 98765 43210", null, done = false, onClick = {})
                FriendCard("Aditya Kumar", null, onClick = {}, label = "Up next")
                FriendCard("Priya Sharma", LocalDate.of(2026, 9, 3), onClick = {})
            }
        }
    }
}

@Preview(widthDp = 380)
@Composable
private fun CardsNoirPreview() {
    GodfatherReevzTheme(AppTheme.CORLEONE_NOIR) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TodayCard("Marco Rossi", "+91 98765 43210", LocalDate.of(2026, 10, 30), done = true, onClick = {})
                FriendCard("Aditya Kumar", null, onClick = {})
            }
        }
    }
}

@Preview(widthDp = 380)
@Composable
private fun CardsNothingPreview() {
    GodfatherReevzTheme(AppTheme.NOTHING) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TodayCard("Marco Rossi", "+91 98765 43210", null, done = false, onClick = {})
                FriendCard("Aditya Kumar", LocalDate.of(2026, 9, 3), onClick = {}, label = "Up next")
            }
        }
    }
}
