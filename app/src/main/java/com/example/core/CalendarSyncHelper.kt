package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.example.data.model.Assignment
import com.example.data.model.SchoolCalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object CalendarSyncHelper {

    fun parseEventEpochMillis(dateLabel: String, timeLabel: String): Long {
        val cleanDate = dateLabel.trim()
        val cleanTime = timeLabel.trim()
        val fullFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
        val dateOnlyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

        val parsedFull = runCatching { fullFmt.parse("$cleanDate $cleanTime") }.getOrNull()
        if (parsedFull != null) return parsedFull.time

        val parsedDate = runCatching { dateOnlyFmt.parse(cleanDate) }.getOrNull()
        if (parsedDate != null) {
            val cal = Calendar.getInstance()
            cal.time = parsedDate
            cal.set(Calendar.HOUR_OF_DAY, 8)
            cal.set(Calendar.MINUTE, 30)
            cal.set(Calendar.SECOND, 0)
            return cal.timeInMillis
        }
        return System.currentTimeMillis()
    }

    fun buildDeviceCalendarInsertIntent(event: SchoolCalendarEvent): Intent {
        val startMillis = parseEventEpochMillis(event.eventDateLabel, event.startTimeLabel)
        val rawEndMillis = parseEventEpochMillis(event.eventDateLabel, event.endTimeLabel)
        val endMillis = if (rawEndMillis > startMillis) rawEndMillis else startMillis + 60 * 60 * 1000L

        val descriptionText = buildString {
            if (event.subject.isNotBlank()) append("Subject: ${event.subject}\n")
            if (event.gradeLevel.isNotBlank()) append("Grade: ${event.gradeLevel}\n")
            if (event.description.isNotBlank()) append(event.description)
        }.trim()

        return Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            .putExtra(CalendarContract.Events.TITLE, event.title)
            .putExtra(CalendarContract.Events.DESCRIPTION, descriptionText)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, "Mana Barumsaa Koofkeet (Kofket School)")
    }

    fun buildIcsContent(
        events: List<SchoolCalendarEvent>,
        assignments: List<Assignment> = emptyList()
    ): String {
        val utcFmt = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        fun esc(s: String): String = s
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")

        val combined = buildList {
            addAll(events)
            val existingAssignIds = events.filter { it.sourceType == "ASSIGNMENT" }.map { it.sourceId }.toSet()
            assignments.forEach { hw ->
                if (hw.id !in existingAssignIds) {
                    add(
                        SchoolCalendarEvent(
                            id = "assign_${hw.id}",
                            title = "${hw.subject}: ${hw.title}",
                            description = hw.description,
                            eventDateLabel = hw.dueDateLabel,
                            startTimeLabel = "08:30",
                            endTimeLabel = "09:30",
                            category = "ASSIGNMENT",
                            gradeLevel = hw.gradeLevel,
                            subject = hw.subject,
                            sourceType = "ASSIGNMENT",
                            sourceId = hw.id
                        )
                    )
                }
            }
        }

        return buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//Kofket School Management//School Calendar Sync//EN")
            appendLine("CALSCALE:GREGORIAN")
            appendLine("METHOD:PUBLISH")
            combined.forEachIndexed { idx, ev ->
                val startMs = parseEventEpochMillis(ev.eventDateLabel, ev.startTimeLabel)
                val endMs = parseEventEpochMillis(ev.eventDateLabel, ev.endTimeLabel)
                    .let { if (it > startMs) it else startMs + 60 * 60 * 1000L }
                val uid = (ev.id.ifBlank { "ev_$idx" }) + "@kofket.school"
                appendLine("BEGIN:VEVENT")
                appendLine("UID:${esc(uid)}")
                appendLine("DTSTAMP:${utcFmt.format(System.currentTimeMillis())}")
                appendLine("DTSTART:${utcFmt.format(startMs)}")
                appendLine("DTEND:${utcFmt.format(endMs)}")
                appendLine("SUMMARY:${esc(ev.title)}")
                val desc = listOfNotNull(
                    ev.subject.takeIf { it.isNotBlank() }?.let { "Subject: $it" },
                    ev.gradeLevel.takeIf { it.isNotBlank() }?.let { "Grade: $it" },
                    ev.description.takeIf { it.isNotBlank() }
                ).joinToString(" | ")
                if (desc.isNotBlank()) appendLine("DESCRIPTION:${esc(desc)}")
                appendLine("LOCATION:Kofket School")
                appendLine("CATEGORIES:${esc(ev.category)}")
                appendLine("END:VEVENT")
            }
            appendLine("END:VCALENDAR")
        }
    }

    suspend fun exportCalendarIcs(
        context: Context,
        uri: Uri,
        events: List<SchoolCalendarEvent>,
        assignments: List<Assignment> = emptyList()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Unable to open .ics destination"))
            val content = buildIcsContent(events, assignments)
            stream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
