package com.reevan.godfatherreevz.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Schema history:
 * - v1: `friends`, `calls`, `app_settings`
 *
 * Once real friends are on the phone, v1 is frozen: never edit it in place. Every schema change is
 * a version bump plus a migration here. Adding a table, or a nullable column, is purely additive,
 * so Room generates the migration from the exported schemas in `app/schemas/` via
 * `autoMigrations = [AutoMigration(from = n, to = n + 1)]`. Never use
 * fallbackToDestructiveMigration — call history and notes are not recoverable.
 */
@Database(
    entities = [
        Friend::class,
        Call::class,
        AppSettings::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GodfatherDatabase : RoomDatabase() {

    abstract fun friendDao(): FriendDao

    abstract fun callDao(): CallDao

    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        const val NAME = "godfather-reevz.db"

        @Volatile
        private var instance: GodfatherDatabase? = null

        fun getInstance(context: Context): GodfatherDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GodfatherDatabase::class.java,
                    NAME,
                ).build().also { instance = it }
            }
    }
}
