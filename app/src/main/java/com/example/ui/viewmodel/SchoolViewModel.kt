package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppLanguage
import com.example.core.AttendanceDateHelper
import com.example.core.MonthlyReportData
import com.example.core.ReportStudentRow
import com.example.core.RoleGuardState
import com.example.core.RoleNavigationGuard
import com.example.data.model.AiChatMessage
import com.example.data.model.Announcement
import com.example.data.model.AnnouncementCategory
import com.example.data.model.Assignment
import com.example.data.model.AssignmentSubmission
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceSyncStatus
import com.example.data.model.CalendarEventCategory
import com.example.data.model.EarlyWarning
import com.example.data.model.ExamResult
import com.example.data.model.LearningMaterial
import com.example.data.model.MaterialType
import com.example.data.model.ParentMessage
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.RoleGrant
import com.example.data.model.SchoolCalendarEvent
import com.example.data.model.SchoolMeta
import com.example.data.model.StudentRegistration
import com.example.data.model.SyncPhase
import com.example.data.model.TeacherAssignment
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.AiStreamEvent
import com.example.data.repository.AiTutorRepository
import com.example.data.repository.CommunicationAndRecordsRepository
import com.example.data.repository.DailyAttendanceEntry
import com.example.data.repository.SchoolRepository
import com.example.data.repository.StudySupportMode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiTutorUiState {
    data object Idle : AiTutorUiState
    data object Loading : AiTutorUiState
    data class Streaming(val text: String) : AiTutorUiState
    data class Success(val text: String) : AiTutorUiState
    data class Error(val message: String) : AiTutorUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class SchoolViewModel(
    private val schoolRepo: SchoolRepository,
    private val commsRepo: CommunicationAndRecordsRepository,
    private val aiRepo: AiTutorRepository,
    private val currentUserId: String
) : ViewModel() {

    private companion object {
        const val TAG = "SchoolVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private fun <T> Flow<T>.toUiStateFlow(defaultTag: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                val root = fsError ?: error
                Log.w(TAG, "Stream error in $defaultTag", root)
                emit(UiState.Error(root.localizedMessage ?: "Dogoggorri odeeffannoo uumameera"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _schoolMeta = MutableStateFlow<SchoolMeta?>(null)
    val schoolMeta: StateFlow<SchoolMeta?> = _schoolMeta.asStateFlow()

    private val _pendingGrant = MutableStateFlow<RoleGrant?>(null)
    val pendingGrant: StateFlow<RoleGrant?> = _pendingGrant.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        schoolRepo.observeUserProfile(currentUserId).toUiStateFlow("userProfile")

    val roleGuardState: StateFlow<RoleGuardState> = combine(
        userProfileState,
        _pendingGrant,
        _schoolMeta
    ) { profileState, grant, meta ->
        when (profileState) {
            is UiState.Loading -> RoleGuardState.CheckingFirestoreRole
            is UiState.Error -> RoleGuardState.Error(profileState.message)
            is UiState.Success -> {
                val profile = profileState.data
                if (profile == null) {
                    val suggested = when {
                        grant != null -> UserRole.fromRaw(grant.role)
                        meta == null -> UserRole.ADMIN
                        else -> UserRole.STUDENT
                    }
                    RoleGuardState.NeedsOnboarding(suggested)
                } else {
                    RoleGuardState.Authorized(
                        profile = profile,
                        targetDashboard = RoleNavigationGuard.homeRouteFor(profile.userRole)
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = RoleGuardState.CheckingFirestoreRole
    )

    private val activeProfileFlow: Flow<UserProfile?> = userProfileState.map { state ->
        (state as? UiState.Success)?.data
    }

    val teacherAssignmentsState: StateFlow<UiState<List<TeacherAssignment>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList()) else schoolRepo.observeTeacherAssignments()
        }.toUiStateFlow("teacherAssignments")

    val usersState: StateFlow<UiState<List<UserProfile>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null || profile.userRole != UserRole.ADMIN) flowOf(emptyList())
            else schoolRepo.observeUsers(profile)
        }.toUiStateFlow("users")

    val roleGrantsState: StateFlow<UiState<List<RoleGrant>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null || profile.userRole != UserRole.ADMIN) flowOf(emptyList())
            else schoolRepo.observeRoleGrants(profile)
        }.toUiStateFlow("roleGrants")

    val studentRegistrationsState: StateFlow<UiState<List<StudentRegistration>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList()) else schoolRepo.observeStudentRegistrations(profile)
        }.toUiStateFlow("studentRegistrations")

    val studentUsersState: StateFlow<UiState<List<UserProfile>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null || (profile.userRole != UserRole.TEACHER && profile.userRole != UserRole.ADMIN)) {
                flowOf(emptyList())
            } else {
                schoolRepo.observeStudentUsers(profile)
            }
        }.toUiStateFlow("studentUsers")

    val studentRosterState: StateFlow<List<StudentRegistration>> =
        combine(studentRegistrationsState, studentUsersState) { regState, usersState ->
            val regs = (regState as? UiState.Success)?.data.orEmpty()
            val studentProfiles = (usersState as? UiState.Success)?.data.orEmpty()
            val registeredUids = regs.map { it.studentUid }.filter { it.isNotBlank() }.toSet()
            val extraFromUsers = studentProfiles
                .filter { it.userId.isNotBlank() && it.userId !in registeredUids }
                .map { u ->
                    StudentRegistration(
                        id = u.userId,
                        studentUid = u.userId,
                        studentName = u.displayName,
                        studentCode = "",
                        gradeLevel = u.gradeLevel.ifBlank { "Hunda" },
                        section = "A",
                        parentUid = "",
                        parentName = "",
                        adminId = u.adminId,
                        status = "ACTIVE"
                    )
                }
            (regs + extraFromUsers).sortedBy { it.studentName.lowercase() }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val assignmentsState: StateFlow<UiState<List<Assignment>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList()) else schoolRepo.observeAssignments()
        }.toUiStateFlow("assignments")

    val submissionsState: StateFlow<UiState<List<AssignmentSubmission>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList()) else schoolRepo.observeSubmissions(profile)
        }.toUiStateFlow("submissions")

    val quizzesState: StateFlow<UiState<List<Quiz>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList()) else schoolRepo.observeQuizzes()
        }.toUiStateFlow("quizzes")

    val quizAttemptsState: StateFlow<UiState<List<QuizAttempt>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList()) else schoolRepo.observeQuizAttempts(profile)
        }.toUiStateFlow("quizAttempts")

    val examResultsState: StateFlow<UiState<List<ExamResult>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList()) else commsRepo.observeExamResults(profile)
        }.toUiStateFlow("examResults")

    val attendanceState: StateFlow<UiState<List<AttendanceRecord>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList()) else commsRepo.observeAttendance(profile)
        }.toUiStateFlow("attendance")

    val materialsState: StateFlow<UiState<List<LearningMaterial>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList()) else commsRepo.observeLearningMaterials()
        }.toUiStateFlow("materials")

    val earlyWarningsState: StateFlow<UiState<List<EarlyWarning>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList()) else commsRepo.observeEarlyWarnings(profile)
        }.toUiStateFlow("earlyWarnings")

    val parentMessagesState: StateFlow<UiState<List<ParentMessage>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList()) else commsRepo.observeParentMessages(profile)
        }.toUiStateFlow("parentMessages")

    val announcementsState: StateFlow<UiState<List<Announcement>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList()) else commsRepo.observeAnnouncements()
        }.toUiStateFlow("announcements")

    val aiChatsState: StateFlow<UiState<List<AiChatMessage>>> =
        commsRepo.observeAiChats(currentUserId).toUiStateFlow("aiChats")

    val calendarEventsState: StateFlow<UiState<List<SchoolCalendarEvent>>> =
        activeProfileFlow.flatMapLatest { profile ->
            if (profile == null) flowOf(emptyList()) else commsRepo.observeCalendarEvents()
        }.toUiStateFlow("schoolEvents")

    private val _manualSyncPhase = MutableStateFlow<SyncPhase?>(null)
    private val _lastManualSyncMillis = MutableStateFlow(0L)

    val attendanceSyncStatus: StateFlow<AttendanceSyncStatus> =
        combine(
            activeProfileFlow.flatMapLatest { profile ->
                if (profile == null) flowOf(AttendanceSyncStatus()) else commsRepo.observeAttendanceSyncStatus(profile)
            },
            _manualSyncPhase,
            _lastManualSyncMillis
        ) { metadataStatus, manualPhase, manualMillis ->
            val effectivePhase = manualPhase ?: metadataStatus.phase
            val effectiveLastSynced = maxOf(metadataStatus.lastSyncedMillis, manualMillis)
            metadataStatus.copy(
                phase = effectivePhase,
                lastSyncedMillis = effectiveLastSynced
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), AttendanceSyncStatus())

    private val _aiTutorState = MutableStateFlow<AiTutorUiState>(AiTutorUiState.Idle)
    val aiTutorState: StateFlow<AiTutorUiState> = _aiTutorState.asStateFlow()

    init {
        refreshOnboardingContext()
    }

    fun refreshOnboardingContext() {
        viewModelScope.launch {
            _schoolMeta.value = schoolRepo.fetchSchoolMeta()
            _pendingGrant.value = schoolRepo.fetchRoleGrantForCurrentUser()
        }
    }

    private fun runMutation(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = block()
            _isBusy.value = false
            res.fold(
                onSuccess = { _events.emit(successMessage) },
                onFailure = { err -> _events.emit(err.localizedMessage ?: "Rakkoon uumameera") }
            )
        }
    }

    fun completeOnboarding(
        displayName: String,
        gradeLevel: String,
        preferredLanguage: AppLanguage,
        claimAdmin: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = schoolRepo.onboardCurrentUser(displayName, gradeLevel, preferredLanguage, claimAdmin)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    refreshOnboardingContext()
                    val msg = when (preferredLanguage) {
                        AppLanguage.ENGLISH -> "Profile saved to Firestore! Welcome to Koofkeet School."
                        AppLanguage.AMHARIC -> "መገለጫዎ ወደ Firestore ተቀምጧል! እንኳን ደህና መጡ።"
                        AppLanguage.AFAAN_OROMOO -> "Profile keessan Firestore irratti olkaa'ameera! Baga nagaan dhuftan."
                    }
                    _events.emit(msg)
                },
                onFailure = { err ->
                    _events.emit(err.localizedMessage ?: "Galmeen hin milkoofne")
                }
            )
        }
    }

    fun updatePreferredLanguage(language: AppLanguage) {
        viewModelScope.launch {
            schoolRepo.updatePreferredLanguage(language)
        }
    }

    fun addTeacherAssignment(
        teacherId: String,
        teacherName: String,
        subject: String,
        gradeLevel: String,
        schedule: String
    ) = runMutation("Ramaddiin barsiisaa galmaa'eera!") {
        schoolRepo.addTeacherAssignment(teacherId, teacherName, subject, gradeLevel, schedule)
    }

    fun issueRoleGrant(
        email: String,
        role: UserRole,
        fullName: String,
        gradeLevel: String,
        subject: String,
        linkedStudentId: String,
        linkedStudentName: String
    ) = runMutation("Mirgi fayyadamaa ($role) imeelii kanaaf kennameera!") {
        schoolRepo.createRoleGrant(email, role, fullName, gradeLevel, subject, linkedStudentId, linkedStudentName)
    }

    fun updateUserRole(
        targetUser: UserProfile,
        newRole: UserRole,
        subject: String,
        gradeLevel: String
    ) = runMutation("Gaheen ${targetUser.displayName} gara ${newRole.labelOromo}tti jijjiirameera!") {
        schoolRepo.updateUserRole(targetUser, newRole, subject, gradeLevel)
    }

    fun revokeRoleGrant(email: String) =
        runMutation("Mirgi fayyadamaa ($email) haqameera!") {
            schoolRepo.revokeRoleGrant(email)
        }

    fun registerStudent(
        studentUid: String,
        studentName: String,
        studentCode: String,
        gradeLevel: String,
        section: String,
        parentUid: String,
        parentName: String
    ) = runMutation("Galmeen barataa milkaa'eera!") {
        schoolRepo.registerStudent(
            studentUid = studentUid,
            studentName = studentName,
            studentCode = studentCode,
            gradeLevel = gradeLevel,
            section = section,
            parentUid = parentUid,
            parentName = parentName,
            teacherUids = listOf(currentUserId)
        )
    }

    fun createAssignment(
        title: String,
        description: String,
        subject: String,
        gradeLevel: String,
        dueDateLabel: String,
        profile: UserProfile
    ) = runMutation("Hojii manaa haaraan maxxanfameera!") {
        schoolRepo.createAssignment(
            title = title,
            description = description,
            subject = subject,
            gradeLevel = gradeLevel,
            dueDateLabel = dueDateLabel,
            teacherName = profile.displayName,
            adminId = profile.adminId
        )
    }

    fun submitAssignment(assignment: Assignment, profile: UserProfile, answerText: String) {
        val linkedParentId = (studentRegistrationsState.value as? UiState.Success)?.data
            ?.firstOrNull { it.studentUid == profile.userId }?.parentUid.orEmpty()
        runMutation("Deebiin hojii manaa ergameera!") {
            schoolRepo.submitAssignment(assignment, profile.displayName, linkedParentId, answerText)
        }
    }

    fun gradeSubmission(submissionId: String, score: Double, feedback: String) =
        runMutation("Qabxiin hojii manaa galmaa'eera!") {
            schoolRepo.gradeSubmission(submissionId, score, feedback)
        }

    fun createQuiz(
        title: String,
        subject: String,
        gradeLevel: String,
        durationMinutes: Int,
        questions: List<QuizQuestion>,
        profile: UserProfile
    ) = runMutation("Qormaanni / Quiz qophaa'eera!") {
        schoolRepo.createQuiz(
            title = title,
            subject = subject,
            gradeLevel = gradeLevel,
            durationMinutes = durationMinutes,
            questions = questions,
            teacherName = profile.displayName,
            adminId = profile.adminId
        )
    }

    fun submitQuizAttempt(quiz: Quiz, profile: UserProfile, selectedIndices: Map<Int, Int>) {
        val qs = quiz.parsedQuestions()
        val correct = qs.indices.count { idx -> selectedIndices[idx] == qs[idx].correctIndex }
        val linkedParentId = (studentRegistrationsState.value as? UiState.Success)?.data
            ?.firstOrNull { it.studentUid == profile.userId }?.parentUid.orEmpty()
        runMutation("Qormaanni xumurameera! Deebii sirrii: $correct/${qs.size}") {
            schoolRepo.submitQuizAttempt(quiz, profile.displayName, linkedParentId, correct, qs.size)
        }
    }

    fun recordExamResult(
        studentId: String,
        studentName: String,
        parentId: String,
        subject: String,
        gradeLevel: String,
        examName: String,
        monthLabel: String,
        score: Double,
        remarks: String,
        profile: UserProfile,
        successMessage: String = "Bu'aan qormaataa galmaa'eera!"
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val currentList = (examResultsState.value as? UiState.Success)?.data.orEmpty()
                .filter { it.studentId == studentId || (studentId.isBlank() && it.studentName == studentName) }
            val res = commsRepo.recordExamResultWithEarlyWarning(
                studentId = studentId,
                studentName = studentName,
                parentId = parentId,
                teacherName = profile.displayName,
                adminId = profile.adminId,
                subject = subject,
                gradeLevel = gradeLevel,
                examName = examName,
                monthLabel = monthLabel,
                score = score,
                remarks = remarks,
                existingResultsForStudent = currentList
            )
            _isBusy.value = false
            res.fold(
                onSuccess = { triggeredWarning ->
                    if (triggeredWarning) {
                        _events.emit("$successMessage (Early Warning triggered)")
                    } else {
                        _events.emit(successMessage)
                    }
                },
                onFailure = { err ->
                    _events.emit(err.localizedMessage ?: "Qabxii galchuun hin milkoofne")
                }
            )
        }
    }

    fun updateExamResult(
        resultId: String,
        score: Double,
        remarks: String,
        successMessage: String
    ) = runMutation(successMessage) {
        commsRepo.updateExamResult(resultId, score, remarks)
    }

    fun deleteExamResult(
        resultId: String,
        successMessage: String
    ) = runMutation(successMessage) {
        commsRepo.deleteExamResult(resultId)
    }

    fun recordAttendance(
        studentId: String,
        studentName: String,
        parentId: String,
        gradeLevel: String,
        dateLabel: String,
        status: AttendanceStatus,
        note: String,
        profile: UserProfile,
        successMessage: String = "Argamaan (Attendance) galmaa'eera!"
    ) = runMutation(successMessage) {
        commsRepo.recordAttendance(
            studentId = studentId,
            studentName = studentName,
            parentId = parentId,
            adminId = profile.adminId,
            gradeLevel = gradeLevel,
            dateLabel = dateLabel,
            status = status,
            note = note
        )
    }

    fun markAllPresentForDate(
        dateLabel: String,
        students: List<StudentRegistration>,
        existingForDate: List<AttendanceRecord>,
        profile: UserProfile,
        successPrefix: String
    ) {
        val markedIds = existingForDate.map { it.studentId }.toSet()
        val pending = students.filter { it.studentUid.isNotBlank() && it.studentUid !in markedIds }
        val targets = if (pending.isNotEmpty()) pending else students
        if (targets.isEmpty()) return
        val entries = targets.map { s ->
            DailyAttendanceEntry(
                studentId = s.studentUid,
                studentName = s.studentName,
                parentId = s.parentUid,
                gradeLevel = s.gradeLevel.ifBlank { "Hunda" },
                status = AttendanceStatus.PRESENT
            )
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = commsRepo.markBatchAttendance(entries, dateLabel, profile.adminId)
            _isBusy.value = false
            res.fold(
                onSuccess = { count -> _events.emit("$successPrefix$count") },
                onFailure = { err -> _events.emit(err.localizedMessage ?: "Rakkoon uumameera") }
            )
        }
    }

    fun deleteAttendanceRecord(
        attendanceId: String,
        successMessage: String
    ) = runMutation(successMessage) {
        commsRepo.deleteAttendanceRecord(attendanceId)
    }

    fun syncAttendance(
        profile: UserProfile,
        syncedMessage: String,
        offlineMessage: String
    ) {
        viewModelScope.launch {
            _manualSyncPhase.value = SyncPhase.SYNCING
            val res = commsRepo.syncAttendanceWithServer(profile)
            res.fold(
                onSuccess = { count ->
                    _manualSyncPhase.value = null
                    _lastManualSyncMillis.value = System.currentTimeMillis()
                    _events.emit("$syncedMessage ($count)")
                },
                onFailure = {
                    _manualSyncPhase.value = null
                    _events.emit(offlineMessage)
                }
            )
        }
    }

    fun upsertCalendarEvent(
        title: String,
        description: String,
        eventDateLabel: String,
        startTimeLabel: String,
        endTimeLabel: String,
        category: CalendarEventCategory,
        gradeLevel: String,
        subject: String,
        profile: UserProfile,
        successMessage: String
    ) = runMutation(successMessage) {
        commsRepo.upsertCalendarEvent(
            title = title,
            description = description,
            eventDateLabel = eventDateLabel,
            startTimeLabel = startTimeLabel,
            endTimeLabel = endTimeLabel,
            category = category,
            gradeLevel = gradeLevel,
            subject = subject,
            creatorName = profile.displayName,
            adminId = profile.adminId
        )
    }

    fun deleteCalendarEvent(
        eventId: String,
        successMessage: String
    ) = runMutation(successMessage) {
        commsRepo.deleteCalendarEvent(eventId)
    }

    fun syncAcademicsToCalendar(
        profile: UserProfile,
        successPrefix: String
    ) {
        val assignments = (assignmentsState.value as? UiState.Success)?.data.orEmpty()
        val quizzes = (quizzesState.value as? UiState.Success)?.data.orEmpty()
        viewModelScope.launch {
            _isBusy.value = true
            val res = commsRepo.syncAcademicsToCalendar(
                assignments = assignments,
                quizzes = quizzes,
                creatorName = profile.displayName,
                adminId = profile.adminId,
                fallbackDate = AttendanceDateHelper.todayDateLabel()
            )
            _isBusy.value = false
            res.fold(
                onSuccess = { count -> _events.emit("$successPrefix$count") },
                onFailure = { err -> _events.emit(err.localizedMessage ?: "Sync hin milkoofne") }
            )
        }
    }

    fun addLearningMaterial(
        title: String,
        description: String,
        subject: String,
        gradeLevel: String,
        materialType: MaterialType,
        resourceUrl: String,
        contentBody: String,
        profile: UserProfile
    ) = runMutation("Meeshaan barnootaa fe'ameera!") {
        commsRepo.addLearningMaterial(
            title = title,
            description = description,
            subject = subject,
            gradeLevel = gradeLevel,
            materialType = materialType,
            resourceUrl = resourceUrl,
            contentBody = contentBody,
            teacherName = profile.displayName,
            adminId = profile.adminId
        )
    }

    fun resolveEarlyWarning(warningId: String) =
        runMutation("Deeggarsi dursa-beeksisaa furameera!") {
            commsRepo.resolveEarlyWarning(warningId)
        }

    fun sendParentMessage(
        profile: UserProfile,
        parentId: String,
        parentName: String,
        studentId: String,
        studentName: String,
        recipientRole: String,
        recipientId: String,
        subject: String,
        body: String
    ) = runMutation("Ergaan keessan nageenyaan ergameera!") {
        commsRepo.sendParentMessage(
            parentId = parentId,
            parentName = parentName,
            studentId = studentId,
            studentName = studentName,
            recipientRole = recipientRole,
            recipientId = recipientId,
            senderName = profile.displayName,
            senderRole = profile.userRole,
            subject = subject,
            body = body
        )
    }

    fun postAnnouncement(
        title: String,
        body: String,
        category: AnnouncementCategory,
        profile: UserProfile
    ) = runMutation("Beeksisni haaraan maxxanfameera!") {
        commsRepo.postAnnouncement(
            title = title,
            body = body,
            category = category,
            authorName = profile.displayName,
            adminId = profile.adminId
        )
    }

    fun askAiTutor(
        prompt: String,
        subject: String,
        studyMode: StudySupportMode = StudySupportMode.EXPLAIN,
        attachedBitmap: Bitmap? = null,
        language: AppLanguage = AppLanguage.AFAAN_OROMOO,
        gradeLevel: String = ""
    ) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) return
        val history = (aiChatsState.value as? UiState.Success)?.data.orEmpty()
        viewModelScope.launch {
            _aiTutorState.value = AiTutorUiState.Loading
            aiRepo.streamTutorAnswer(
                prompt = cleanPrompt,
                subject = subject,
                attachedImage = attachedBitmap,
                studyMode = studyMode,
                language = language,
                gradeLevel = gradeLevel,
                recentHistory = history
            ).collect { event ->
                when (event) {
                    is AiStreamEvent.Chunk -> {
                        _aiTutorState.value = AiTutorUiState.Streaming(event.accumulatedText)
                    }
                    is AiStreamEvent.Completed -> {
                        _aiTutorState.value = AiTutorUiState.Success(event.finalText)
                        commsRepo.saveAiChat(cleanPrompt, event.finalText, subject)
                    }
                    is AiStreamEvent.Error -> {
                        _aiTutorState.value = AiTutorUiState.Error(event.message)
                    }
                }
            }
        }
    }

    fun deleteAiChat(chatId: String) {
        viewModelScope.launch {
            commsRepo.deleteAiChat(chatId)
        }
    }

    fun clearAllPrivateAiChats() {
        viewModelScope.launch {
            _aiTutorState.value = AiTutorUiState.Idle
            commsRepo.clearAllAiChats()
        }
    }

    fun buildMonthlyReport(monthLabel: String): MonthlyReportData {
        val registeredStudents = (studentRegistrationsState.value as? UiState.Success)?.data.orEmpty()
        val teachers = (teacherAssignmentsState.value as? UiState.Success)?.data.orEmpty()
        val assignments = (assignmentsState.value as? UiState.Success)?.data.orEmpty()
        val exams = (examResultsState.value as? UiState.Success)?.data.orEmpty()
            .filter { monthLabel.equals("Hunda", ignoreCase = true) || it.monthLabel.contains(monthLabel, ignoreCase = true) }
        val attendance = (attendanceState.value as? UiState.Success)?.data.orEmpty()
            .filter { monthLabel.equals("Hunda", ignoreCase = true) || it.dateLabel.startsWith(monthLabel, ignoreCase = true) }
        val warnings = (earlyWarningsState.value as? UiState.Success)?.data.orEmpty()

        val studentDirectory = linkedMapOf<String, Pair<String, String>>() // key -> (name, grade)
        registeredStudents.forEach { s ->
            val key = s.studentUid.ifBlank { s.studentName }
            if (key.isNotBlank() && s.studentName.isNotBlank()) {
                studentDirectory[key] = s.studentName to s.gradeLevel.ifBlank { "Grade 1" }
            }
        }
        exams.forEach { ex ->
            val key = ex.studentId.ifBlank { ex.studentName }
            if (key.isNotBlank()) {
                studentDirectory.putIfAbsent(key, ex.studentName to ex.gradeLevel.ifBlank { "Grade 1" })
            }
        }
        attendance.forEach { rec ->
            val key = rec.studentId.ifBlank { rec.studentName }
            if (key.isNotBlank()) {
                studentDirectory.putIfAbsent(key, rec.studentName to rec.gradeLevel.ifBlank { "Grade 1" })
            }
        }

        val avgScore = if (exams.isEmpty()) 0.0 else exams.map { it.score }.average()
        val presentCount = attendance.count { it.status == AttendanceStatus.PRESENT.name }
        val attendanceRate = if (attendance.isEmpty()) 100.0 else (presentCount.toDouble() / attendance.size) * 100.0

        val rows = studentDirectory.entries.map { (key, info) ->
            val (sName, sGrade) = info
            val sExams = exams.filter { it.studentId == key || it.studentName == sName }
            val sAvg = if (sExams.isEmpty()) 0.0 else sExams.map { it.score }.average()
            val sAtt = attendance.filter { it.studentId == key || it.studentName == sName }
            val sRate = if (sAtt.isEmpty()) 100.0 else (sAtt.count { it.status == AttendanceStatus.PRESENT.name }.toDouble() / sAtt.size) * 100.0
            ReportStudentRow(
                studentName = sName,
                gradeLevel = sGrade,
                averageScore = sAvg,
                attendancePercent = sRate
            )
        }.sortedBy { it.studentName.lowercase() }

        val warningLines = warnings.map { w ->
            "${w.studentName} (${w.subject}): ${"%.1f".format(w.currentScore)}% [${w.status}] - ${w.reason}"
        }

        return MonthlyReportData(
            monthLabel = monthLabel,
            totalStudents = studentDirectory.size,
            totalTeachers = teachers.map { it.teacherName }.distinct().size,
            totalAssignments = assignments.size,
            totalExamsRecorded = exams.size,
            averageExamScore = avgScore,
            attendanceRatePercent = attendanceRate,
            openEarlyWarnings = warnings.count { it.status == "OPEN" },
            resolvedEarlyWarnings = warnings.count { it.status == "RESOLVED" },
            studentRows = rows,
            warningRows = warningLines
        )
    }
}
