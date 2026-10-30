package com.reevan.godfatherreevz.ui.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reevan.godfatherreevz.data.AppTheme
import com.reevan.godfatherreevz.ui.navigation.Destination
import com.reevan.godfatherreevz.ui.theme.GodfatherReevzTheme
import com.reevan.godfatherreevz.ui.theme.GodfatherTheme

/**
 * A game's main menu (decision D6, as in Reevz Habitz): the four sections as a centred stack of
 * wide, squared-off buttons with gaps between them, each outlined in the theme's accent with
 * widely spaced capitals.
 */
@Composable
fun MenuScreen(
    onOpen: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    // fillMaxSize before verticalScroll keeps the viewport height as a minimum, so the stack
    // stays vertically centred and only scrolls if it truly doesn't fit (big fonts).
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Destination.menuEntries.forEach { destination ->
            MenuButton(label = destination.label, onClick = { onOpen(destination) })
        }
    }
}

@Composable
private fun MenuButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .heightIn(min = 64.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = GodfatherTheme.displayFont,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
        )
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun MenuScreenPreview() {
    GodfatherReevzTheme(AppTheme.CORLEONE_NOIR) {
        Surface {
            MenuScreen(onOpen = {})
        }
    }
}
