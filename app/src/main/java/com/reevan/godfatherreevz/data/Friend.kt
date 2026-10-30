package com.reevan.godfatherreevz.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * A friend in the rotation.
 *
 * [position] and [turnUsed] belong to the rotation engine (`rotation/Rotation.kt`) and are only
 * ever written by [RotationDao]'s transactions: [position] is the friend's place in the global call
 * order (0 = next up, roughly "days until their next call"); [turnUsed] says they have already had
 * their call in the current pass. Every waiting friend sits before every friend whose turn is used.
 *
 * Never written with REPLACE: SQLite does that as delete + insert, and the delete would set every
 * one of this friend's calls' `friendId` to null, detaching their history.
 */
@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Each word capitalised on save. */
    val name: String,
    /** As entered or imported; used for display and dialling. */
    val phone: String,
    /** Free-text conversation notes, edited on the friend's details page. */
    @ColumnInfo(defaultValue = "") val notes: String = "",
    /** The day the friend was added ("friend since"). */
    val addedOn: LocalDate,
    /** Epoch millis; tiebreaks only. */
    val createdAt: Long,
    val position: Int,
    val turnUsed: Boolean = false,
    /**
     * The friend's photo: a file name in the app's private photos folder (`photos/FriendPhotos`),
     * or null for none. Added in schema v2.
     */
    val photo: String? = null,
)

/** A friend plus the date of their most recent call, or null if never called. */
data class FriendWithLastCall(
    @Embedded val friend: Friend,
    val lastCalled: LocalDate?,
)
