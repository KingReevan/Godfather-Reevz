package com.reevan.godfatherreevz.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * One row = "the owner pressed Call for this friend on this date". Whether the friend picked up
 * is unknown and irrelevant: pressing the in-app Call button is the only signal.
 *
 * At most one row per friend per day (the unique index), so redialling never double counts.
 * Removing a friend keeps their calls — [friendId] becomes null and [friendName] keeps the name —
 * so Statistics totals and streaks don't change.
 */
@Entity(
    tableName = "calls",
    foreignKeys = [
        ForeignKey(
            entity = Friend::class,
            parentColumns = ["id"],
            childColumns = ["friendId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["friendId", "date"], unique = true),
        Index(value = ["date"]),
    ],
)
data class Call(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val friendId: Long?,
    /** Snapshot of the name at call time, so history survives the friend's removal. */
    val friendName: String,
    val date: LocalDate,
    /** This was the day's call: the friend was today's focus when Call was pressed. */
    val scheduled: Boolean,
    /** This call used the friend's turn in the rotation. Undo needs it to put things back. */
    val consumedTurn: Boolean,
)
