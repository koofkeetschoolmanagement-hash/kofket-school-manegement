package com.example.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AttendanceDateHelper {
    private const val DATE_PATTERN = "yyyy-MM-dd"

    fun todayDateLabel(): String {
        val formatter = SimpleDateFormat(DATE_PATTERN, Locale.US)
        return formatter.format(Calendar.getInstance().time)
    }

    fun currentMonthLabel(): String {
        val formatter = SimpleDateFormat("yyyy-MM", Locale.US)
        return formatter.format(Calendar.getInstance().time)
    }

    fun shiftDateLabel(dateLabel: String, deltaDays: Int): String {
        val formatter = SimpleDateFormat(DATE_PATTERN, Locale.US).apply { isLenient = false }
        val cal = Calendar.getInstance()
        val parsed = runCatching { formatter.parse(dateLabel.trim()) }.getOrNull()
        if (parsed != null) {
            cal.time = parsed
        }
        cal.add(Calendar.DAY_OF_MONTH, deltaDays)
        return formatter.format(cal.time)
    }
}
