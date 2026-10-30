package com.reevan.godfatherreevz.ui.friendform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.reevan.godfatherreevz.util.capitalizeWords

/**
 * The layout Add Friend and the Edit Friend editor share: a scrolling form above a fixed bar with
 * the one action at the bottom right — in thumb reach, and riding up above the keyboard.
 */
@Composable
fun FriendFormScaffold(
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    form: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().imePadding()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            content = form,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Button(
                onClick = onAction,
                enabled = actionEnabled,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * The friend's name, shown with every word's first letter capitalised as it is typed.
 *
 * The capitalisation is display-only ([OutputTransformation]); the saved name gets it from
 * [FriendDraft.toNewFriend]. Changing the text itself while the keyboard is mid-word knocks the
 * keyboard out of sync and drops keystrokes (found in Reevz Habitz); an output transformation
 * never touches the text the keyboard owns.
 */
@Composable
fun FriendNameField(state: TextFieldState, modifier: Modifier = Modifier) {
    OutlinedTextField(
        state = state,
        inputTransformation = InputTransformation.maxLength(MAX_NAME_LENGTH),
        outputTransformation = CapitalizeWords,
        label = { Text("Name") },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun FriendPhoneField(state: TextFieldState, error: String?, modifier: Modifier = Modifier) {
    OutlinedTextField(
        state = state,
        inputTransformation = InputTransformation.maxLength(MAX_PHONE_LENGTH),
        label = { Text("Phone number") },
        lineLimits = TextFieldLineLimits.SingleLine,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** 15 digits plus a generous allowance for spaces, dashes and brackets. */
private const val MAX_PHONE_LENGTH = 25

/** Draws the text through [capitalizeWords], replacing only the letters it changes. */
private val CapitalizeWords = OutputTransformation {
    val current = asCharSequence().toString()
    val capitalized = capitalizeWords(current)
    for (i in current.indices) {
        if (current[i] != capitalized[i]) replace(i, i + 1, capitalized[i].toString())
    }
}
