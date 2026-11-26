package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.AiChatMessage
import com.example.data.model.Announcement
import com.example.data.model.AnnouncementCategory
import com.example.data.model.Assignment
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceSyncStatus
import com.example.data.model.BOOTSTRAP_ADMIN_EMAIL
import com.example.data.model.CalendarEventCategory
import com.example.data.model.EarlyWarning
import com.example.data.model.ExamResult
import com.example.data.model.LearningMaterial
import com.example.data.model.MaterialType
import com.example.data.model.ParentMessage
import com.example.data.model.Quiz
import com.example.data.model.SCHOOL_ID_MAIN
import com.example.data.model.SchoolCalendarEvent
import com.example.data.model.SyncPhase
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.serverTime
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

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
        val cleanStudentName = studentName.trim().take(100)
        val cleanGrade = gradeLevel.trim().take(40).ifBlank { "Grade 1" }
        val cleanSubject = subject.trim().take(60)
        val cleanExam = examName.trim().take(100).ifBlank { "Assessment" }
        val cleanMonth = monthLabel.trim().take(40).ifBlank { "2026-11" }
        require(cleanStudentName.length >= 2) { "Student name must be at least 2 characters" }
        require(cleanSubject.length >= 2) { "Subject must be at least 2 characters" }

        val resolvedStudentId = studentId.trim().take(128).ifBlank {
            "manual_${cleanStudentName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "student" }}"
        }
        val resultRef = db.collection("exam_results").document()
        val batch = db.batch()

        val resultPayload = mapOf(
            "studentId" to resolvedStudentId,
            "studentName" to cleanStudentName,
            "parentId" to parentId.trim().take(128).ifBlank { null },
            "teacherId" to uid,
            "teacherName" to teacherName.trim().take(100).ifBlank { "Teacher" },
            "adminId" to adminId.ifBlank { uid }.take(128),
            "subject" to cleanSubject,
            "gradeLevel" to cleanGrade,
            "examName" to cleanExam,
            "monthLabel" to cleanMonth,
            "score" to clampedScore,
            "maxScore" to 100.0,
            "remarks" to remarks.trim().take(500).ifBlank { null },
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
                "Gahumsi barataa $cleanSubject irratti ${"%.1f".format(prevAvg)}% irraa gara ${"%.1f".format(clampedScore)}% gadi bu'eera (Deeggarsa dabalataa barbaachisa)."
            } else {
                "Qabxiin $cleanSubject (${"%.1f".format(clampedScore)}%) sadarkaa gadi aanaa irra jira; deeggarsi dabalataa barbaachisa."
            }
            batch.set(
                warningRef,
                mapOf(
                    "studentId" to resolvedStudentId,
                    "studentName" to cleanStudentName,
                    "gradeLevel" to cleanGrade,
                    "subject" to cleanSubject,
                    "previousAverage" to prevAvg,
                    "currentScore" to clampedScore,
                    "dropAmount" to dropAmount,
                    "reason" to reason.take(500),
                    "teacherId" to uid,
                    "adminId" to adminId.ifBlank { uid }.take(128),
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

    suspend fun updateExamResult(
        resultId: String,
        score: Double,
        remarks: String
    ): Result<Unit> = try {
        requireUserId()
        require(resultId.isNotBlank()) { "Result ID is required" }
        val updates = buildMap<String, Any> {
            put("score", score.coerceIn(0.0, 100.0))
            put("remarks", remarks.trim().take(500))
            put("updatedAt", serverTime())
        }
        db.collection("exam_results").document(resultId).update(updates).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "exam_results/$resultId")
        Result.failure(e)
    }

    suspend fun deleteExamResult(resultId: String): Result<Unit> = try {
        requireUserId()
        require(resultId.isNotBlank()) { "Result ID is required" }
        db.collection("exam_results").document(resultId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "exam_results/$resultId")
        Result.failure(e)
    }

    private fun buildAttendanceQuery(profile: UserProfile, uid: String): Query = when (profile.userRole) {
        UserRole.ADMIN -> if (profile.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)) {
            db.collection("attendance")
        } else {
            db.collection("attendance").whereEqualTo("adminId", uid)
        }
        UserRole.TEACHER -> db.collection("attendance").whereEqualTo("teacherId", uid)
        UserRole.PARENT -> db.collection("attendance").whereEqualTo("parentId", uid)
        UserRole.STUDENT -> db.collection("attendance").whereEqualTo("studentId", uid)
    }

    fun observeAttendance(profile: UserProfile): Flow<List<AttendanceRecord>> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        return buildAttendanceQuery(profile, uid)
            .snapshots()
            .map { snap -> snap.toObjects(AttendanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.LIST, "attendance", uid)
    }

    fun observeAttendanceSyncStatus(profile: UserProfile): Flow<AttendanceSyncStatus> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        return buildAttendanceQuery(profile, uid)
            .snapshots(MetadataChanges.INCLUDE)
            .map { snap ->
                val pendingCount = snap.documents.count { it.metadata.hasPendingWrites() }
                val hasPending = snap.metadata.hasPendingWrites() || pendingCount > 0
                val fromCache = snap.metadata.isFromCache
                AttendanceSyncStatus(
                    phase = if (hasPending) SyncPhase.OFFLINE_PENDING else SyncPhase.SYNCED,
                    pendingWriteCount = pendingCount,
                    isFromCache = fromCache,
                    totalRecords = snap.size(),
                    lastSyncedMillis = if (!hasPending && !fromCache) System.currentTimeMillis() else 0L
                )
            }
            .catch { /* Keep last status if metadata stream errors */ }
    }

    suspend fun syncAttendanceWithServer(profile: UserProfile): Result<Int> = try {
        val uid = requireUserId()
        val flushed = withTimeoutOrNull(7_000L) {
            db.waitForPendingWrites().await()
            true
        } ?: false
        val serverSnap = withTimeoutOrNull(7_000L) {
            buildAttendanceQuery(profile, uid).get(Source.SERVER).await()
        }
        if (flushed && serverSnap != null) {
            Result.success(serverSnap.size())
        } else {
            val cacheSnap = buildAttendanceQuery(profile, uid).get(Source.CACHE).await()
            Result.failure(
                IllegalStateException("Offline queue active (${cacheSnap.size()} cached records). Will sync when online.")
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.LIST, "attendance/sync")
        Result.failure(e)
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
        val cleanName = studentName.trim().take(100)
        val cleanGrade = gradeLevel.trim().take(40)
        val cleanDate = dateLabel.trim().take(40)
        val cleanNote = note.trim().take(300)
        val cleanParent = parentId.trim().take(128)
        require(cleanName.length >= 2) { "Student name must be at least 2 characters" }
        require(cleanGrade.isNotEmpty()) { "Grade level is required" }
        require(cleanDate.length >= 4) { "Date is required" }

        val resolvedStudentId = studentId.trim().take(128).ifBlank {
            "manual_${cleanName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "student" }}"
        }
        val docId = buildDailyAttendanceDocId(cleanDate, resolvedStudentId, uid)
        val ref = db.collection("attendance").document(docId)
        val existingSnap = ref.get().await()

        if (existingSnap.exists()) {
            val updates = buildMap<String, Any> {
                put("studentName", cleanName)
                put("gradeLevel", cleanGrade)
                put("dateLabel", cleanDate)
                put("status", status.name)
                put("note", cleanNote)
                if (cleanParent.isNotEmpty()) put("parentId", cleanParent)
                put("updatedAt", serverTime())
            }
            ref.update(updates).await()
        } else {
            val payload = buildMap<String, Any> {
                put("studentId", resolvedStudentId)
                put("studentName", cleanName)
                if (cleanParent.isNotEmpty()) put("parentId", cleanParent)
                put("teacherId", uid)
                put("adminId", adminId.ifBlank { uid }.take(128))
                put("gradeLevel", cleanGrade)
                put("dateLabel", cleanDate)
                put("status", status.name)
                if (cleanNote.isNotEmpty()) put("note", cleanNote)
                put("createdAt", serverTime())
                put("updatedAt", serverTime())
            }
            ref.set(payload).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "attendance")
        Result.failure(e)
    }

    suspend fun markBatchAttendance(
        entries: List<DailyAttendanceEntry>,
        dateLabel: String,
        adminId: String
    ): Result<Int> = try {
        val uid = requireUserId()
        val cleanDate = dateLabel.trim().take(40)
        require(cleanDate.length >= 4) { "Date is required" }
        if (entries.isEmpty()) return Result.success(0)

        var processed = 0
        entries.chunked(200).forEach { chunk ->
            val refsAndEntries = chunk.map { entry ->
                val resolvedStudentId = entry.studentId.trim().take(128).ifBlank {
                    "manual_${entry.studentName.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "student" }}"
                }
                val docId = buildDailyAttendanceDocId(cleanDate, resolvedStudentId, uid)
                Triple(db.collection("attendance").document(docId), resolvedStudentId, entry)
            }
            val batch = db.batch()
            refsAndEntries.forEach { (ref, resolvedStudentId, entry) ->
                val cleanName = entry.studentName.trim().take(100)
                val cleanGrade = entry.gradeLevel.trim().take(40).ifBlank { "Hunda" }
                val cleanNote = entry.note.trim().take(300)
                val cleanParent = entry.parentId.trim().take(128)
                if (cleanName.length >= 2) {
                    val exists = ref.get().await().exists()
                    if (exists) {
                        val updates = buildMap<String, Any> {
                            put("studentName", cleanName)
                            put("gradeLevel", cleanGrade)
                            put("dateLabel", cleanDate)
                            put("status", entry.status.name)
                            put("note", cleanNote)
                            if (cleanParent.isNotEmpty()) put("parentId", cleanParent)
                            put("updatedAt", serverTime())
                        }
                        batch.update(ref, updates)
                    } else {
                        val payload = buildMap<String, Any> {
                            put("studentId", resolvedStudentId)
                            put("studentName", cleanName)
                            if (cleanParent.isNotEmpty()) put("parentId", cleanParent)
                            put("teacherId", uid)
                            put("adminId", adminId.ifBlank { uid }.take(128))
                            put("gradeLevel", cleanGrade)
                            put("dateLabel", cleanDate)
                            put("status", entry.status.name)
                            if (cleanNote.isNotEmpty()) put("note", cleanNote)
                            put("createdAt", serverTime())
                            put("updatedAt", serverTime())
                        }
                        batch.set(ref, payload)
                    }
                    processed++
                }
            }
            batch.commit().await()
        }
        Result.success(processed)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "attendance")
        Result.failure(e)
    }

    suspend fun deleteAttendanceRecord(attendanceId: String): Result<Unit> = try {
        requireUserId()
        require(attendanceId.isNotBlank()) { "Attendance ID is required" }
        db.collection("attendance").document(attendanceId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "attendance/$attendanceId")
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

    suspend fun deleteAiChat(chatId: String): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("ai_chats").document(chatId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "users/$chatId")
        Result.failure(e)
    }

    suspend fun clearAllAiChats(): Result<Unit> = try {
        val uid = requireUserId()
        val snap = db.collection("users").document(uid).collection("ai_chats").get().await()
        val docs = snap.documents
        docs.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "users/ai_chats")
        Result.failure(e)
    }

    fun observeCalendarEvents(): Flow<List<SchoolCalendarEvent>> {
        val uid = auth.currentUser?.uid ?: return emptyFlow()
        return db.collection("school_events")
            .whereEqualTo("schoolId", SCHOOL_ID_MAIN)
            .snapshots()
            .map { snap -> snap.toObjects(SchoolCalendarEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.LIST, "school_events", uid)
    }

    suspend fun upsertCalendarEvent(
        eventId: String = "",
        title: String,
        description: String,
        eventDateLabel: String,
        startTimeLabel: String,
        endTimeLabel: String,
        category: CalendarEventCategory,
        gradeLevel: String,
        subject: String,
        creatorName: String,
        adminId: String,
        sourceType: String = "MANUAL",
        sourceId: String = ""
    ): Result<Unit> = try {
        val uid = requireUserId()
        val cleanTitle = title.trim().take(150)
        val cleanDate = eventDateLabel.trim().take(40)
        val cleanGrade = gradeLevel.trim().take(40).ifBlank { "Hunda" }
        val cleanStart = startTimeLabel.trim().take(16).ifBlank { "08:30" }
        val cleanEnd = endTimeLabel.trim().take(16).ifBlank { "09:30" }
        require(cleanTitle.length >= 2) { "Event title must be at least 2 characters" }
        require(cleanDate.length >= 4) { "Event date is required" }

        val ref = if (eventId.isNotBlank()) {
            db.collection("school_events").document(eventId)
        } else {
            db.collection("school_events").document()
        }
        val exists = eventId.isNotBlank() && ref.get().await().exists()
        if (exists) {
            val updates = buildMap<String, Any> {
                put("title", cleanTitle)
                put("description", description.trim().take(1500))
                put("eventDateLabel", cleanDate)
                put("startTimeLabel", cleanStart)
                put("endTimeLabel", cleanEnd)
                put("category", category.name)
                put("gradeLevel", cleanGrade)
                put("subject", subject.trim().take(60))
                put("updatedAt", serverTime())
            }
            ref.update(updates).await()
        } else {
            val payload = buildMap<String, Any> {
                put("title", cleanTitle)
                description.trim().take(1500).takeIf { it.isNotEmpty() }?.let { put("description", it) }
                put("eventDateLabel", cleanDate)
                put("startTimeLabel", cleanStart)
                put("endTimeLabel", cleanEnd)
                put("category", category.name)
                put("gradeLevel", cleanGrade)
                subject.trim().take(60).takeIf { it.isNotEmpty() }?.let { put("subject", it) }
                put("sourceType", sourceType)
                sourceId.trim().take(128).takeIf { it.isNotEmpty() }?.let { put("sourceId", it) }
                put("createdByUid", uid)
                put("createdByName", creatorName.trim().take(100).ifBlank { "Koofkeet Staff" })
                put("adminId", adminId.ifBlank { uid }.take(128))
                put("schoolId", SCHOOL_ID_MAIN)
                put("createdAt", serverTime())
                put("updatedAt", serverTime())
            }
            ref.set(payload).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "school_events")
        Result.failure(e)
    }

    suspend fun deleteCalendarEvent(eventId: String): Result<Unit> = try {
        requireUserId()
        require(eventId.isNotBlank()) { "Event ID is required" }
        db.collection("school_events").document(eventId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "school_events/$eventId")
        Result.failure(e)
    }

    suspend fun syncAcademicsToCalendar(
        assignments: List<Assignment>,
        quizzes: List<Quiz>,
        creatorName: String,
        adminId: String,
        fallbackDate: String
    ): Result<Int> = try {
        val uid = requireUserId()
        var synced = 0
        val targets = buildList {
            assignments.forEach { hw ->
                val docId = "assign_${hw.id.replace(Regex("[^a-zA-Z0-9_.-]+"), "_").take(100)}"
                add(
                    Triple(
                        docId,
                        CalendarEventCategory.ASSIGNMENT,
                        mapOf(
                            "title" to "${hw.subject}: ${hw.title}".take(150),
                            "description" to hw.description.take(1500),
                            "eventDateLabel" to hw.dueDateLabel.ifBlank { fallbackDate }.take(40),
                            "startTimeLabel" to "08:30",
                            "endTimeLabel" to "09:30",
                            "category" to CalendarEventCategory.ASSIGNMENT.name,
                            "gradeLevel" to hw.gradeLevel.ifBlank { "Hunda" }.take(40),
                            "subject" to hw.subject.take(60),
                            "sourceType" to "ASSIGNMENT",
                            "sourceId" to hw.id.take(128)
                        )
                    )
                )
            }
            quizzes.forEach { q ->
                val docId = "quiz_${q.id.replace(Regex("[^a-zA-Z0-9_.-]+"), "_").take(100)}"
                add(
                    Triple(
                        docId,
                        CalendarEventCategory.EXAM,
                        mapOf(
                            "title" to "Quiz: ${q.title}".take(150),
                            "description" to "${q.subject} • ${q.durationMinutes} min",
                            "eventDateLabel" to fallbackDate.take(40),
                            "startTimeLabel" to "10:00",
                            "endTimeLabel" to "11:00",
                            "category" to CalendarEventCategory.EXAM.name,
                            "gradeLevel" to q.gradeLevel.ifBlank { "Hunda" }.take(40),
                            "subject" to q.subject.take(60),
                            "sourceType" to "QUIZ",
                            "sourceId" to q.id.take(128)
                        )
                    )
                )
            }
        }
        if (targets.isEmpty()) return Result.success(0)

        targets.chunked(200).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (docId, _, fields) ->
                val ref = db.collection("school_events").document(docId)
                val existing = ref.get().await()
                if (existing.exists()) {
                    val updates = fields.toMutableMap<String, Any>().apply {
                        put("updatedAt", serverTime())
                    }
                    batch.update(ref, updates)
                } else {
                    val payload = fields.toMutableMap<String, Any>().apply {
                        put("createdByUid", uid)
                        put("createdByName", creatorName.trim().take(100).ifBlank { "Koofkeet Staff" })
                        put("adminId", adminId.ifBlank { uid }.take(128))
                        put("schoolId", SCHOOL_ID_MAIN)
                        put("createdAt", serverTime())
                        put("updatedAt", serverTime())
                    }
                    batch.set(ref, payload)
                }
                synced++
            }
            batch.commit().await()
        }
        Result.success(synced)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "school_events/sync")
        Result.failure(e)
    }
}

data class DailyAttendanceEntry(
    val studentId: String,
    val studentName: String,
    val parentId: String = "",
    val gradeLevel: String,
    val status: AttendanceStatus,
    val note: String = ""
)

fun buildDailyAttendanceDocId(
    dateLabel: String,
    studentId: String,
    teacherUid: String
): String {
    val sanitize: (String, Int) -> String = { raw, maxLen ->
        raw.trim()
            .replace(Regex("[^a-zA-Z0-9_.-]+"), "_")
            .trim('_')
            .take(maxLen)
            .ifBlank { "x" }
    }
    val datePart = sanitize(dateLabel, 24)
    val studentPart = sanitize(studentId, 56)
    val teacherPart = sanitize(teacherUid, 28)
    return "${datePart}_${studentPart}_${teacherPart}".take(120)
}

