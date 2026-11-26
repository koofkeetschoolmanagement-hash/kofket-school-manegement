package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val SCHOOL_ID_MAIN = "koofkeet_main"
const val BOOTSTRAP_ADMIN_EMAIL = "koofkeetschoolmanagement@gmail.com"

enum class UserRole(val labelOromo: String, val labelEnglish: String) {
    STUDENT("Barataa", "Student"),
    TEACHER("Barsiisaa", "Teacher"),
    PARENT("Maatii Barataa", "Parent"),
    ADMIN("Bulchiinsa Mana Barumsaa", "School Admin");

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.name == raw } ?: STUDENT
    }
}

enum class AttendanceStatus(val labelOromo: String) {
    PRESENT("Argamaa (Present)"),
    ABSENT("Hafeera (Absent)"),
    LATE("Turteera (Late)");

    companion object {
        fun fromRaw(raw: String?): AttendanceStatus =
            entries.firstOrNull { it.name == raw } ?: PRESENT
    }
}

enum class MaterialType(val labelOromo: String) {
    PDF("PDF"),
    BOOK("Kitaaba"),
    NOTES("Yaadannoo");

    companion object {
        fun fromRaw(raw: String?): MaterialType =
            entries.firstOrNull { it.name == raw } ?: PDF
    }
}

enum class AnnouncementCategory(val labelOromo: String) {
    GENERAL("Waliigalaa"),
    EXAM("Qormaata"),
    EVENT("Sagantaa"),
    URGENT("Ariifachiisaa");

    companion object {
        fun fromRaw(raw: String?): AnnouncementCategory =
            entries.firstOrNull { it.name == raw } ?: GENERAL
    }
}

@Keep
@IgnoreExtraProperties
data class SchoolMeta(
    @DocumentId val id: String = "",
    val schoolId: String = SCHOOL_ID_MAIN,
    val adminUid: String = "",
    val adminEmail: String = "",
    val schoolName: String = "Mana Barumsaa Koofkeet",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoleGrant(
    @DocumentId val id: String = "",
    val email: String = "",
    val role: String = UserRole.STUDENT.name,
    val fullName: String = "",
    val gradeLevel: String = "",
    val subject: String = "",
    val linkedStudentId: String = "",
    val linkedStudentName: String = "",
    val adminId: String = "",
    val claimedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = UserRole.STUDENT.name,
    val preferredLanguage: String = "om",
    val gradeLevel: String = "",
    val subject: String = "",
    val linkedStudentId: String = "",
    val linkedStudentName: String = "",
    val adminId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val userRole: UserRole get() = UserRole.fromRaw(role)
}

@Keep
@IgnoreExtraProperties
data class AiChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val prompt: String = "",
    val response: String = "",
    val subject: String = "Waliigalaa",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TeacherAssignment(
    @DocumentId val id: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val subject: String = "",
    val gradeLevel: String = "",
    val schedule: String = "",
    val adminId: String = "",
    val schoolId: String = SCHOOL_ID_MAIN,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentRegistration(
    @DocumentId val id: String = "",
    val studentUid: String = "",
    val studentName: String = "",
    val studentCode: String = "",
    val gradeLevel: String = "",
    val section: String = "A",
    val parentUid: String = "",
    val parentName: String = "",
    val adminId: String = "",
    val teacherUids: List<String> = emptyList(),
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Assignment(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val subject: String = "",
    val gradeLevel: String = "",
    val dueDateLabel: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val adminId: String = "",
    val schoolId: String = SCHOOL_ID_MAIN,
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AssignmentSubmission(
    @DocumentId val id: String = "",
    val assignmentId: String = "",
    val assignmentTitle: String = "",
    val subject: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val parentId: String = "",
    val teacherId: String = "",
    val adminId: String = "",
    val answerText: String = "",
    val score: Double = 0.0,
    val feedback: String = "",
    val status: String = "SUBMITTED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizQuestion(
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0
)

@Keep
@IgnoreExtraProperties
data class Quiz(
    @DocumentId val id: String = "",
    val title: String = "",
    val subject: String = "",
    val gradeLevel: String = "",
    val durationMinutes: Long = 20L,
    val questions: List<Map<String, Any>> = emptyList(),
    val teacherId: String = "",
    val teacherName: String = "",
    val adminId: String = "",
    val schoolId: String = SCHOOL_ID_MAIN,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedQuestions(): List<QuizQuestion> = questions.mapNotNull { map ->
        val prompt = map["prompt"] as? String ?: return@mapNotNull null
        val rawOptions = (map["options"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        val correctIdx = (map["correctIndex"] as? Number)?.toInt() ?: 0
        if (rawOptions.isEmpty()) null else QuizQuestion(prompt, rawOptions, correctIdx)
    }
}

@Keep
@IgnoreExtraProperties
data class QuizAttempt(
    @DocumentId val id: String = "",
    val quizId: String = "",
    val quizTitle: String = "",
    val subject: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val parentId: String = "",
    val teacherId: String = "",
    val adminId: String = "",
    val score: Double = 0.0,
    val totalQuestions: Long = 1L,
    val correctCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExamResult(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val parentId: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val adminId: String = "",
    val subject: String = "",
    val gradeLevel: String = "",
    val examName: String = "",
    val monthLabel: String = "",
    val score: Double = 0.0,
    val maxScore: Double = 100.0,
    val remarks: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val parentId: String = "",
    val teacherId: String = "",
    val adminId: String = "",
    val gradeLevel: String = "",
    val dateLabel: String = "",
    val status: String = AttendanceStatus.PRESENT.name,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LearningMaterial(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val subject: String = "",
    val gradeLevel: String = "",
    val materialType: String = MaterialType.PDF.name,
    val resourceUrl: String = "",
    val contentBody: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val adminId: String = "",
    val schoolId: String = SCHOOL_ID_MAIN,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EarlyWarning(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val gradeLevel: String = "",
    val subject: String = "",
    val previousAverage: Double = 0.0,
    val currentScore: Double = 0.0,
    val dropAmount: Double = 0.0,
    val reason: String = "",
    val teacherId: String = "",
    val adminId: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ParentMessage(
    @DocumentId val id: String = "",
    val parentId: String = "",
    val parentName: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val recipientRole: String = "TEACHER",
    val recipientId: String = "",
    val participantUids: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "PARENT",
    val subject: String = "",
    val body: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = AnnouncementCategory.GENERAL.name,
    val authorId: String = "",
    val authorName: String = "",
    val adminId: String = "",
    val schoolId: String = SCHOOL_ID_MAIN,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class CalendarEventCategory(val labelOromo: String, val labelEnglish: String) {
    GENERAL("Waliigalaa", "General"),
    EXAM("Qormaata", "Exam"),
    ASSIGNMENT("Hojii Manaa", "Assignment"),
    HOLIDAY("Ayyaana / Boqonnaa", "Holiday"),
    MEETING("Walgahii Maatii", "Meeting");

    companion object {
        fun fromRaw(raw: String?): CalendarEventCategory =
            entries.firstOrNull { it.name == raw } ?: GENERAL
    }
}

@Keep
@IgnoreExtraProperties
data class SchoolCalendarEvent(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val eventDateLabel: String = "",
    val startTimeLabel: String = "08:30",
    val endTimeLabel: String = "09:30",
    val category: String = CalendarEventCategory.GENERAL.name,
    val gradeLevel: String = "Hunda",
    val subject: String = "",
    val sourceType: String = "MANUAL",
    val sourceId: String = "",
    val createdByUid: String = "",
    val createdByName: String = "",
    val adminId: String = "",
    val schoolId: String = SCHOOL_ID_MAIN,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class SyncPhase {
    SYNCED,
    SYNCING,
    OFFLINE_PENDING,
    ERROR
}

data class AttendanceSyncStatus(
    val phase: SyncPhase = SyncPhase.SYNCED,
    val pendingWriteCount: Int = 0,
    val isFromCache: Boolean = false,
    val totalRecords: Int = 0,
    val lastSyncedMillis: Long = 0L,
    val message: String = ""
)

fun serverTime(): Any = FieldValue.serverTimestamp()

