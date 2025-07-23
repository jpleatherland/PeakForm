package dev.jpleatherland.peakform.util

import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date

fun Date.asDayEpochMillis(): Long {
    val calendar =
        Calendar.getInstance().apply {
            time = this@asDayEpochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    return calendar.timeInMillis
}

fun LocalDate.toEpochMillis(): Long = this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
