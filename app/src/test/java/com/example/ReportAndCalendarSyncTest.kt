package com.example

import com.example.core.CalendarSyncHelper
import com.example.core.MonthlyReportData
import com.example.core.ReportExporter
import com.example.core.ReportStudentRow
import com.example.data.model.Assignment
import com.example.data.model.AttendanceRecord
import com.example.data.model.ExamResult
import com.example.data.model.SchoolCalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportAndCalendarSyncTest {

    @Test
    fun `buildCsvContent exports summary, marklist, grades, and attendance with escaping`() {
        val report = MonthlyReportData(
            monthLabel = "2026-11",
            totalStudents = 1,
            totalTeachers = 2,
            totalAssignments = 3,
            totalExamsRecorded = 1,
            averageExamScore = 88.5,
            attendanceRatePercent = 100.0,
            openEarlyWarnings = 0,
            resolvedEarlyWarnings = 1,
            studentRows = listOf(
                ReportStudentRow(
                    studentName = "Bona, \"Tola\"",
                    gradeLevel = "Grade 7",
                    averageScore = 88.5,
                    attendancePercent = 100.0
                )
            ),
            warningRows = emptyList()
        )
        val exams = listOf(
            ExamResult(
                id = "ex1",
                studentId = "s1",
                studentName = "Bona, \"Tola\"",
                gradeLevel = "Grade 7",
                subject = "Herrega",
                examName = "Midterm",
                monthLabel = "2026-11",
                score = 88.5,
                remarks = "Great\nWork"
            )
        )
        val attendance = listOf(
            AttendanceRecord(
                id = "att1",
                studentId = "s1",
                studentName = "Bona, \"Tola\"",
                gradeLevel = "Grade 7",
                dateLabel = "2026-11-25",
                status = "PRESENT",
                note = "On time"
            )
        )

        val csv = ReportExporter.buildCsvContent(report, exams, attendance)

        assertTrue(csv.contains("Section,Field,Value"))
        assertTrue(csv.contains("Summary,Month,\"2026-11\""))
        assertTrue(csv.contains("Marklist Summary,\"Bona, \"\"Tola\"\"\",\"Grade 7\",88.5,100.0"))
        assertTrue(csv.contains("Subject Grades,\"Bona, \"\"Tola\"\"\",\"Grade 7\",\"Herrega\",\"Midterm\",\"2026-11\",88.5,\"Great Work\""))
        assertTrue(csv.contains("Attendance History,\"2026-11-25\",\"Bona, \"\"Tola\"\"\",\"Grade 7\",\"PRESENT\",\"On time\""))
    }

    @Test
    fun `buildIcsContent produces valid VCALENDAR with events and unsynced assignments`() {
        val syncedEvent = SchoolCalendarEvent(
            id = "ev1",
            title = "Parent Meeting; Fall",
            description = "Room 4, Main Hall",
            eventDateLabel = "2026-11-28",
            startTimeLabel = "09:00",
            endTimeLabel = "10:30",
            category = "MEETING",
            gradeLevel = "Grade 7",
            subject = "",
            sourceType = "MANUAL",
            sourceId = ""
        )
        val syncedAssignmentEvent = SchoolCalendarEvent(
            id = "ev2",
            title = "Herrega: Algebra Quiz Prep",
            description = "Already synced",
            eventDateLabel = "2026-11-29",
            startTimeLabel = "08:30",
            endTimeLabel = "09:30",
            category = "ASSIGNMENT",
            gradeLevel = "Grade 7",
            subject = "Herrega",
            sourceType = "ASSIGNMENT",
            sourceId = "hw_synced"
        )
        val assignments = listOf(
            Assignment(
                id = "hw_synced",
                title = "Algebra Quiz Prep",
                subject = "Herrega",
                gradeLevel = "Grade 7",
                dueDateLabel = "2026-11-29"
            ),
            Assignment(
                id = "hw_new",
                title = "Gadaa Essay",
                description = "Write 2 pages",
                subject = "Gadaa",
                gradeLevel = "Grade 8",
                dueDateLabel = "2026-12-01"
            )
        )

        val ics = CalendarSyncHelper.buildIcsContent(
            events = listOf(syncedEvent, syncedAssignmentEvent),
            assignments = assignments
        )

        assertTrue(ics.startsWith("BEGIN:VCALENDAR"))
        assertTrue(ics.trimEnd().endsWith("END:VCALENDAR"))
        assertTrue(ics.contains("SUMMARY:Parent Meeting\\; Fall"))
        assertTrue(ics.contains("DESCRIPTION:Grade: Grade 7 | Room 4\\, Main Hall"))
        assertTrue(ics.contains("UID:assign_hw_new@kofket.school"))
        assertTrue(ics.contains("SUMMARY:Gadaa: Gadaa Essay"))
        val eventCount = Regex("BEGIN:VEVENT").findAll(ics).count()
        assertEquals(3, eventCount)
    }

    @Test
    fun `parseEventEpochMillis parses full datetime and falls back to date-only`() {
        val fullMs = CalendarSyncHelper.parseEventEpochMillis("2026-11-28", "09:00")
        val dateOnlyMs = CalendarSyncHelper.parseEventEpochMillis("2026-11-28", "invalid")
        assertTrue(fullMs > dateOnlyMs)
        assertEquals(30 * 60 * 1000L, fullMs - dateOnlyMs)
    }
}
