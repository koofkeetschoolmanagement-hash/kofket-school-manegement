package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.AiChatMessage
import com.example.data.model.Announcement
import com.example.data.model.AnnouncementCategory
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.BOOTSTRAP_ADMIN_EMAIL
import com.example.data.model.EarlyWarning
import com.example.data.model.ExamResult
import com.example.data.model.LearningMaterial
import com.example.data.model.MaterialType
import com.example.data.model.ParentMessage
import com.example.data.model.SCHOOL_ID_MAIN
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.serverTime
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CommunicationAndRecordsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun <T> Flow<T>.handleStreamErrors(op: OperationType, path: String, uid: String): Flow<T> =
        catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val toLog = fsError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    fun observeExamResults(profile: UserProfile): Flow<List<ExamResult>> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        val query = when (profile.userRole) {
            UserRole.ADMIN -> if (profile.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)) {
                db.collection("exam_results")
            } else {
                db.collection("exam_results").whereEqualTo("adminId", uid)
            }
            UserRole.TEACHER -> db.collection("exam_results").whereEqualTo("teacherId", uid)
            UserRole.PARENT -> db.collection("exam_results").whereEqualTo("parentId", uid)
            UserRole.STUDENT -> db.collection("exam_results").whereEqualTo("studentId", uid)
        }
        return query.snapshots()
            .map { snap -> snap.toObjects(ExamResult::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.LIST, "exam_results", uid)
    }

    /**
     * Records an exam score and evaluates the Early Warning System (#8):
     * If the student's new score is < 50.0 OR dropped >= 15.0 points below their prior average,
     * automatically creates an Early Warning Notification for the teacher and school admin.
     */
    suspend fun recordExamResultWithEarlyWarning(
        studentId: String,
        studentName: String,
        parentId: String,
        teacherName: String,
        adminId: String,
        subject: String,
        gradeLevel: String,
        examName: String,
        monthLabel: String,
        score: Double,
        remarks: String,
        existingResultsForStudent: List<ExamResult>
    ): Result<Boolean> = try {
        val uid = requireUserId()
        val clampedScore = score.coerceIn(0.0, 100.0)
        val resultRef = db.collection("exam_results").document()
        val batch = db.batch()

        val resultPayload = mapOf(
            "studentId" to studentId,
            "studentName" to studentName.trim(),
            "parentId" to parentId.trim().ifBlank { null },
            "teacherId" to uid,
            "teacherName" to teacherName.trim(),
            "adminId" to adminId.ifBlank { uid },
            "subject" to subject.trim(),
            "gradeLevel" to gradeLevel.trim(),
            "examName" to examName.trim(),
            "monthLabel" to monthLabel.trim(),
            "score" to clampedScore,
            "maxScore" to 100.0,
            "remarks" to remarks.trim().ifBlank { null },
            "createdAt" to serverTime(),
            "updatedAt" to serverTime()
        ).filterValues { it != null } as Map<String, Any>

        batch.set(resultRef, resultPayload)

        val priorScores = existingResultsForStudent.map { it.score }
        val prevAvg = if (priorScores.isNotEmpty()) priorScores.average() else clampedScore
        val dropAmount = (prevAvg - clampedScore).coerceAtLeast(0.0)
        val triggeredWarning = clampedScore < 50.0 || (priorScores.isNotEmpty() && dropAmount >= 15.0)

        if (triggeredWarning) {
            val warningRef = db.collection("early_warnings").document()
            val reason = if (priorScores.isNotEmpty() && dropAmount >= 15.0) {
                "Gahumsi barataa $subject irratti ${"%.1f".format(prevAvg)}% irraa gara ${"%.1f".format(clampedScore)}% gadi bu'eera (Deeggarsa dabalataa barbaachisa)."
            } else {
                "Qabxiin $subject (${"%.1f".format(clampedScore)}%) sadarkaa gadi aanaa irra jira; deeggarsi dabalataa barbaachisa."
            }
            batch.set(
                warningRef,
                mapOf(
                    "studentId" to studentId,
                    "studentName" to studentName.trim(),
                    "gradeLevel" to gradeLevel.trim(),
                    "subject" to subject.trim(),
                    "previousAverage" to prevAvg,
                    "currentScore" to clampedScore,
                    "dropAmount" to dropAmount,
                    "reason" to reason,
                    "teacherId" to uid,
                    "adminId" to adminId.ifBlank { uid },
                    "status" to "OPEN",
                    "createdAt" to serverTime(),
                    "updatedAt" to serverTime()
                )
            )
        }

        batch.commit().await()
        Result.success(triggeredWarning)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "exam_results")
        Result.failure(e)
    }

    fun observeAttendance(profile: UserProfile): Flow<List<AttendanceRecord>> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        val query = when (profile.userRole) {
            UserRole.ADMIN -> if (profile.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)) {
                db.collection("attendance")
            } else {
                db.collection("attendance").whereEqualTo("adminId", uid)
            }
            UserRole.TEACHER -> db.collection("attendance").whereEqualTo("teacherId", uid)
            UserRole.PARENT -> db.collection("attendance").whereEqualTo("parentId", uid)
            UserRole.STUDENT -> db.collection("attendance").whereEqualTo("studentId", uid)
        }
        return query.snapshots()
            .map { snap -> snap.toObjects(AttendanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.LIST, "attendance", uid)
    }

    suspend fun recordAttendance(
        studentId: String,
        studentName: String,
        parentId: String,
        adminId: String,
        gradeLevel: String,
        dateLabel: String,
        status: AttendanceStatus,
        note: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        val ref = db.collection("attendance").document()
        val payload = buildMap<String, Any> {
            put("studentId", studentId)
            put("studentName", studentName.trim())
            parentId.trim().takeIf { it.isNotEmpty() }?.let { put("parentId", it) }
            put("teacherId", uid)
            put("adminId", adminId.ifBlank { uid })
            put("gradeLevel", gradeLevel.trim())
            put("dateLabel", dateLabel.trim())
            put("status", status.name)
            note.trim().takeIf { it.isNotEmpty() }?.let { put("note", it) }
            put("createdAt", serverTime())
            put("updatedAt", serverTime())
        }
        ref.set(payload).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "attendance")
        Result.failure(e)
    }

    fun observeLearningMaterials(): Flow<List<LearningMaterial>> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        return db.collection("learning_materials")
            .whereEqualTo("schoolId", SCHOOL_ID_MAIN)
            .snapshots()
            .map { snap -> snap.toObjects(LearningMaterial::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.LIST, "learning_materials", uid)
    }

    suspend fun addLearningMaterial(
        title: String,
        description: String,
        subject: String,
        gradeLevel: String,
        materialType: MaterialType,
        resourceUrl: String,
        contentBody: String,
        teacherName: String,
        adminId: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        val ref = db.collection("learning_materials").document()
        val payload = buildMap<String, Any> {
            put("title", title.trim())
            put("description", description.trim())
            put("subject", subject.trim())
            put("gradeLevel", gradeLevel.trim())
            put("materialType", materialType.name)
            resourceUrl.trim().takeIf { it.isNotEmpty() }?.let { put("resourceUrl", it) }
            contentBody.trim().takeIf { it.isNotEmpty() }?.let { put("contentBody", it) }
            put("teacherId", uid)
            put("teacherName", teacherName.trim())
            put("adminId", adminId.ifBlank { uid })
            put("schoolId", SCHOOL_ID_MAIN)
            put("createdAt", serverTime())
            put("updatedAt", serverTime())
        }
        ref.set(payload).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "learning_materials")
        Result.failure(e)
    }

    fun observeEarlyWarnings(profile: UserProfile): Flow<List<EarlyWarning>> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        if (profile.userRole == UserRole.STUDENT || profile.userRole == UserRole.PARENT) {
            return emptyFlow()
        }
        val query = when (profile.userRole) {
            UserRole.ADMIN -> if (profile.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)) {
                db.collection("early_warnings")
            } else {
                db.collection("early_warnings").whereEqualTo("adminId", uid)
            }
            UserRole.TEACHER -> db.collection("early_warnings").whereEqualTo("teacherId", uid)
            else -> return emptyFlow()
        }
        return query.snapshots()
            .map { snap -> snap.toObjects(EarlyWarning::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.LIST, "early_warnings", uid)
    }

    suspend fun resolveEarlyWarning(warningId: String): Result<Unit> = try {
        requireUserId()
        db.collection("early_warnings").document(warningId)
            .update(mapOf("status" to "RESOLVED", "updatedAt" to serverTime()))
            .await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "early_warnings/$warningId")
        Result.failure(e)
    }

    fun observeParentMessages(profile: UserProfile): Flow<List<ParentMessage>> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        if (profile.userRole == UserRole.STUDENT) return emptyFlow()
        val query = if (profile.userRole == UserRole.ADMIN &&
            profile.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)
        ) {
            db.collection("parent_messages")
        } else {
            db.collection("parent_messages").whereArrayContains("participantUids", uid)
        }
        return query.snapshots()
            .map { snap -> snap.toObjects(ParentMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.LIST, "parent_messages", uid)
    }

    suspend fun sendParentMessage(
        parentId: String,
        parentName: String,
        studentId: String,
        studentName: String,
        recipientRole: String,
        recipientId: String,
        senderName: String,
        senderRole: UserRole,
        subject: String,
        body: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        val participants = listOf(parentId, recipientId, uid).filter { it.isNotBlank() }.distinct().take(4)
        require(studentId !in participants) { "Barataan qunnamtii maatii argachuu hin danda'u" }
        val ref = db.collection("parent_messages").document()
        ref.set(
            mapOf(
                "parentId" to parentId,
                "parentName" to parentName.trim(),
                "studentId" to studentId,
                "studentName" to studentName.trim(),
                "recipientRole" to recipientRole,
                "recipientId" to recipientId,
                "participantUids" to participants,
                "senderId" to uid,
                "senderName" to senderName.trim(),
                "senderRole" to senderRole.name,
                "subject" to subject.trim(),
                "body" to body.trim(),
                "createdAt" to serverTime(),
                "updatedAt" to serverTime()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "parent_messages")
        Result.failure(e)
    }

    fun observeAnnouncements(): Flow<List<Announcement>> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        return db.collection("announcements")
            .whereEqualTo("schoolId", SCHOOL_ID_MAIN)
            .snapshots()
            .map { snap -> snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.LIST, "announcements", uid)
    }

    suspend fun postAnnouncement(
        title: String,
        body: String,
        category: AnnouncementCategory,
        authorName: String,
        adminId: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        val ref = db.collection("announcements").document()
        ref.set(
            mapOf(
                "title" to title.trim(),
                "body" to body.trim(),
                "category" to category.name,
                "authorId" to uid,
                "authorName" to authorName.trim(),
                "adminId" to adminId.ifBlank { uid },
                "schoolId" to SCHOOL_ID_MAIN,
                "createdAt" to serverTime(),
                "updatedAt" to serverTime()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "announcements")
        Result.failure(e)
    }

    fun observeAiChats(userId: String): Flow<List<AiChatMessage>> =
        db.collection("users").document(userId).collection("ai_chats")
            .snapshots()
            .map { snap -> snap.toObjects(AiChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.LIST, "users/$userId/ai_chats", userId)

    suspend fun saveAiChat(prompt: String, response: String, subject: String): Result<Unit> = try {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("ai_chats").document()
        ref.set(
            mapOf(
                "userId" to uid,
                "prompt" to prompt.take(2000),
                "response" to response.take(8000),
                "subject" to subject.take(60),
                "createdAt" to serverTime()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/ai_chats")
        Result.failure(e)
    }
}
