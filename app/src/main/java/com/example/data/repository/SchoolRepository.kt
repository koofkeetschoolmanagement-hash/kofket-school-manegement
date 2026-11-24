package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.Assignment
import com.example.data.model.AssignmentSubmission
import com.example.data.model.BOOTSTRAP_ADMIN_EMAIL
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.RoleGrant
import com.example.data.model.SCHOOL_ID_MAIN
import com.example.data.model.SchoolMeta
import com.example.data.model.StudentRegistration
import com.example.data.model.TeacherAssignment
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SchoolRepository(
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

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snap -> snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.GET, path, userId)
    }

    suspend fun fetchSchoolMeta(): SchoolMeta? = try {
        db.collection("school_meta").document("initialized")
            .get().await()
            .toObject(SchoolMeta::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, "school_meta/initialized")
        null
    }

    suspend fun fetchRoleGrantForCurrentUser(): RoleGrant? {
        val user = auth.currentUser ?: return null
        val email = user.email?.trim()?.lowercase() ?: return null
        if (!user.isEmailVerified) return null
        return try {
            db.collection("role_grants").document(email)
                .get().await()
                .toObject(RoleGrant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun onboardCurrentUser(
        displayName: String,
        gradeLevel: String,
        claimSchoolAdminIfUninitialized: Boolean
    ): Result<UserProfile> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Signed-in user required"))
        val uid = user.uid
        val email = user.email?.trim()?.lowercase().orEmpty()
        return try {
            val meta = fetchSchoolMeta()
            val grant = fetchRoleGrantForCurrentUser()
            val isBootstrap = user.isEmailVerified && email == BOOTSTRAP_ADMIN_EMAIL

            val resolvedRole: UserRole
            val resolvedAdminId: String
            val resolvedSubject: String
            val resolvedGrade: String
            val resolvedLinkedStudentId: String
            val resolvedLinkedStudentName: String

            when {
                isBootstrap || (meta == null && claimSchoolAdminIfUninitialized) -> {
                    resolvedRole = UserRole.ADMIN
                    resolvedAdminId = uid
                    resolvedSubject = "Bulchiinsa"
                    resolvedGrade = "Hunda"
                    resolvedLinkedStudentId = ""
                    resolvedLinkedStudentName = ""
                }
                grant != null -> {
                    resolvedRole = UserRole.fromRaw(grant.role)
                    resolvedAdminId = grant.adminId.ifBlank { meta?.adminUid ?: uid }
                    resolvedSubject = grant.subject
                    resolvedGrade = grant.gradeLevel.ifBlank { gradeLevel }
                    resolvedLinkedStudentId = grant.linkedStudentId
                    resolvedLinkedStudentName = grant.linkedStudentName
                }
                else -> {
                    resolvedRole = UserRole.STUDENT
                    resolvedAdminId = meta?.adminUid ?: uid
                    resolvedSubject = ""
                    resolvedGrade = gradeLevel.ifBlank { "Kutaa 10" }
                    resolvedLinkedStudentId = ""
                    resolvedLinkedStudentName = ""
                }
            }

            val batch = db.batch()
            if (resolvedRole == UserRole.ADMIN) {
                val adminDoc = mapOf(
                    "userId" to uid,
                    "email" to email.ifBlank { "admin@koofkeet.org" },
                    "createdAt" to serverTime()
                )
                batch.set(db.collection("admins").document(uid), adminDoc)
                if (meta == null) {
                    val metaDoc = mapOf(
                        "schoolId" to SCHOOL_ID_MAIN,
                        "adminUid" to uid,
                        "adminEmail" to email.ifBlank { "admin@koofkeet.org" },
                        "schoolName" to "Mana Barumsaa Koofkeet",
                        "createdAt" to serverTime(),
                        "updatedAt" to serverTime()
                    )
                    batch.set(db.collection("school_meta").document("initialized"), metaDoc)
                }
            }

            val userPayload = mapOf(
                "userId" to uid,
                "displayName" to displayName.trim().ifBlank { user.displayName ?: "Fayyadamaa Koofkeet" },
                "email" to email.ifBlank { "user@koofkeet.org" },
                "role" to resolvedRole.name,
                "gradeLevel" to resolvedGrade,
                "subject" to resolvedSubject,
                "linkedStudentId" to resolvedLinkedStudentId,
                "linkedStudentName" to resolvedLinkedStudentName,
                "adminId" to resolvedAdminId,
                "createdAt" to serverTime(),
                "updatedAt" to serverTime()
            ).filterValues { it != "" }

            batch.set(db.collection("users").document(uid), userPayload)

            if (grant != null && email.isNotBlank()) {
                batch.update(
                    db.collection("role_grants").document(email),
                    mapOf("claimedByUid" to uid, "updatedAt" to serverTime())
                )
            }

            batch.commit().await()
            val saved = db.collection("users").document(uid).get().await()
                .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(userId = uid, displayName = displayName, email = email, role = resolvedRole.name, adminId = resolvedAdminId)
            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    fun observeTeacherAssignments(): Flow<List<TeacherAssignment>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.emptyFlow()
        return db.collection("teacher_assignments")
            .whereEqualTo("schoolId", SCHOOL_ID_MAIN)
            .snapshots()
            .map { snap -> snap.toObjects(TeacherAssignment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.LIST, "teacher_assignments", uid)
    }

    suspend fun addTeacherAssignment(
        teacherId: String,
        teacherName: String,
        subject: String,
        gradeLevel: String,
        schedule: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        val ref = db.collection("teacher_assignments").document()
        ref.set(
            mapOf(
                "teacherId" to teacherId.ifBlank { uid },
                "teacherName" to teacherName.trim(),
                "subject" to subject.trim(),
                "gradeLevel" to gradeLevel.trim(),
                "schedule" to schedule.trim(),
                "adminId" to uid,
                "schoolId" to SCHOOL_ID_MAIN,
                "createdAt" to serverTime(),
                "updatedAt" to serverTime()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "teacher_assignments")
        Result.failure(e)
    }

    suspend fun createRoleGrant(
        email: String,
        role: UserRole,
        fullName: String,
        gradeLevel: String,
        subject: String,
        linkedStudentId: String,
        linkedStudentName: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        val normalized = email.trim().lowercase()
        require(normalized.contains("@")) { "Imeelii sirrii galchaa" }
        val payload = buildMap<String, Any> {
            put("email", normalized)
            put("role", role.name)
            put("fullName", fullName.trim())
            gradeLevel.trim().takeIf { it.isNotEmpty() }?.let { put("gradeLevel", it) }
            subject.trim().takeIf { it.isNotEmpty() }?.let { put("subject", it) }
            linkedStudentId.trim().takeIf { it.isNotEmpty() }?.let { put("linkedStudentId", it) }
            linkedStudentName.trim().takeIf { it.isNotEmpty() }?.let { put("linkedStudentName", it) }
            put("adminId", uid)
            put("createdAt", serverTime())
            put("updatedAt", serverTime())
        }
        db.collection("role_grants").document(normalized).set(payload).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "role_grants")
        Result.failure(e)
    }

    fun observeStudentRegistrations(profile: UserProfile): Flow<List<StudentRegistration>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.emptyFlow()
        val query = when (profile.userRole) {
            UserRole.ADMIN -> if (profile.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)) {
                db.collection("student_registrations").whereEqualTo("status", "ACTIVE")
            } else {
                db.collection("student_registrations").whereEqualTo("adminId", uid)
            }
            UserRole.TEACHER -> db.collection("student_registrations").whereArrayContains("teacherUids", uid)
            UserRole.PARENT -> db.collection("student_registrations").whereEqualTo("parentUid", uid)
            UserRole.STUDENT -> db.collection("student_registrations").whereEqualTo("studentUid", uid)
        }
        return query.snapshots()
            .map { snap -> snap.toObjects(StudentRegistration::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.LIST, "student_registrations", uid)
    }

    suspend fun registerStudent(
        studentUid: String,
        studentName: String,
        studentCode: String,
        gradeLevel: String,
        section: String,
        parentUid: String,
        parentName: String,
        teacherUids: List<String>
    ): Result<Unit> = try {
        val uid = requireUserId()
        val ref = db.collection("student_registrations").document()
        val payload = buildMap<String, Any> {
            put("studentUid", studentUid.ifBlank { ref.id })
            put("studentName", studentName.trim())
            put("studentCode", studentCode.trim())
            put("gradeLevel", gradeLevel.trim())
            put("section", section.trim())
            parentUid.trim().takeIf { it.isNotEmpty() }?.let { put("parentUid", it) }
            parentName.trim().takeIf { it.isNotEmpty() }?.let { put("parentName", it) }
            put("adminId", uid)
            put("teacherUids", teacherUids.ifEmpty { listOf(uid) }.take(20))
            put("status", "ACTIVE")
            put("createdAt", serverTime())
            put("updatedAt", serverTime())
        }
        ref.set(payload).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "student_registrations")
        Result.failure(e)
    }

    fun observeAssignments(): Flow<List<Assignment>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.emptyFlow()
        return db.collection("assignments")
            .whereEqualTo("schoolId", SCHOOL_ID_MAIN)
            .snapshots()
            .map { snap -> snap.toObjects(Assignment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.LIST, "assignments", uid)
    }

    suspend fun createAssignment(
        title: String,
        description: String,
        subject: String,
        gradeLevel: String,
        dueDateLabel: String,
        teacherName: String,
        adminId: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        val ref = db.collection("assignments").document()
        ref.set(
            mapOf(
                "title" to title.trim(),
                "description" to description.trim(),
                "subject" to subject.trim(),
                "gradeLevel" to gradeLevel.trim(),
                "dueDateLabel" to dueDateLabel.trim(),
                "teacherId" to uid,
                "teacherName" to teacherName.trim(),
                "adminId" to adminId.ifBlank { uid },
                "schoolId" to SCHOOL_ID_MAIN,
                "status" to "OPEN",
                "createdAt" to serverTime(),
                "updatedAt" to serverTime()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "assignments")
        Result.failure(e)
    }

    fun observeSubmissions(profile: UserProfile): Flow<List<AssignmentSubmission>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.emptyFlow()
        val query = when (profile.userRole) {
            UserRole.ADMIN -> if (profile.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)) {
                db.collection("assignment_submissions")
            } else {
                db.collection("assignment_submissions").whereEqualTo("adminId", uid)
            }
            UserRole.TEACHER -> db.collection("assignment_submissions").whereEqualTo("teacherId", uid)
            UserRole.PARENT -> db.collection("assignment_submissions").whereEqualTo("parentId", uid)
            UserRole.STUDENT -> db.collection("assignment_submissions").whereEqualTo("studentId", uid)
        }
        return query.snapshots()
            .map { snap -> snap.toObjects(AssignmentSubmission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.LIST, "assignment_submissions", uid)
    }

    suspend fun submitAssignment(
        assignment: Assignment,
        studentName: String,
        parentId: String,
        answerText: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        val ref = db.collection("assignment_submissions").document()
        val payload = buildMap<String, Any> {
            put("assignmentId", assignment.id)
            put("assignmentTitle", assignment.title)
            put("subject", assignment.subject)
            put("studentId", uid)
            put("studentName", studentName.trim())
            parentId.takeIf { it.isNotBlank() }?.let { put("parentId", it) }
            put("teacherId", assignment.teacherId)
            put("adminId", assignment.adminId)
            put("answerText", answerText.trim())
            put("score", 0.0)
            put("status", "SUBMITTED")
            put("createdAt", serverTime())
            put("updatedAt", serverTime())
        }
        ref.set(payload).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "assignment_submissions")
        Result.failure(e)
    }

    suspend fun gradeSubmission(submissionId: String, score: Double, feedback: String): Result<Unit> = try {
        requireUserId()
        db.collection("assignment_submissions").document(submissionId)
            .update(
                mapOf(
                    "score" to score.coerceIn(0.0, 100.0),
                    "feedback" to feedback.trim(),
                    "status" to "GRADED",
                    "updatedAt" to serverTime()
                )
            ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "assignment_submissions/$submissionId")
        Result.failure(e)
    }

    fun observeQuizzes(): Flow<List<Quiz>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.emptyFlow()
        return db.collection("quizzes")
            .whereEqualTo("schoolId", SCHOOL_ID_MAIN)
            .snapshots()
            .map { snap -> snap.toObjects(Quiz::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.LIST, "quizzes", uid)
    }

    suspend fun createQuiz(
        title: String,
        subject: String,
        gradeLevel: String,
        durationMinutes: Int,
        questions: List<QuizQuestion>,
        teacherName: String,
        adminId: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        val ref = db.collection("quizzes").document()
        val qMaps = questions.take(25).map { q ->
            mapOf(
                "prompt" to q.prompt,
                "options" to q.options,
                "correctIndex" to q.correctIndex
            )
        }
        ref.set(
            mapOf(
                "title" to title.trim(),
                "subject" to subject.trim(),
                "gradeLevel" to gradeLevel.trim(),
                "durationMinutes" to durationMinutes.coerceIn(1, 180),
                "questions" to qMaps,
                "teacherId" to uid,
                "teacherName" to teacherName.trim(),
                "adminId" to adminId.ifBlank { uid },
                "schoolId" to SCHOOL_ID_MAIN,
                "published" to true,
                "createdAt" to serverTime(),
                "updatedAt" to serverTime()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "quizzes")
        Result.failure(e)
    }

    fun observeQuizAttempts(profile: UserProfile): Flow<List<QuizAttempt>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.emptyFlow()
        val query = when (profile.userRole) {
            UserRole.ADMIN -> if (profile.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)) {
                db.collection("quiz_attempts")
            } else {
                db.collection("quiz_attempts").whereEqualTo("adminId", uid)
            }
            UserRole.TEACHER -> db.collection("quiz_attempts").whereEqualTo("teacherId", uid)
            UserRole.PARENT -> db.collection("quiz_attempts").whereEqualTo("parentId", uid)
            UserRole.STUDENT -> db.collection("quiz_attempts").whereEqualTo("studentId", uid)
        }
        return query.snapshots()
            .map { snap -> snap.toObjects(QuizAttempt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .handleStreamErrors(OperationType.LIST, "quiz_attempts", uid)
    }

    suspend fun submitQuizAttempt(
        quiz: Quiz,
        studentName: String,
        parentId: String,
        correctCount: Int,
        totalQuestions: Int
    ): Result<Unit> = try {
        val uid = requireUserId()
        val score = if (totalQuestions > 0) (correctCount.toDouble() / totalQuestions) * 100.0 else 0.0
        val ref = db.collection("quiz_attempts").document()
        val payload = buildMap<String, Any> {
            put("quizId", quiz.id)
            put("quizTitle", quiz.title)
            put("subject", quiz.subject)
            put("studentId", uid)
            put("studentName", studentName.trim())
            parentId.takeIf { it.isNotBlank() }?.let { put("parentId", it) }
            put("teacherId", quiz.teacherId)
            put("adminId", quiz.adminId)
            put("score", score)
            put("totalQuestions", totalQuestions)
            put("correctCount", correctCount)
            put("createdAt", serverTime())
            put("updatedAt", serverTime())
        }
        ref.set(payload).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "quiz_attempts")
        Result.failure(e)
    }
}
