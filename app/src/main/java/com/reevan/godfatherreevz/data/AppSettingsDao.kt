package com.reevan.godfatherreevz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * The single settings row. Each preference is written with its own targeted UPDATE rather than by
 * rewriting the whole row, so two screens can never overwrite each other with a stale copy.
 */
@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettings?>

    /** Creates the row with defaults on first use; does nothing once it exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(settings: AppSettings)

    @Query("UPDATE app_settings SET theme = :theme WHERE id = 1")
    suspend fun updateTheme(theme: AppTheme)

    @Query("UPDATE app_settings SET callDirectly = :callDirectly WHERE id = 1")
    suspend fun updateCallDirectly(callDirectly: Boolean)

    @Transaction
    suspend fun setTheme(theme: AppTheme) {
        insertIfMissing(AppSettings())
        updateTheme(theme)
    }

    @Transaction
    suspend fun setCallDirectly(callDirectly: Boolean) {
        insertIfMissing(AppSettings())
        updateCallDirectly(callDirectly)
    }
}
