package com.reevan.godfatherreevz.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reevan.godfatherreevz.ui.addfriend.AddFriendScreen
import com.reevan.godfatherreevz.ui.common.AppMessages
import com.reevan.godfatherreevz.ui.common.BreadcrumbHeader
import com.reevan.godfatherreevz.ui.common.ComingSoon
import com.reevan.godfatherreevz.ui.common.HeaderActions
import com.reevan.godfatherreevz.ui.common.HomeHeader
import com.reevan.godfatherreevz.ui.common.LocalAppMessages
import com.reevan.godfatherreevz.ui.contactpicker.ContactImportScreen
import com.reevan.godfatherreevz.ui.menu.MenuScreen
import com.reevan.godfatherreevz.ui.navigation.BackStackSaver
import com.reevan.godfatherreevz.ui.navigation.Destination
import com.reevan.godfatherreevz.ui.navigation.InitialBackStack
import com.reevan.godfatherreevz.ui.navigation.LeaveGuard
import com.reevan.godfatherreevz.ui.navigation.breadcrumbs
import com.reevan.godfatherreevz.ui.navigation.jumpTo
import com.reevan.godfatherreevz.ui.navigation.pop
import com.reevan.godfatherreevz.ui.navigation.popIfCurrent
import com.reevan.godfatherreevz.ui.navigation.popTo
import com.reevan.godfatherreevz.ui.navigation.pushFrom
import com.reevan.godfatherreevz.ui.settings.SettingsScreen
import com.reevan.godfatherreevz.util.TodayClock
import com.reevan.godfatherreevz.util.formatHeaderDate

/**
 * App shell: owns the back stack, picks the header for the current screen, and renders that
 * screen below it.
 *
 * Every header carries the Settings and Menu buttons on the right; they lead to that screen
 * straight above Home. Home's header shows the app's name and today's date; every other screen
 * gets a back arrow and a breadcrumb whose earlier crumbs jump straight back. System back pops one
 * screen; from Home it leaves the app as usual.
 */
@Composable
fun GodfatherApp() {
    var backStack by rememberSaveable(stateSaver = BackStackSaver) {
        mutableStateOf(InitialBackStack)
    }
    val current = backStack.last()
    val today by TodayClock.instance.today.collectAsStateWithLifecycle()

    // Opens a screen from the one showing in this composition; ignored if that one is no longer
    // on top (a second tap in the same frame).
    val navigate: (Destination) -> Unit = { backStack = backStack.pushFrom(current, it) }

    // Leaving the screen on top for [target] — system back, the header arrow, a breadcrumb, or
    // the header's Settings / Menu buttons. The screen may hold it up (the Edit Friend editor, to
    // ask before discarding changes) and later approve it.
    val leaveGuard = remember { LeaveGuard() }
    val leaveTo: (List<Destination>) -> Unit = { target ->
        if (target != backStack && !leaveGuard.intercepts(target)) backStack = target
    }

    // A screen finishing its own job goes back to where it came from — but only if it is still on
    // top, so a breadcrumb tapped while it saved is never overruled.
    val finish: (Destination) -> () -> Unit = { screen ->
        { backStack = backStack.popIfCurrent(screen) }
    }

    BackHandler(enabled = backStack.size > 1) { leaveTo(backStack.pop()) }

    val snackbarHost = remember { SnackbarHostState() }
    val shellScope = rememberCoroutineScope()
    val messages = remember(snackbarHost, shellScope) { AppMessages(snackbarHost, shellScope) }

    val actions: @Composable () -> Unit = {
        HeaderActions(
            onSettings = { leaveTo(backStack.jumpTo(Destination.Settings)) },
            onMenu = { leaveTo(backStack.jumpTo(Destination.Menu)) },
        )
    }

    CompositionLocalProvider(LocalAppMessages provides messages) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Lifted above the bottom action bar most screens have (Add, Call, Remove...), so a
        // message never covers the button the owner is about to tap again.
        snackbarHost = { SnackbarHost(snackbarHost, Modifier.padding(bottom = 76.dp)) },
        topBar = {
            if (current == Destination.Home) {
                HomeHeader(date = formatHeaderDate(today)) { actions() }
            } else {
                BreadcrumbHeader(
                    crumbs = backStack.breadcrumbs(),
                    onBack = { leaveTo(backStack.pop()) },
                    onCrumbClick = { index -> leaveTo(backStack.popTo(index)) },
                ) { actions() }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Marks the header and system bar insets as handled, so a screen's own
                // imePadding() adds only the keyboard height beyond them, not the bars twice.
                .consumeWindowInsets(innerPadding),
        ) {
            when (current) {
                Destination.Home -> ComingSoon("Home", phase = 4)
                Destination.Menu -> MenuScreen(onOpen = navigate)
                Destination.Settings -> SettingsScreen()
                Destination.AddFriend -> AddFriendScreen(
                    onOpenContacts = { navigate(Destination.ContactImport) },
                )
                // Friends picked in bulk: straight Home, where they now appear in the stack.
                Destination.ContactImport -> ContactImportScreen(
                    onDone = {
                        if (backStack.lastOrNull() == Destination.ContactImport) backStack = InitialBackStack
                    },
                )
                Destination.EditFriendList -> ComingSoon("Edit Friend", phase = 5)
                Destination.RemoveFriend -> ComingSoon("Remove Friend", phase = 5)
                Destination.Statistics -> ComingSoon("Statistics", phase = 6)
                is Destination.FriendDetails -> ComingSoon("Friend details", phase = 4)
                is Destination.EditFriend -> ComingSoon("Edit", phase = 5)
            }
        }
    }
    }
}
