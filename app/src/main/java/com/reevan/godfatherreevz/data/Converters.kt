package com.reevan.godfatherreevz.data

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * Dates are stored as epoch days: a plain calendar day with no time or zone, so "called on
 * 3 October" stays 3 October even if the phone later changes time zone.
 */
class Converters {

    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
}
