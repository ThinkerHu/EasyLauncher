package com.vincenthu.launcher

import android.content.Context
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.ZoneId

data class CalendarEntry(val title: String, val begin: Long, val end: Long, val allDay: Boolean, val calendarName: String)

object CalendarRepository {
    fun today(context: Context): List<CalendarEntry> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(start.toString()).appendPath(end.toString()).build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME
        )
        val result = mutableListOf<CalendarEntry>()
        context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
            val titleIndex = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginIndex = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endIndex = cursor.getColumnIndex(CalendarContract.Instances.END)
            val allDayIndex = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
            val calendarIndex = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
            while (cursor.moveToNext() && result.size < 5) {
                result += CalendarEntry(
                    title = cursor.getString(titleIndex).orEmpty().ifBlank { "未命名日程" },
                    begin = cursor.getLong(beginIndex),
                    end = cursor.getLong(endIndex),
                    allDay = cursor.getInt(allDayIndex) != 0,
                    calendarName = cursor.getString(calendarIndex).orEmpty()
                )
            }
        }
        return result
    }
}
