package com.reevan.godfatherreevz.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reevan.godfatherreevz.R
import com.reevan.godfatherreevz.data.AppTheme
import com.reevan.godfatherreevz.ui.theme.GodfatherReevzTheme
import com.reevan.godfatherreevz.ui.theme.GodfatherTheme

/**
 * The header bar's own height, excluding the status bar. Thin — Material's TopAppBar is 64dp — but
 * no thinner than the 48dp an icon button needs to stay an accessible touch target.
 */
private val HeaderHeight = 48.dp

/** Home's header: the app's name and today's date on the left, actions on the right. */
@Composable
fun HomeHeader(
    date: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    HeaderBar(modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Text(
                text = "Godfather Reevz",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = GodfatherTheme.displayFont,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        actions()
    }
}

/**
 * Every other screen's header: a back arrow, then the path to this screen, e.g.
 * `Home › Menu › Add Friend`, then the actions. The current screen is emphasised; each earlier
 * crumb is a button that jumps straight back to that screen ([onCrumbClick] gets its index).
 *
 * If the trail is wider than the space it scrolls instead of being cut off, and starts scrolled
 * to the end so the current screen's name is the part in view.
 */
@Composable
fun BreadcrumbHeader(
    crumbs: List<String>,
    onBack: () -> Unit,
    onCrumbClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val trailColor = MaterialTheme.colorScheme.onSurfaceVariant
    HeaderBar(modifier) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                // reverseScrolling: scroll position 0 is the *end* of the trail.
                .horizontalScroll(rememberScrollState(), reverseScrolling = true)
                .padding(end = 4.dp)
                .testTag(BREADCRUMBS_TAG),
        ) {
            crumbs.forEachIndexed { index, crumb ->
                if (index == crumbs.lastIndex) {
                    Text(
                        text = crumb,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .semantics { heading() },
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .heightIn(min = HeaderHeight)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Go to $crumb",
                            ) { onCrumbClick(index) }
                            .padding(horizontal = 6.dp),
                    ) {
                        Text(
                            text = crumb,
                            style = MaterialTheme.typography.titleSmall,
                            color = trailColor,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.titleSmall,
                        color = trailColor,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
            }
        }
        actions()
    }
}

/**
 * The Settings and Menu buttons every header carries on its right (the owner's brief: "the entire
 * app will have a header at the top with a settings button and a menu button").
 */
@Composable
fun HeaderActions(onSettings: () -> Unit, onMenu: () -> Unit) {
    IconButton(onClick = onSettings) {
        Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings")
    }
    IconButton(onClick = onMenu) {
        Icon(painterResource(R.drawable.ic_menu), contentDescription = "Menu")
    }
}

/** Test tag on the breadcrumb trail. */
const val BREADCRUMBS_TAG = "breadcrumbs"

/** Shared frame: draws behind the status bar, then a thin divider underneath. */
@Composable
private fun HeaderBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    // A minimum, not a fixed height, so a large system font grows the bar
                    // instead of clipping the title or breadcrumb.
                    .heightIn(min = HeaderHeight)
                    .padding(horizontal = 4.dp),
                content = content,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Preview
@Composable
private fun HomeHeaderPreview() {
    GodfatherReevzTheme {
        HomeHeader(date = "Thu, 29 October") {
            HeaderActions(onSettings = {}, onMenu = {})
        }
    }
}

@Preview
@Composable
private fun BreadcrumbHeaderPreview() {
    GodfatherReevzTheme(AppTheme.CORLEONE_NOIR) {
        BreadcrumbHeader(
            crumbs = listOf("Home", "Menu", "Add Friend"),
            onBack = {},
            onCrumbClick = {},
        ) {
            HeaderActions(onSettings = {}, onMenu = {})
        }
    }
}
