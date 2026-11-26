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
}
