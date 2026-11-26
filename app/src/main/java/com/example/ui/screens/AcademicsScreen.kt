package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Assignment
import com.example.data.model.AssignmentSubmission
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceSyncStatus
import com.example.data.model.CalendarEventCategory
import com.example.data.model.ExamResult
import com.example.data.model.LearningMaterial
import com.example.data.model.MaterialType
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.SchoolCalendarEvent
import com.example.data.model.StudentRegistration
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.components.EmptyStateBanner
import com.example.ui.components.PrivacyIsolationNotice
import com.example.ui.components.SectionHeader

enum class AcademicSection(val titleOromo: String) {
    HOMEWORK("Hojii Manaa"),
    QUIZZES("Qormaataa & Quiz"),
    EXAMS("Bu'aa Qormaataa"),
    MARKLIST("Marklistii (Marklist)"),
    ATTENDANCE("To'annoo Argamaa"),
    CALENDAR("Kalandarii (Calendar)"),
    MATERIALS("Meeshaalee")
}

@Composable
fun AcademicsScreen(
    profile: UserProfile,
    students: List<StudentRegistration>,
    assignments: List<Assignment>,
    submissions: List<AssignmentSubmission>,
    quizzes: List<Quiz>,
    quizAttempts: List<QuizAttempt>,
    examResults: List<ExamResult>,
    attendance: List<AttendanceRecord>,
    materials: List<LearningMaterial>,
    onCreateAssignment: (String, String, String, String, String) -> Unit,
    onSubmitAssignment: (Assignment, String) -> Unit,
    onGradeSubmission: (String, Double, String) -> Unit,
    onCreateQuiz: (String, String, String, Int, List<QuizQuestion>) -> Unit,
    onSubmitQuizAttempt: (Quiz, Map<Int, Int>) -> Unit,
    onRecordExamResult: (String, String, String, String, String, String, String, Double, String) -> Unit,
    onRecordAttendance: (String, String, String, String, String, AttendanceStatus, String) -> Unit,
    onAddMaterial: (String, String, String, String, MaterialType, String, String) -> Unit,
    modifier: Modifier = Modifier,
    requestedSection: AcademicSection? = null,
    onConsumeRequestedSection: () -> Unit = {},
    onMarkAllPresent: (String, List<StudentRegistration>, List<AttendanceRecord>) -> Unit = { _, _, _ -> },
    onDeleteAttendance: (String) -> Unit = {},
    onDeleteExamResult: (String) -> Unit = {},
    attendanceSyncStatus: AttendanceSyncStatus = AttendanceSyncStatus(),
    onSyncAttendance: () -> Unit = {},
    calendarEvents: List<SchoolCalendarEvent> = emptyList(),
    onCreateCalendarEvent: (String, String, String, String, String, CalendarEventCategory, String, String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeleteCalendarEvent: (String) -> Unit = {},
    onSyncAcademicsToCalendar: () -> Unit = {},
    onShowStatus: (String) -> Unit = {}
) {
    var selectedSection by remember { mutableStateOf(requestedSection ?: AcademicSection.HOMEWORK) }
    LaunchedEffect(requestedSection) {
        if (requestedSection != null) {
            selectedSection = requestedSection
            onConsumeRequestedSection()
        }
    }
    val canManage = profile.userRole == UserRole.TEACHER || profile.userRole == UserRole.ADMIN

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AcademicSection.entries.forEach { sec ->
                    FilterChip(
                        selected = selectedSection == sec,
                        onClick = { selectedSection = sec },
                        label = { Text(sec.titleOromo) },
                        modifier = Modifier.testTag("academic_tab_${sec.name.lowercase()}")
                    )
                }
            }

            when (selectedSection) {
                AcademicSection.HOMEWORK -> AssignmentsTab(
                    profile = profile,
                    canManage = canManage,
                    assignments = assignments,
                    submissions = submissions,
                    onCreateAssignment = onCreateAssignment,
                    onSubmitAssignment = onSubmitAssignment,
                    onGradeSubmission = onGradeSubmission
                )
                AcademicSection.QUIZZES -> QuizzesTab(
                    profile = profile,
                    canManage = canManage,
                    quizzes = quizzes,
                    attempts = quizAttempts,
                    onCreateQuiz = onCreateQuiz,
                    onSubmitQuizAttempt = onSubmitQuizAttempt
                )
                AcademicSection.EXAMS -> ExamResultsTab(
                    profile = profile,
                    canManage = canManage,
                    students = students,
                    examResults = examResults,
                    onRecordExamResult = onRecordExamResult,
                    onDeleteExamResult = onDeleteExamResult
                )
                AcademicSection.MARKLIST -> MarklistTab(
                    profile = profile,
                    canManage = canManage,
                    students = students,
                    examResults = examResults,
                    submissions = submissions,
                    quizAttempts = quizAttempts,
                    onRecordGrade = onRecordExamResult,
                    onDeleteGrade = onDeleteExamResult
                )
                AcademicSection.ATTENDANCE -> AttendanceTab(
                    profile = profile,
                    canManage = canManage,
                    students = students,
                    attendance = attendance,
                    onRecordAttendance = onRecordAttendance,
                    onMarkAllPresent = onMarkAllPresent,
                    onDeleteAttendance = onDeleteAttendance,
                    syncStatus = attendanceSyncStatus,
                    onSyncAttendance = onSyncAttendance
                )
                AcademicSection.CALENDAR -> CalendarSyncTab(
                    profile = profile,
                    canManage = canManage,
                    events = calendarEvents,
                    assignments = assignments,
                    onCreateEvent = onCreateCalendarEvent,
                    onDeleteEvent = onDeleteCalendarEvent,
                    onSyncAcademics = onSyncAcademicsToCalendar,
                    onShowStatus = onShowStatus
                )
                AcademicSection.MATERIALS -> MaterialsTab(
                    canManage = canManage,
                    materials = materials,
                    onAddMaterial = onAddMaterial
                )
            }
        }
    }
}

@Composable
private fun AssignmentsTab(
    profile: UserProfile,
    canManage: Boolean,
    assignments: List<Assignment>,
    submissions: List<AssignmentSubmission>,
    onCreateAssignment: (String, String, String, String, String) -> Unit,
    onSubmitAssignment: (Assignment, String) -> Unit,
    onGradeSubmission: (String, Double, String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var submittingTarget by remember { mutableStateOf<Assignment?>(null) }
    var gradingTarget by remember { mutableStateOf<AssignmentSubmission?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Hojii Manaa (Assignments)",
                    subtitle = if (canManage) "Hojii manaa kennaa fi galmee hordofaa" else "Hojii manaa ilaalaa fi deebii galchaa",
                    modifier = Modifier.weight(1f)
                )
                if (canManage) {
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("create_assignment_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kenni")
                    }
                }
            }
        }

        if (assignments.isEmpty()) {
            item {
                EmptyStateBanner(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    title = "Hojii manaa hin jiru",
                    message = "Yeroo barsiisaan hojii manaa kennu asitti mul'ata."
                )
            }
        } else {
            items(assignments, key = { it.id }) { hw ->
                val mySub = submissions.firstOrNull { it.assignmentId == hw.id && it.studentId == profile.userId }
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${hw.subject} • ${hw.gradeLevel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "Guyyaa: ${hw.dueDateLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(hw.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(hw.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Barsiisaa: ${hw.teacherName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

                        if (profile.userRole == UserRole.STUDENT || profile.userRole == UserRole.ADMIN) {
                            if (mySub != null) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = if (mySub.status == "GRADED") {
                                            "Galmaa'eera • Qabxii: ${"%.1f".format(mySub.score)}% (${mySub.feedback})"
                                        } else {
                                            "Ergameera (Submitted): ${mySub.answerText}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { submittingTarget = hw },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submit_hw_${hw.id}")
                                ) {
                                    Text("Deebii Galchi (Submit Homework)")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (submissions.isNotEmpty()) {
            item {
                SectionHeader(
                    title = if (canManage) "Deebii Barattootaa (Submissions)" else "Hojii Manaa Galmeeffame",
                    subtitle = "Haala galmee hojii manaa"
                )
            }
            items(submissions, key = { it.id }) { sub ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${sub.studentName} • ${sub.assignmentTitle}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (sub.status == "GRADED") "${"%.1f".format(sub.score)}%" else "SUBMITTED",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(sub.answerText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (sub.feedback.isNotBlank()) {
                            Text("Yaada Barsiisaa: ${sub.feedback}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        if (canManage && sub.status != "GRADED") {
                            OutlinedButton(onClick = { gradingTarget = sub }) {
                                Text("Qabxii Kenni (Grade)")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCurriculumAssignmentDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, desc, subject, grade, due ->
                onCreateAssignment(title, desc, subject, grade, due)
                showCreateDialog = false
            }
        )
    }

    submittingTarget?.let { hw ->
        var answer by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { submittingTarget = null },
            title = { Text("Deebii: ${hw.title}") },
            text = {
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Deebii keessan barreessaa") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitAssignment(hw, answer)
                        submittingTarget = null
                    },
                    enabled = answer.isNotBlank()
                ) { Text("Ergi") }
            },
            dismissButton = { TextButton(onClick = { submittingTarget = null }) { Text("Dhiisi") } }
        )
    }

    gradingTarget?.let { sub ->
        var scoreStr by remember { mutableStateOf("") }
        var feedback by remember { mutableStateOf("") }
        val parsedScore = scoreStr.toDoubleOrNull()
        AlertDialog(
            onDismissRequest = { gradingTarget = null },
            title = { Text("Qabxii Kenni: ${sub.studentName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = scoreStr, onValueChange = { scoreStr = it }, label = { Text("Qabxii (0 - 100)") }, singleLine = true)
                    OutlinedTextField(value = feedback, onValueChange = { feedback = it }, label = { Text("Yaada Barsiisaa") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onGradeSubmission(sub.id, parsedScore ?: 0.0, feedback)
                        gradingTarget = null
                    },
                    enabled = parsedScore != null && parsedScore in 0.0..100.0
                ) { Text("Galmeessi") }
            },
            dismissButton = { TextButton(onClick = { gradingTarget = null }) { Text("Dhiisi") } }
        )
    }
}

@Composable
private fun QuizzesTab(
    profile: UserProfile,
    canManage: Boolean,
    quizzes: List<Quiz>,
    attempts: List<QuizAttempt>,
    onCreateQuiz: (String, String, String, Int, List<QuizQuestion>) -> Unit,
    onSubmitQuizAttempt: (Quiz, Map<Int, Int>) -> Unit
) {
    var showCreateQuiz by remember { mutableStateOf(false) }
    var activeQuiz by remember { mutableStateOf<Quiz?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Qormaataa fi Quiz",
                    subtitle = "Qormaata online qopheessuu fi fudhachuu",
                    modifier = Modifier.weight(1f)
                )
                if (canManage) {
                    Button(
                        onClick = { showCreateQuiz = true },
                        modifier = Modifier.testTag("create_quiz_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Qopheessi")
                    }
                }
            }
        }

        if (quizzes.isEmpty()) {
            item {
                EmptyStateBanner(
                    icon = Icons.Filled.Quiz,
                    title = "Quiz qophaa'e hin jiru",
                    message = "Yeroo barsiisaan Quiz qopheessu barattoonni asirraa fudhachuu danda'u."
                )
            }
        } else {
            items(quizzes, key = { it.id }) { quiz ->
                val qs = quiz.parsedQuestions()
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(quiz.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${quiz.subject} • ${quiz.gradeLevel} • Gaaffilee ${qs.size} (${quiz.durationMinutes} min)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { activeQuiz = quiz },
                            enabled = qs.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("take_quiz_${quiz.id}")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Qormaata Jalqabi (Take Quiz)")
                        }
                    }
                }
            }
        }

        if (attempts.isNotEmpty()) {
            item {
                PrivacyIsolationNotice("Qabxiin Quiz dhuunfaan qofa mul'ata.")
            }
            items(attempts, key = { it.id }) { att ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${att.quizTitle} (${att.studentName})", style = MaterialTheme.typography.titleSmall)
                            Text("Deebii sirrii: ${att.correctCount}/${att.totalQuestions}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = "${"%.0f".format(att.score)}%",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showCreateQuiz) {
        var title by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("") }
        var grade by remember { mutableStateOf("") }
        var qPrompt by remember { mutableStateOf("") }
        var optA by remember { mutableStateOf("") }
        var optB by remember { mutableStateOf("") }
        var optC by remember { mutableStateOf("") }
        var correctIdx by remember { mutableStateOf(0) }

        AlertDialog(
            onDismissRequest = { showCreateQuiz = false },
            title = { Text("Quiz Haaraa Qopheessi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Mata-duree Quiz") }, singleLine = true)
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Gosa Barumsaa") }, singleLine = true)
                    OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("Kutaa") }, singleLine = true)
                    OutlinedTextField(value = qPrompt, onValueChange = { qPrompt = it }, label = { Text("Gaaffii 1ffaa") })
                    OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Filannoo A") }, singleLine = true)
                    OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Filannoo B") }, singleLine = true)
                    OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Filannoo C") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("A" to 0, "B" to 1, "C" to 2).forEach { (lbl, idx) ->
                            FilterChip(selected = correctIdx == idx, onClick = { correctIdx = idx }, label = { Text("Sirrii: $lbl") })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val opts = listOf(optA.trim(), optB.trim(), optC.trim()).filter { it.isNotEmpty() }
                        val q = QuizQuestion(qPrompt.trim(), opts, correctIdx.coerceIn(0, (opts.size - 1).coerceAtLeast(0)))
                        onCreateQuiz(title, subject, grade, 15, listOf(q))
                        showCreateQuiz = false
                    },
                    enabled = title.trim().length >= 2 &&
                        subject.trim().length >= 2 &&
                        grade.trim().isNotEmpty() &&
                        qPrompt.trim().length >= 2 &&
                        optA.isNotBlank() &&
                        optB.isNotBlank()
                ) { Text("Qopheessi") }
            },
            dismissButton = { TextButton(onClick = { showCreateQuiz = false }) { Text("Dhiisi") } }
        )
    }

    activeQuiz?.let { quiz ->
        val questions = remember(quiz.id) { quiz.parsedQuestions() }
        val selections = remember(quiz.id) { mutableStateMapOf<Int, Int>() }
        AlertDialog(
            onDismissRequest = { activeQuiz = null },
            title = { Text(quiz.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    questions.forEachIndexed { qIdx, q ->
                        Text("${qIdx + 1}. ${q.prompt}", style = MaterialTheme.typography.titleSmall)
                        q.options.forEachIndexed { oIdx, opt ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selections[qIdx] == oIdx,
                                    onClick = { selections[qIdx] = oIdx }
                                )
                                Text(opt, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitQuizAttempt(quiz, selections.toMap())
                        activeQuiz = null
                    },
                    enabled = selections.size == questions.size
                ) { Text("Xumuri & Ergi") }
            },
            dismissButton = { TextButton(onClick = { activeQuiz = null }) { Text("Dhiisi") } }
        )
    }
}
