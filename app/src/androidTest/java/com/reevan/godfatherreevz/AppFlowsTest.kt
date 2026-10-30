package com.reevan.godfatherreevz

import android.content.Context
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.reevan.godfatherreevz.data.AppTheme
import com.reevan.godfatherreevz.data.GodfatherDatabase
import com.reevan.godfatherreevz.data.NewFriend
import com.reevan.godfatherreevz.ui.details.CALL_BUTTON_TAG
import com.reevan.godfatherreevz.ui.home.TODAY_CARD_TAG
import com.reevan.godfatherreevz.util.AndroidCaller
import com.reevan.godfatherreevz.util.Caller
import com.reevan.godfatherreevz.util.TodayClock
import com.reevan.godfatherreevz.util.formatCallDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

/** Places no calls; remembers what it was asked to dial. */
class FakeCaller : Caller {
    val dialled = CopyOnWriteArrayList<String>()
    override fun canCall(context: Context) = true
    override fun mayCallDirectly(context: Context) = true
    override fun call(context: Context, phone: String, direct: Boolean): Boolean {
        dialled += phone
        return true
    }
}

/**
 * End-to-end flows through the real Activity and **the real database, which is wiped before every
 * test** — so never run these on the phone once it holds real friends (back the database up
 * first; see CLAUDE.md). Calls go to [FakeCaller]: nobody is ever dialled.
 */
@RunWith(AndroidJUnit4::class)
class AppFlowsTest {

    private val app = ApplicationProvider.getApplicationContext<GodfatherApplication>()
    private val db get() = GodfatherDatabase.getInstance(app)
    private val caller = FakeCaller()
    private val today get() = TodayClock.instance.today.value

    /** Runs before the Activity starts: a clean database and the fake caller. */
    @get:Rule(order = 0)
    val setUp = object : ExternalResource() {
        override fun before() {
            db.clearAllTables()
            app.caller = caller
        }

        override fun after() {
            app.caller = AndroidCaller
        }
    }

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    // ---- Helpers ------------------------------------------------------------------------------

    private fun seed(vararg friends: Pair<String, String>): List<Long> = runBlocking {
        db.friendDao().addFriends(friends.map { NewFriend(it.first, it.second) }, today, 0L, Random(7))
    }

    private fun seedThree() = seed("Ana" to "90000 00001", "Bo" to "90000 00002", "Cy" to "90000 00003")

    private fun order() = runBlocking { db.friendDao().observeInCallOrder().first() }
    private fun calls() = runBlocking { db.callDao().observeAll().first() }

    private fun waitFor(text: String, substring: Boolean = false) {
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitUntil(condition: () -> Boolean) = compose.waitUntil(5_000, condition)

    private fun button(text: String): SemanticsNodeInteraction =
        compose.onNode(hasText(text) and hasClickAction())

    private fun openMenu(entry: String) {
        compose.onNodeWithContentDescription("Menu").performClick()
        compose.onNodeWithText(entry).performClick()
    }

    private fun todayCard() = compose.onNodeWithTag(TODAY_CARD_TAG, useUnmergedTree = false)

    // ---- Home ---------------------------------------------------------------------------------

    @Test
    fun emptyHome_invitesToAddTheFirstFriend_whoBecomesTodaysCall() {
        compose.onNodeWithText("No friends yet").assertIsDisplayed()
        compose.onNodeWithText("Add your first friend").performClick()

        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("marco rossi")
        fields[1].performTextInput("+91 98765 43210")
        button("Add Friend").performClick()
        waitFor("Marco Rossi added")

        compose.onNodeWithText("Home").performClick()
        waitUntil { order().size == 1 }
        todayCard().assert(hasText("Marco Rossi")).assert(hasText("TODAY")).assert(hasText("-"))
    }

    @Test
    fun theOrderIsNotTheOrderOfAdding() {
        // 12 friends added in order: the chance a shuffle keeps exactly that order is 1 in 12!.
        val ids = seed(*(1..12).map { "Friend $it" to "9100000${it.toString().padStart(4, '0')}" }.toTypedArray())
        assertTrue(order().map { it.friend.id } != ids)
    }

    // ---- Calling ------------------------------------------------------------------------------

    @Test
    fun callingTodaysFriend_countsOnce_andKeepsThemInFocusMarkedCalled() {
        seedThree()
        val head = order().first().friend
        waitUntil { compose.onAllNodesWithText(head.name).fetchSemanticsNodes().isNotEmpty() }
        todayCard().performClick()

        compose.onNodeWithTag(CALL_BUTTON_TAG).performClick()
        waitFor("Called today")
        assertEquals(listOf(head.phone), caller.dialled.toList())

        // Again the same day: redials, but counts nothing new.
        waitFor("Call again")
        compose.mainClock.advanceTimeBy(1_600) // past the button's double-tap cool-down
        compose.onNodeWithTag(CALL_BUTTON_TAG).performClick()
        waitUntil { caller.dialled.size == 2 }
        assertEquals(1, calls().size)
        assertTrue(calls().single().scheduled)

        compose.onNodeWithContentDescription("Back").performClick()
        waitFor("CALLED TODAY", substring = true)
        todayCard().assert(hasText(head.name)).assert(hasText(formatCallDate(today)))
    }

    @Test
    fun undoingTodaysCall_putsThemBackAsTodaysCall() {
        seedThree()
        val head = order().first().friend
        todayCard().performClick()
        compose.onNodeWithTag(CALL_BUTTON_TAG).performClick()
        waitFor("Undo")

        compose.onNodeWithText("Undo").performClick()
        compose.onNodeWithText("Undo call").performClick()
        waitFor("Call ${head.name}")
        assertTrue(calls().isEmpty())
        assertEquals(head.id, order().first().friend.id)
    }

    @Test
    fun callingSomeoneElse_usesTheirTurn_andLeavesTodaysFriendAlone() {
        seedThree()
        val before = order()
        val other = before[2].friend
        compose.onNodeWithText(other.name).performClick()
        compose.onNodeWithText("Not today's call", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(CALL_BUTTON_TAG).performClick()

        waitUntil { calls().size == 1 }
        val after = order()
        assertEquals(before.first().friend.id, after.first().friend.id)
        assertTrue(after.first { it.friend.id == other.id }.friend.turnUsed)
        assertTrue(!calls().single().scheduled)
    }

    // ---- Add, Edit, Remove --------------------------------------------------------------------

    @Test
    fun addFriend_refusesANumberAlreadySaved() {
        seed("Ana" to "+91 98765 43210")
        openMenu("ADD FRIEND")
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("Someone")
        fields[1].performTextInput("098765-43210")
        compose.onNodeWithText("Already saved as Ana").assertIsDisplayed()
        button("Add Friend").assertIsNotEnabled()
    }

    @Test
    fun editFriend_asksBeforeDiscarding_andSavesInPlace() {
        seedThree()
        val ana = order().first { it.friend.name == "Ana" }.friend
        openMenu("EDIT FRIEND")
        compose.onNodeWithText("Ana").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput(" Maria")

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Discard changes?").assertIsDisplayed()
        compose.onNodeWithText("Keep editing").performClick()

        button("Save").assertIsEnabled().performClick()
        waitFor("Ana Maria")
        val saved = order().first { it.friend.id == ana.id }.friend
        assertEquals("Ana Maria", saved.name)
        assertEquals(ana.position, saved.position)
    }

    @Test
    fun removeFriend_confirmsFirst_andTheirCallsStayInStatistics() {
        seedThree()
        val ana = order().first { it.friend.name == "Ana" }.friend
        runBlocking { db.friendDao().recordCall(ana.id, today, Random(1)) }

        openMenu("REMOVE FRIEND")
        compose.onNodeWithText("Ana").performClick()
        compose.onNodeWithText("Remove (1)").performClick()
        compose.onNodeWithText("Remove Ana?").assertIsDisplayed()
        compose.onNodeWithText("Remove").performClick()

        waitFor("ADD FRIEND") // back on the Menu
        assertEquals(listOf("Bo", "Cy"), order().map { it.friend.name }.sorted())
        val call = calls().single()
        assertNull(call.friendId)
        assertEquals("Ana", call.friendName)

        compose.onNodeWithText("STATISTICS").performClick()
        compose.onNode(hasText("Calls made")).assertIsDisplayed()
        // Today's call still counts: current and longest streak are both 1 day.
        waitUntil { compose.onAllNodesWithText("1 day").fetchSemanticsNodes().size == 2 }
    }

    // ---- Settings -----------------------------------------------------------------------------

    @Test
    fun shuffleAgain_asksFirst() {
        seedThree()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNode(hasText("Shuffle") and hasClickAction() and !hasAnyAncestor(isDialog()))
            .performScrollTo()
            .performClick()
        compose.onNodeWithText("Shuffle the order?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()

        compose.onNode(hasText("Shuffle") and hasClickAction() and !hasAnyAncestor(isDialog())).performClick()
        compose.onNode(hasText("Shuffle") and hasClickAction() and hasAnyAncestor(isDialog())).performClick()
        waitFor("Shuffled")
        assertEquals(3, order().size)
    }

    @Test
    fun theme_appliesAtOnce_andSurvivesARestart() {
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNode(hasText("Corleone Noir") and isSelectable()).performClick()
        waitUntil { runBlocking { db.appSettingsDao().observe().first()?.theme } == AppTheme.CORLEONE_NOIR }
        compose.activityRule.scenario.recreate()
        compose.onNode(hasText("Corleone Noir") and isSelectable()).assertIsSelected()
    }

    @Test
    fun theHeaderReachesSettingsAndMenuFromAnyScreen() {
        seedThree()
        todayCard().performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Theme").assertIsDisplayed()
        compose.onNodeWithContentDescription("Menu").performClick()
        compose.onNodeWithText("REMOVE FRIEND").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag(TODAY_CARD_TAG).assertIsDisplayed()
    }
}
