package com.reevan.godfatherreevz

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.reevan.godfatherreevz.data.AppTheme
import com.reevan.godfatherreevz.data.CallOutcome
import com.reevan.godfatherreevz.data.FriendDao
import com.reevan.godfatherreevz.data.GodfatherDatabase
import com.reevan.godfatherreevz.data.NewFriend
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.random.Random

/** The rotation transactions against a real (in-memory) Room database. */
@RunWith(AndroidJUnit4::class)
class GodfatherDatabaseTest {

    private lateinit var db: GodfatherDatabase
    private lateinit var friends: FriendDao
    private val today = LocalDate.of(2026, 10, 29)
    private val random = Random(42)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GodfatherDatabase::class.java,
        ).build()
        friends = db.friendDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun add(vararg names: String): List<Long> =
        friends.addFriends(names.map { NewFriend(it, "98765 4321${names.indexOf(it)}") }, today, 0L, random)

    private suspend fun order() = friends.observeInCallOrder().first()
    private suspend fun head() = order().first().friend.id

    @Test
    fun positionsAreAPermutationAfterAdding() = runBlocking {
        add("A", "B", "C", "D", "E")
        assertEquals((0..4).toList(), order().map { it.friend.position })
    }

    @Test
    fun callingTodaysFriendRecordsAScheduledCallAndMovesThemBack() = runBlocking {
        add("A", "B", "C", "D")
        val head = head()
        assertEquals(CallOutcome.RECORDED, friends.recordCall(head, today, random))
        val call = db.callDao().observeCallOn(head, today).first()!!
        assertTrue(call.scheduled)
        assertTrue(call.consumedTurn)
        assertNotEquals(head, head())
        assertEquals(today, order().first { it.friend.id == head }.lastCalled)
    }

    @Test
    fun redialingTheSameDayDoesNotCountTwice() = runBlocking {
        add("A", "B", "C")
        val head = head()
        friends.recordCall(head, today, random)
        val orderAfterFirst = order()
        assertEquals(CallOutcome.ALREADY_RECORDED, friends.recordCall(head, today, random))
        assertEquals(1, db.callDao().observeAll().first().size)
        assertEquals(orderAfterFirst, order())
    }

    @Test
    fun anOffScheduleCallKeepsTodaysFriendAndIsNotScheduled() = runBlocking {
        add("A", "B", "C", "D")
        val head = head()
        val other = order()[2].friend.id
        friends.recordCall(other, today, random)
        assertEquals(head, head())
        assertFalse(db.callDao().observeCallOn(other, today).first()!!.scheduled)
        // Today's call is still to make.
        assertNull(db.callDao().observeScheduledBetween(today, today.plusDays(2)).first())
    }

    @Test
    fun onlyOneScheduledCallPerDay() = runBlocking {
        add("A", "B", "C")
        friends.recordCall(head(), today, random)
        val tomorrowsHead = head()
        friends.recordCall(tomorrowsHead, today, random) // calling ahead: off-schedule
        assertFalse(db.callDao().observeCallOn(tomorrowsHead, today).first()!!.scheduled)
    }

    @Test
    fun undoingTodaysCallPutsTheFriendBackAtTheHead() = runBlocking {
        add("A", "B", "C", "D")
        val head = head()
        friends.recordCall(head, today, random)
        assertTrue(friends.undoCall(head, today, random))
        assertEquals(head, head())
        assertNull(order().first().lastCalled)
        assertTrue(db.callDao().observeAll().first().isEmpty())
        // A second Undo finds nothing to undo.
        assertFalse(friends.undoCall(head, today, random))
    }

    @Test
    fun removingAFriendKeepsTheirCallsWithTheNameSnapshot() = runBlocking {
        add("A", "B", "C")
        val head = head()
        val name = order().first().friend.name
        friends.recordCall(head, today, random)
        friends.removeFriends(listOf(head))
        val call = db.callDao().observeAll().first().single()
        assertNull(call.friendId)
        assertEquals(name, call.friendName)
        assertEquals((0..1).toList(), order().map { it.friend.position })
        // Today's call stays done even though that friend is gone.
        assertTrue(db.callDao().observeScheduledBetween(today, today.plusDays(2)).first()!!.scheduled)
    }

    @Test
    fun editingDetailsLeavesHistoryAndPlaceAlone() = runBlocking {
        add("A", "B", "C")
        val head = head()
        friends.recordCall(head, today, random)
        val before = order().first { it.friend.id == head }
        friends.updateDetails(head, "Renamed", "+1 555 0100", "new.jpg")
        val after = order().first { it.friend.id == head }
        assertEquals(before.friend.position, after.friend.position)
        assertEquals(before.lastCalled, after.lastCalled)
        assertEquals(head, db.callDao().observeAll().first().single().friendId)
    }

    @Test
    fun aNewFriendAddedWhileTodayIsLockedNeverBecomesTodaysFriend() = runBlocking {
        add("A", "B", "C")
        val head = head()
        repeat(10) { add("New$it") }
        assertEquals(head, head())
    }

    @Test
    fun aFriendWhoBecomesTodaysAfterAnEarlierCallTodayCountsAsDone() = runBlocking {
        // [A, B]: call B off-schedule, then remove A — B becomes today's friend, already called.
        val (a, b) = add("A", "B").let { ids -> order().map { it.friend.id }.also { assertEquals(ids.toSet(), it.toSet()) } }
        friends.recordCall(b, today, random)
        friends.removeFriends(listOf(a))
        assertEquals(b, head())
        assertEquals(CallOutcome.ALREADY_RECORDED, friends.recordCall(b, today, random))
        assertTrue(db.callDao().observeCallOn(b, today).first()!!.scheduled)
    }

    // ---- Photos --------------------------------------------------------------------------------

    @Test
    fun photosChangeWithoutTouchingTheRotation() = runBlocking {
        val (a, b) = add("A", "B", "C")
        friends.recordCall(head(), today, random)
        val before = order().map { it.friend.copy(photo = null) to it.lastCalled }

        assertNull(friends.replacePhoto(a, "a1.jpg"))
        assertEquals("a1.jpg", friends.replacePhoto(a, "a2.jpg"))
        assertTrue(friends.setPhotoIfNone(b, "b1.jpg"))
        // Someone with a photo keeps it.
        assertFalse(friends.setPhotoIfNone(b, "b2.jpg"))
        assertEquals("b1.jpg", friends.get(b)!!.photo)
        assertEquals(setOf("a2.jpg", "b1.jpg"), friends.photosInUse().toSet())

        assertEquals(before, order().map { it.friend.copy(photo = null) to it.lastCalled })
        assertEquals("a2.jpg", friends.replacePhoto(a, null))
        assertNull(friends.get(a)!!.photo)
    }

    @Test
    fun aNewFriendCanComeWithAPhoto_andRemovingThemHandsItBack() = runBlocking {
        val (id) = friends.addFriends(listOf(NewFriend("Ana", "90000 00001", photo = "ana.jpg")), today, 0L, random)
        add("B")
        assertEquals("ana.jpg", friends.get(id)!!.photo)
        assertEquals(listOf("ana.jpg"), friends.removeFriends(listOf(id)))
        assertEquals(emptyList<String>(), friends.photosInUse())
    }

    /**
     * Schema v1 → v2 (photos) on a database built exactly as `app/schemas/.../1.json` describes —
     * what the owner's phone had. Room runs the auto-migration and then checks the result against
     * the v2 schema, so any mismatch fails here.
     */
    @Test
    fun upgradingFromV1KeepsEverything() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { v1 ->
            V1_SCHEMA.forEach(v1::execSQL)
            v1.execSQL(
                "INSERT INTO friends (id, name, phone, notes, addedOn, createdAt, position, turnUsed) VALUES " +
                    "(1, 'Ana', '90000 00001', 'Likes jazz', 20390, 5, 1, 0), (2, 'Bo', '90000 00002', '', 20391, 6, 0, 1)",
            )
            v1.execSQL(
                "INSERT INTO calls (id, friendId, friendName, date, scheduled, consumedTurn) VALUES " +
                    "(1, 2, 'Bo', 20755, 1, 1), (2, NULL, 'Gone', 20750, 1, 1)",
            )
            v1.execSQL("INSERT INTO app_settings (id, theme, callDirectly) VALUES (1, 'NOTHING', 0)")
            v1.version = 1
        }

        val upgraded = Room.databaseBuilder(context, GodfatherDatabase::class.java, name).build()
        try {
            val all = upgraded.friendDao().observeInCallOrder().first()
            assertEquals(listOf("Bo", "Ana"), all.map { it.friend.name })
            assertEquals(listOf(true, false), all.map { it.friend.turnUsed })
            assertEquals("Likes jazz", all[1].friend.notes)
            assertEquals(LocalDate.ofEpochDay(20390), all[1].friend.addedOn)
            assertEquals(LocalDate.ofEpochDay(20755), all[0].lastCalled)
            assertTrue(all.all { it.friend.photo == null })
            assertEquals(2, upgraded.callDao().observeAll().first().size)
            val settings = upgraded.appSettingsDao().observe().first()!!
            assertEquals(AppTheme.NOTHING, settings.theme)
            assertFalse(settings.callDirectly)
            // And the new column works.
            assertNull(upgraded.friendDao().replacePhoto(1, "ana.jpg"))
            assertEquals("ana.jpg", upgraded.friendDao().get(1)!!.photo)
        } finally {
            upgraded.close()
            context.deleteDatabase(name)
        }
    }

    private companion object {
        /** Schema v1, from `app/schemas/com.reevan.godfatherreevz.data.GodfatherDatabase/1.json`. */
        val V1_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `friends` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                "`phone` TEXT NOT NULL, `notes` TEXT NOT NULL DEFAULT '', `addedOn` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `position` INTEGER NOT NULL, `turnUsed` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `calls` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `friendId` INTEGER, " +
                "`friendName` TEXT NOT NULL, `date` INTEGER NOT NULL, `scheduled` INTEGER NOT NULL, " +
                "`consumedTurn` INTEGER NOT NULL, FOREIGN KEY(`friendId`) REFERENCES `friends`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_calls_friendId_date` ON `calls` (`friendId`, `date`)",
            "CREATE INDEX IF NOT EXISTS `index_calls_date` ON `calls` (`date`)",
            "CREATE TABLE IF NOT EXISTS `app_settings` (`id` INTEGER NOT NULL, `theme` TEXT NOT NULL, " +
                "`callDirectly` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '4767e6bc72cfb91346098d0b044aed3b')",
        )
    }
}
