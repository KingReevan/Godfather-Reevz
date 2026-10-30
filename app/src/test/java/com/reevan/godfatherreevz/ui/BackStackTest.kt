package com.reevan.godfatherreevz.ui

import com.reevan.godfatherreevz.ui.navigation.Destination
import com.reevan.godfatherreevz.ui.navigation.InitialBackStack
import com.reevan.godfatherreevz.ui.navigation.breadcrumbs
import com.reevan.godfatherreevz.ui.navigation.jumpTo
import com.reevan.godfatherreevz.ui.navigation.pop
import com.reevan.godfatherreevz.ui.navigation.popIfCurrent
import com.reevan.godfatherreevz.ui.navigation.popTo
import com.reevan.godfatherreevz.ui.navigation.push
import com.reevan.godfatherreevz.ui.navigation.pushFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackStackTest {

    @Test
    fun startsAtHome() {
        assertEquals(listOf(Destination.Home), InitialBackStack)
    }

    @Test
    fun pushThenPop_returnsToWhereItWas() {
        val menu = InitialBackStack.push(Destination.Menu)
        val add = menu.push(Destination.AddFriend)
        assertEquals(Destination.AddFriend, add.last())
        assertEquals(menu, add.pop())
        assertEquals(InitialBackStack, add.pop().pop())
    }

    @Test
    fun push_theScreenAlreadyOnTop_doesNotStackItTwice() {
        val adding = InitialBackStack.push(Destination.Menu).push(Destination.AddFriend)
        assertEquals(adding, adding.push(Destination.AddFriend))
        val editing = adding.push(Destination.EditFriend(1))
        assertEquals(editing, editing.push(Destination.EditFriend(1)))
        // A different friend is a different screen.
        assertEquals(editing + Destination.EditFriend(2), editing.push(Destination.EditFriend(2)))
    }

    @Test
    fun pushFrom_onlyOpensFromTheScreenOnTop() {
        val menu = InitialBackStack.push(Destination.Menu)
        val adding = menu.pushFrom(Destination.Menu, Destination.AddFriend)
        assertEquals(menu + Destination.AddFriend, adding)
        // A second tap in the same frame, from the Menu that is no longer showing: ignored.
        assertEquals(adding, adding.pushFrom(Destination.Menu, Destination.RemoveFriend))
    }

    @Test
    fun pop_neverRemovesHome() {
        assertEquals(InitialBackStack, InitialBackStack.pop())
    }

    @Test
    fun popTo_jumpsBackToTheTappedCrumb() {
        val stack = InitialBackStack
            .push(Destination.Menu)
            .push(Destination.EditFriendList)
            .push(Destination.EditFriend(5))
        assertEquals(InitialBackStack, stack.popTo(0))
        assertEquals(listOf(Destination.Home, Destination.Menu), stack.popTo(1))
        assertEquals(stack.dropLast(1), stack.popTo(2))
        assertEquals(stack, stack.popTo(3))
        assertEquals(stack, stack.popTo(99))
        assertEquals(InitialBackStack, stack.popTo(-1))
    }

    @Test
    fun popIfCurrent_onlyPopsTheScreenStillOnTop() {
        val adding = InitialBackStack.push(Destination.Menu).push(Destination.AddFriend)
        assertEquals(adding.dropLast(1), adding.popIfCurrent(Destination.AddFriend))
        val alreadyLeft = adding.popTo(1)
        assertEquals(alreadyLeft, alreadyLeft.popIfCurrent(Destination.AddFriend))
        val editing = InitialBackStack.push(Destination.EditFriend(1))
        assertEquals(editing, editing.popIfCurrent(Destination.EditFriend(2)))
        assertEquals(InitialBackStack, editing.popIfCurrent(Destination.EditFriend(1)))
    }

    @Test
    fun jumpTo_putsTheSectionStraightAboveHome() {
        val deep = InitialBackStack.push(Destination.Menu).push(Destination.EditFriendList)
            .push(Destination.EditFriend(3))
        assertEquals(listOf(Destination.Home, Destination.Settings), deep.jumpTo(Destination.Settings))
        assertEquals(listOf(Destination.Home, Destination.Menu), deep.jumpTo(Destination.Menu))
        val details = InitialBackStack.push(Destination.FriendDetails(4))
        assertEquals(listOf(Destination.Home, Destination.Menu), details.jumpTo(Destination.Menu))
        val settings = listOf(Destination.Home, Destination.Settings)
        assertEquals(settings, settings.jumpTo(Destination.Settings))
    }

    @Test
    fun breadcrumbs_followTheStack() {
        val stack = InitialBackStack.push(Destination.Menu).push(Destination.AddFriend)
            .push(Destination.ContactImport)
        assertEquals(listOf("Home", "Menu", "Add Friend", "Contacts"), stack.breadcrumbs())
    }

    @Test
    fun menuEntries_areTheFourSectionsInSpecOrder() {
        assertEquals(
            listOf("Add Friend", "Edit Friend", "Remove Friend", "Statistics"),
            Destination.menuEntries.map { it.label },
        )
    }

    @Test
    fun everyDestination_roundTripsThroughItsRoute() {
        val all = listOf(
            Destination.Home,
            Destination.Menu,
            Destination.Settings,
            Destination.AddFriend,
            Destination.ContactImport,
            Destination.EditFriendList,
            Destination.RemoveFriend,
            Destination.Statistics,
            Destination.FriendDetails(42),
            Destination.EditFriend(7),
        )
        all.forEach { assertEquals(it, Destination.fromRoute(it.route)) }
        assertEquals(all.size, all.map { it.route }.toSet().size)
    }

    @Test
    fun fromRoute_rejectsGarbage() {
        listOf("", "nope", "edit/", "edit/abc", "friend/1/2", "home/3", "friend/x")
            .forEach { route ->
                assertThrows(IllegalArgumentException::class.java) { Destination.fromRoute(route) }
            }
    }
}
