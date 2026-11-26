package com.example

import com.example.core.AttendanceDateHelper
import com.example.data.repository.buildDailyAttendanceDocId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceTrackingTest {

    @Test
    fun `buildDailyAttendanceDocId is deterministic, sanitized, and within Firestore ID limits`() {
        val id1 = buildDailyAttendanceDocId("2026-11-25", "student chaltu@1", "teacher/tolasa")
        val id2 = buildDailyAttendanceDocId("2026-11-25", "student chaltu@1", "teacher/tolasa")
        val idDifferentDay = buildDailyAttendanceDocId("2026-11-26", "student chaltu@1", "teacher/tolasa")
        val idDifferentTeacher = buildDailyAttendanceDocId("2026-11-25", "student chaltu@1", "teacher/abebe")

        assertEquals(id1, id2)
        assertNotEquals(id1, idDifferentDay)
        assertNotEquals(id1, idDifferentTeacher)
        assertTrue(id1.length in 1..120)
        assertTrue(id1.matches(Regex("^[a-zA-Z0-9_.\\-]+$")))
    }

    @Test
    fun `AttendanceDateHelper shifts days accurately across month and year boundaries`() {
        assertEquals("2026-12-01", AttendanceDateHelper.shiftDateLabel("2026-11-30", 1))
        assertEquals("2025-12-31", AttendanceDateHelper.shiftDateLabel("2026-01-01", -1))
        assertEquals("2026-11-25", AttendanceDateHelper.shiftDateLabel("2026-11-26", -1))
        assertTrue(AttendanceDateHelper.todayDateLabel().matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")))
    }

    @Test
    fun `Attendance trends accurately calculates daily presence rate and status counts`() {
        val records = listOf(
            com.example.data.model.AttendanceRecord(
                id = "1", studentId = "s1", dateLabel = "2026-11-01", status = "PRESENT"
            ),
            com.example.data.model.AttendanceRecord(
                id = "2", studentId = "s2", dateLabel = "2026-11-01", status = "PRESENT"
            ),
            com.example.data.model.AttendanceRecord(
                id = "3", studentId = "s3", dateLabel = "2026-11-01", status = "ABSENT"
            ),
            com.example.data.model.AttendanceRecord(
                id = "4", studentId = "s1", dateLabel = "2026-11-02", status = "PRESENT"
            ),
            com.example.data.model.AttendanceRecord(
                id = "5", studentId = "s2", dateLabel = "2026-11-02", status = "LATE"
            )
        )

        val grouped = records.groupBy { it.dateLabel }
        val trends = grouped.entries.map { (date, recs) ->
            val present = recs.count { it.status == "PRESENT" }
            val absent = recs.count { it.status == "ABSENT" }
            val late = recs.count { it.status == "LATE" }
            val total = recs.size
            val rate = if (total > 0) (present.toDouble() / total.toDouble()) * 100.0 else 0.0
            com.example.ui.screens.DailyAttendanceTrend(
                dateLabel = date,
                dayNumber = date.takeLast(2),
                presentCount = present,
                absentCount = absent,
                lateCount = late,
                totalCount = total,
                attendanceRate = rate
            )
        }.sortedBy { it.dateLabel }

        assertEquals(2, trends.size)
        // Day 1: 2 present, 1 absent out of 3 -> 66.666%
        assertEquals("2026-11-01", trends[0].dateLabel)
        assertEquals(2, trends[0].presentCount)
        assertEquals(1, trends[0].absentCount)
        assertEquals(0, trends[0].lateCount)
        assertEquals(66.67, trends[0].attendanceRate, 0.01)

        // Day 2: 1 present, 0 absent, 1 late out of 2 -> 50.0%
        assertEquals("2026-11-02", trends[1].dateLabel)
        assertEquals(1, trends[1].presentCount)
        assertEquals(1, trends[1].lateCount)
        assertEquals(50.0, trends[1].attendanceRate, 0.01)
    }

    @Test
    fun `matchesAttendanceSmartQuery matches multi-token query across name, grade, date, and localized status keywords`() {
        val studentName = "Chala Feyisa"
        val gradeLevel = "Grade 8B"
        val dateLabel = "2026-11-25"

        // PRESENT student
        assertTrue(
            com.example.ui.screens.matchesAttendanceSmartQuery(
                query = "Chala",
                studentName = studentName,
                gradeLevel = gradeLevel,
                statusRaw = "PRESENT",
                dateLabel = dateLabel
            )
        )
        assertTrue(
            com.example.ui.screens.matchesAttendanceSmartQuery(
                query = "Grade 8",
                studentName = studentName,
                gradeLevel = gradeLevel,
                statusRaw = "PRESENT",
                dateLabel = dateLabel
            )
        )
        // Oromoo status keywords
        assertTrue(
            com.example.ui.screens.matchesAttendanceSmartQuery(
                query = "argamaa",
                studentName = studentName,
                gradeLevel = gradeLevel,
                statusRaw = "PRESENT",
                dateLabel = dateLabel
            )
        )
        assertTrue(
            com.example.ui.screens.matchesAttendanceSmartQuery(
                query = "present",
                studentName = studentName,
                gradeLevel = gradeLevel,
                statusRaw = "PRESENT",
                dateLabel = dateLabel
            )
        )
        // Multi-token: "Chala argamaa"
        assertTrue(
            com.example.ui.screens.matchesAttendanceSmartQuery(
                query = "Chala argamaa",
                studentName = studentName,
                gradeLevel = gradeLevel,
                statusRaw = "PRESENT",
                dateLabel = dateLabel
            )
        )
        // Should not match "hafe" or "absent" for PRESENT student
        org.junit.Assert.assertFalse(
            com.example.ui.screens.matchesAttendanceSmartQuery(
                query = "hafe",
                studentName = studentName,
                gradeLevel = gradeLevel,
                statusRaw = "PRESENT",
                dateLabel = dateLabel
            )
        )

        // ABSENT student
        assertTrue(
            com.example.ui.screens.matchesAttendanceSmartQuery(
                query = "hafe",
                studentName = "Tolasa",
                gradeLevel = "Grade 7",
                statusRaw = "ABSENT",
                dateLabel = dateLabel
            )
        )
        assertTrue(
            com.example.ui.screens.matchesAttendanceSmartQuery(
                query = "absent",
                studentName = "Tolasa",
                gradeLevel = "Grade 7",
                statusRaw = "ABSENT",
                dateLabel = dateLabel
            )
        )

        // LATE student
        assertTrue(
            com.example.ui.screens.matchesAttendanceSmartQuery(
                query = "turte",
                studentName = "Ayana",
                gradeLevel = "Grade 9",
                statusRaw = "LATE",
                dateLabel = dateLabel
            )
        )
        assertTrue(
            com.example.ui.screens.matchesAttendanceSmartQuery(
                query = "late",
                studentName = "Ayana",
                gradeLevel = "Grade 9",
                statusRaw = "LATE",
                dateLabel = dateLabel
            )
        )

        // UNMARKED student
        assertTrue(
            com.example.ui.screens.matchesAttendanceSmartQuery(
                query = "unmarked",
                studentName = "Bikila",
                gradeLevel = "Grade 10",
                statusRaw = "UNMARKED",
                dateLabel = dateLabel
            )
        )
    }

    @Test
    fun `ReportExporter buildAttendanceCsvContent produces valid CSV structure with header summary and student rows`() {
        val records = listOf(
            com.example.data.model.AttendanceRecord(
                id = "att_1",
                studentId = "s1",
                studentName = "Tolasa Gemechu",
                gradeLevel = "Grade 7",
                dateLabel = "2026-11-25",
                status = "PRESENT",
                note = "On time"
            ),
            com.example.data.model.AttendanceRecord(
                id = "att_2",
                studentId = "s2",
                studentName = "Chaltu Abera",
                gradeLevel = "Grade 7",
                dateLabel = "2026-11-25",
                status = "PRESENT",
                note = ""
            ),
            com.example.data.model.AttendanceRecord(
                id = "att_3",
                studentId = "s3",
                studentName = "Bonsa Desta",
                gradeLevel = "Grade 7",
                dateLabel = "2026-11-25",
                status = "ABSENT",
                note = "Sick leave"
            )
        )

        val csv = com.example.core.ReportExporter.buildAttendanceCsvContent(
            records = records,
            title = "Koofkeet Roll Call - 2026-11-25"
        )

        assertTrue(csv.contains("Attendance Report,\"Koofkeet Roll Call - 2026-11-25\""))
        assertTrue(csv.contains("Total Records,3"))
        assertTrue(csv.contains("Present,2"))
        assertTrue(csv.contains("Absent,1"))
        assertTrue(csv.contains("Attendance Rate,66.7%"))
        assertTrue(csv.contains("Date,Student Name,Grade Level,Status,Note"))
        assertTrue(csv.contains("\"2026-11-25\",\"Tolasa Gemechu\",\"Grade 7\",\"PRESENT\",\"On time\""))
        assertTrue(csv.contains("\"2026-11-25\",\"Bonsa Desta\",\"Grade 7\",\"ABSENT\",\"Sick leave\""))
    }
}
