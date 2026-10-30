package com.reevan.godfatherreevz.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.godfatherreevz.data.AppTheme
import com.reevan.godfatherreevz.ui.theme.GodfatherReevzTheme
import com.reevan.godfatherreevz.ui.theme.GodfatherTheme
import com.reevan.godfatherreevz.ui.theme.colorScheme
import com.reevan.godfatherreevz.ui.theme.displayFont
import com.reevan.godfatherreevz.ui.theme.godfatherColors

/** Settings: theme, how Call works, contacts access, Shuffle again, and about. */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    preferences: PreferencesViewModel = viewModel(factory = PreferencesViewModel.Factory),
) {
    val settings by preferences.settings.collectAsStateWithLifecycle()
    // MainActivity doesn't draw until settings have loaded, so this is never null in practice.
    val current = settings ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        SectionTitle("Theme")
        ThemePicker(selected = current.theme, onSelect = preferences::setTheme)
        SectionDivider()
        SectionTitle("Calling")
        CallingSection(callDirectly = current.callDirectly, onCallDirectlyChange = preferences::setCallDirectly)
        SectionDivider()
        SectionTitle("Contacts")
        ContactsSection()
        SectionDivider()
        SectionTitle("Order")
        OrderSection()
        SectionDivider()
        SectionTitle("About")
        AboutSection()
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

@Composable
internal fun SectionDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun ThemePicker(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    Column(Modifier.selectableGroup()) {
        AppTheme.entries.forEach { theme ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .selectable(
                        selected = theme == selected,
                        onClick = { onSelect(theme) },
                        role = Role.RadioButton,
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                // The row handles the click, so the button itself must not be a second target.
                RadioButton(selected = theme == selected, onClick = null)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = theme.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = theme.displayFont(),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (theme.isDark) "Dark" else "Light",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ThemeSwatch(theme)
            }
        }
    }
}

/** A thumbnail of a theme: its background, today's-card gradient and accent. */
@Composable
private fun ThemeSwatch(theme: AppTheme) {
    val scheme = theme.colorScheme()
    val extra = theme.godfatherColors()
    val shape = RoundedCornerShape(8.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(scheme.background, shape)
            .padding(6.dp)
            .width(64.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
                .background(Brush.linearGradient(listOf(extra.focusStart, extra.focusEnd)), RoundedCornerShape(4.dp)),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(scheme.primary, CircleShape))
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(scheme.surfaceContainerHigh, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun ThemePickerPreview() {
    GodfatherReevzTheme {
        Surface {
            Column {
                SectionTitle("Theme")
                ThemePicker(selected = AppTheme.SICILIAN_SUNSET, onSelect = {})
                Text("Display font", fontFamily = GodfatherTheme.displayFont)
            }
        }
    }
}
