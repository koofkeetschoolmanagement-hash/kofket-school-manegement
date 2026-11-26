package com.example

import com.example.core.AppThemeMode
import com.example.core.GradeBand
import com.example.core.SchoolCurriculum
import org.junit.Assert.assertEquals
import org.junit.Test

class CurriculumAndThemeTest {

    @Test
    fun `grades 1 to 6 contain exact curriculum subjects`() {
        val expected = listOf(
            "Afaan Oromoo",
            "English",
            "Afaan Amaaraa",
            "Saayinsii Naannoo",
            "Herrega",
            "Gadaafi Safuu",
            "Og-aartii",
            "FJQ"
        )
        assertEquals(expected, SchoolCurriculum.grades1To6Subjects)
        assertEquals(expected, SchoolCurriculum.subjectsForGrade("Grade 1"))
        assertEquals(expected, SchoolCurriculum.subjectsForGrade("Kutaa 4"))
        assertEquals(expected, SchoolCurriculum.subjectsForGrade("Grade 6"))
        assertEquals(GradeBand.GRADES_1_TO_6, SchoolCurriculum.bandForGrade("Kutaa 6"))
    }

    @Test
    fun `grades 7 to 8 contain exact curriculum subjects`() {
        val expected = listOf(
            "Afaan Oromoo",
            "Afaan Amaaraa",
            "Herrega",
            "English",
            "Saayinsii waliigalaa",
            "ICT",
            "Gadaa",
            "Lammummaa",
            "Og-aartii",
            "BOT",
            "FJQ",
            "Hawaasa"
        )
        assertEquals(expected, SchoolCurriculum.grades7To8Subjects)
        assertEquals(expected, SchoolCurriculum.subjectsForGrade("Grade 7"))
        assertEquals(expected, SchoolCurriculum.subjectsForGrade("Kutaa 8B"))
        assertEquals(GradeBand.GRADES_7_TO_8, SchoolCurriculum.bandForGrade("Grade 7"))
    }

    @Test
    fun `app theme mode round-trips storage keys cleanly`() {
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromKey(null))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromKey("unknown"))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromKey(AppThemeMode.LIGHT.storageKey))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromKey(AppThemeMode.DARK.storageKey))
    }
}
