package com.example.core

enum class GradeBand {
    GRADES_1_TO_6,
    GRADES_7_TO_8,
    OTHER
}

object SchoolCurriculum {

    val grades1To6Subjects: List<String> = listOf(
        "Afaan Oromoo",
        "English",
        "Afaan Amaaraa",
        "Saayinsii Naannoo",
        "Herrega",
        "Gadaafi Safuu",
        "Og-aartii",
        "FJQ"
    )

    val grades7To8Subjects: List<String> = listOf(
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

    val generalSubjects: List<String> = (grades1To6Subjects + grades7To8Subjects).distinct()

    fun detectGradeNumber(gradeLevel: String): Int? {
        val match = Regex("\\d+").find(gradeLevel.trim()) ?: return null
        return match.value.toIntOrNull()
    }

    fun bandForGrade(gradeLevel: String): GradeBand {
        val num = detectGradeNumber(gradeLevel) ?: return GradeBand.OTHER
        return when (num) {
            in 1..6 -> GradeBand.GRADES_1_TO_6
            in 7..8 -> GradeBand.GRADES_7_TO_8
            else -> GradeBand.OTHER
        }
    }

    fun subjectsForGrade(gradeLevel: String): List<String> = when (bandForGrade(gradeLevel)) {
        GradeBand.GRADES_1_TO_6 -> grades1To6Subjects
        GradeBand.GRADES_7_TO_8 -> grades7To8Subjects
        GradeBand.OTHER -> generalSubjects
    }

    fun subjectsForBand(band: GradeBand): List<String> = when (band) {
        GradeBand.GRADES_1_TO_6 -> grades1To6Subjects
        GradeBand.GRADES_7_TO_8 -> grades7To8Subjects
        GradeBand.OTHER -> generalSubjects
    }
}
