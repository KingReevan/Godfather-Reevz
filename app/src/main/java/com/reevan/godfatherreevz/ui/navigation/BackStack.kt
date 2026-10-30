package com.reevan.godfatherreevz.ui.navigation

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/*
 * The back stack is an immutable list of destinations, bottom first, held as state in
 * GodfatherApp. Home is always at the bottom and is never popped — back from Home leaves the
 * app, which the system does on its own.
 *
 * Hand-rolled instead of a navigation library: the app is at most four screens deep with one
 * argument (a friend id), so push and pop are all it needs.
 */

val InitialBackStack: List<Destination> = listOf(Destination.Home)

/**
 * Opens [destination] on top. Opening the screen that is already on top does nothing, so two taps
 * landing before the new screen appears can't stack it twice (`... > Add Friend > Add Friend`).
 */
fun List<Destination>.push(destination: Destination): List<Destination> =
    if (lastOrNull() == destination) this else this + destination

/**
 * Opens [destination] on top of [from] — but only if [from] is still the screen on top. Two
 * taps landing in the same frame (two fingers on two Menu buttons, say) would otherwise stack
 * both screens: the second tap comes from a screen that is no longer showing, so it is ignored.
 */
fun List<Destination>.pushFrom(from: Destination, destination: Destination): List<Destination> =
    if (lastOrNull() == from) push(destination) else this

/**
 * The back stack the header's Settings and Menu icons lead to: that screen straight above Home,
 * whatever was open. Already there, nothing changes.
 */
fun List<Destination>.jumpTo(section: Destination): List<Destination> =
    if (this == listOf(Destination.Home, section)) this else listOf(Destination.Home, section)

/** Drops the top screen. Never empties the stack: Home stays. */
fun List<Destination>.pop(): List<Destination> = if (size > 1) dropLast(1) else this

/**
 * Jumps back to the screen at [index] — a tapped breadcrumb — dropping everything above it. An
 * index past the top changes nothing; Home (index 0) always stays.
 */
fun List<Destination>.popTo(index: Int): List<Destination> =
    take(index.coerceIn(0, lastIndex) + 1)

/**
 * Pops [screen] only if it is still on top. For a screen finishing its own job (Create, Save,
 * Remove) after an async write: if the user has already left it — say, by tapping a breadcrumb
 * while the save ran — a blind pop would take them one screen further than they chose.
 */
fun List<Destination>.popIfCurrent(screen: Destination): List<Destination> =
    if (lastOrNull() == screen) pop() else this

/** The breadcrumb trail for the current screen, e.g. ["Home", "Menu", "Add Friend"]. */
fun List<Destination>.breadcrumbs(): List<String> = map { it.label }

val BackStackSaver: Saver<List<Destination>, Any> = listSaver(
    save = { stack -> stack.map { it.route } },
    restore = { routes -> routes.map(Destination::fromRoute) },
)
