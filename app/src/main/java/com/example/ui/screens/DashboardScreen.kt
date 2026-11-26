package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AttendanceDateHelper
import com.example.core.LocalAppLanguage
import com.example.core.MonthlyReportData
import com.example.core.stringsFor
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
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.SchoolCalendarEvent
import com.example.data.model.StudentRegistration
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.StudySupportMode
import com.example.ui.components.EmptyStateBanner
import com.example.ui.components.KoofkeetFooterQuote
import com.example.ui.components.MetricStatCard
import com.example.ui.components.RoleBadgePill
import com.example.ui.components.SectionHeader
import com.example.ui.viewmodel.AiTutorUiState

@Composable
fun DashboardScreen(
    profile: UserProfile,
    assignments: List<Assignment>,
    submissions: List<AssignmentSubmission>,
    quizzes: List<Quiz>,
    examResults: List<ExamResult>,
    attendance: List<AttendanceRecord>,
    materials: List<LearningMaterial>,
    earlyWarnings: List<EarlyWarning>,
    announcements: List<Announcement>,
    onNavigateToTab: (String) -> Unit,
    modifier: Modifier = Modifier,
    aiState: AiTutorUiState = AiTutorUiState.Idle,
    aiChats: List<AiChatMessage> = emptyList(),
    onAskAiTutor: (prompt: String, subject: String, mode: StudySupportMode) -> Unit = { _, _, _ -> },
    students: List<StudentRegistration> = emptyList(),
    onMarkAttendance: (String, String, String, String, String, AttendanceStatus, String) -> Unit = { _, _, _, _, _, _, _ -> },
    onMarkAllPresent: (String, List<StudentRegistration>, List<AttendanceRecord>) -> Unit = { _, _, _ -> },
    onOpenAttendance: () -> Unit = { onNavigateToTab("academics") },
    quizAttempts: List<QuizAttempt> = emptyList(),
    onRecordGrade: (String, String, String, String, String, String, String, Double, String) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onDeleteGrade: (String) -> Unit = {},
    onCreateAssignment: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    onOpenAcademicSection: (AcademicSection) -> Unit = { onNavigateToTab("academics") },
    attendanceSyncStatus: AttendanceSyncStatus = AttendanceSyncStatus(),
    onSyncAttendance: () -> Unit = {},
    availableReportMonths: List<String> = listOf("Hunda"),
    buildMonthlyReport: (String) -> MonthlyReportData = { month ->
        MonthlyReportData(
            monthLabel = month,
            totalStudents = students.size,
            totalTeachers = 0,
            totalAssignments = assignments.size,
            totalExamsRecorded = examResults.size,
            averageExamScore = if (examResults.isEmpty()) 0.0 else examResults.map { it.score }.average(),
            attendanceRatePercent = if (attendance.isEmpty()) 100.0
            else (attendance.count { it.status == AttendanceStatus.PRESENT.name }.toDouble() / attendance.size) * 100.0,
            openEarlyWarnings = earlyWarnings.count { it.status == "OPEN" },
            resolvedEarlyWarnings = earlyWarnings.count { it.status == "RESOLVED" },
            studentRows = emptyList(),
            warningRows = emptyList()
        )
    },
    calendarEvents: List<SchoolCalendarEvent> = emptyList(),
    onCreateCalendarEvent: (String, String, String, String, String, CalendarEventCategory, String, String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onSyncAcademicsToCalendar: () -> Unit = {},
    onShowStatus: (String) -> Unit = {}
) {
    val openWarnings = earlyWarnings.filter { it.status == "OPEN" }
    val avgScore = if (examResults.isEmpty()) 0.0 else examResults.map { it.score }.average()
    val presentPct = if (attendance.isEmpty()) 100.0
    else (attendance.count { it.status == AttendanceStatus.PRESENT.name }.toDouble() / attendance.size) * 100.0

    val strings = stringsFor(LocalAppLanguage.current)
    val roleDashboardTitle = when (profile.userRole) {
        UserRole.ADMIN -> strings.adminDashboardTitle
        UserRole.TEACHER -> strings.teacherDashboardTitle
        UserRole.STUDENT -> strings.studentDashboardTitle
        UserRole.PARENT -> strings.parentDashboardTitle
    }
    val roleDashboardTag = "dashboard_role_${profile.userRole.name.lowercase()}"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(MaterialTheme.shapes.large)
                        .testTag(roleDashboardTag)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_school_hero),
                        contentDescription = "Koofkeet School Campus",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RoleBadgePill(role = profile.userRole)
                                if (profile.gradeLevel.isNotBlank()) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = Color.White.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = profile.gradeLevel,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Column {
                                Text(
                                    text = "Nagaa, ${profile.displayName}!",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.school_motto),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = Color(0xFFFFE59E)
                                )
                            }
                        }
                    }
                }
            }

            if (profile.userRole == UserRole.STUDENT) {
                item {
                    StudentPrivateAiTutorCard(
                        aiState = aiState,
                        recentChats = aiChats,
                        onAskAiTutor = onAskAiTutor,
                        onOpenFullTutor = { onNavigateToTab("ai_tutor") }
                    )
                }
            }

            if (profile.userRole == UserRole.TEACHER || profile.userRole == UserRole.ADMIN) {
                item {
                    TeacherGradesListCard(
                        profile = profile,
                        students = students,
                        examResults = examResults,
                        onRecordGrade = onRecordGrade,
                        onDeleteGrade = onDeleteGrade,
                        onOpenMarklist = { onOpenAcademicSection(AcademicSection.MARKLIST) }
                    )
                }
                item {
                    TeacherDailyAttendanceCard(
                        students = students,
                        attendance = attendance,
                        syncStatus = attendanceSyncStatus,
                        onSyncNow = onSyncAttendance,
                        onMarkAttendance = onMarkAttendance,
                        onMarkAllPresent = onMarkAllPresent,
                        onOpenFullAttendance = onOpenAttendance
                    )
                }
            }

            item {
                DashboardAssignmentsCard(
                    profile = profile,
                    assignments = assignments,
                    onCreateAssignment = onCreateAssignment,
                    onOpenAssignments = { onOpenAcademicSection(AcademicSection.HOMEWORK) }
                )
            }

            item {
                DashboardMarklistCard(
                    students = students,
                    examResults = examResults,
                    submissions = submissions,
                    quizAttempts = quizAttempts,
                    onOpenFullMarklist = { onOpenAcademicSection(AcademicSection.MARKLIST) }
                )
            }

            item {
                DashboardCalendarSyncCard(
                    profile = profile,
                    events = calendarEvents,
                    assignments = assignments,
                    onCreateEvent = onCreateCalendarEvent,
                    onSyncAcademics = onSyncAcademicsToCalendar,
                    onOpenFullCalendar = { onOpenAcademicSection(AcademicSection.CALENDAR) },
                    onShowStatus = onShowStatus
                )
            }

            item {
                ExportReportCard(
                    availableMonths = availableReportMonths,
                    buildMonthlyReport = buildMonthlyReport,
                    examResults = examResults,
                    attendance = attendance,
                    onShowStatus = onShowStatus
                )
            }

            if ((profile.userRole == UserRole.TEACHER || profile.userRole == UserRole.ADMIN) && openWarnings.isNotEmpty()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Early Warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Deeggarsa Dursa-Beeksisaa (${openWarnings.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${openWarnings.first().studentName}: ${openWarnings.first().reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            TextButton(
                                onClick = { onNavigateToTab("comms") },
                                modifier = Modifier.testTag("view_early_warnings_button")
                            ) {
                                Text("Ilaali")
                            }
                        }
                    }
                }
            }

            if (profile.userRole == UserRole.PARENT) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Dashboard Maatii: ${profile.linkedStudentName.ifBlank { "Ilma / Intala Keessan" }}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Hojii manaa galmaa'e: ${submissions.size} • Bu'aa qormaataa: ${examResults.size} • Giddu-galeessa: ${if (examResults.isEmpty()) "—" else "${"%.1f".format(avgScore)}%"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            if (examResults.isNotEmpty()) {
                                val latest = examResults.first()
                                Text(
                                    text = "Qormaata dhihoo: ${latest.examName} (${latest.subject}) — ${"%.1f".format(latest.score)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            TextButton(
                                onClick = { onNavigateToTab("academics") },
                                modifier = Modifier.testTag("parent_view_academics_button")
                            ) {
                                Text("Hojii Manaa & Bu'aa Qormaataa Ilaali")
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = roleDashboardTitle,
                    subtitle = when (profile.userRole) {
                        UserRole.ADMIN -> "Bulchiinsa fayyadamtootaa, ramaddii barsiisotaa, galmee fi gabaasa ji'aa"
                        UserRole.TEACHER -> "Hojii manaa, qormaata qopheessuu, qabxii fi to'annoo argamaa"
                        UserRole.STUDENT -> "Hojii manaa, qormaata, bu'aa qabxii fi gargaarsa AI Tutor"
                        UserRole.PARENT -> "Hordoffii barnoota ilma/intala keessanii fi qunnamtii barsiisaa"
                    }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricStatCard(
                            title = "Hojii Manaa",
                            value = "${assignments.size}",
                            subtitle = "Assignments",
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "Qormaataa & Quiz",
                            value = "${quizzes.size}",
                            subtitle = "Quizzes ready",
                            icon = Icons.Filled.Quiz,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricStatCard(
                            title = "Giddu-galeessa Qabxii",
                            value = if (examResults.isEmpty()) "—" else "${"%.1f".format(avgScore)}%",
                            subtitle = "${examResults.size} galmaa'eera",
                            icon = Icons.Filled.Grade,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "Argamaa (Attendance)",
                            value = "${"%.0f".format(presentPct)}%",
                            subtitle = "${materials.size} Kitaaba/PDF",
                            icon = Icons.Filled.CheckCircle,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Tajaajiloota Saffisaa (Quick Actions)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { onNavigateToTab("academics") },
                                label = { Text("Hojii Manaa & Quiz") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("quick_academics_chip")
                            )
                            AssistChip(
                                onClick = { onNavigateToTab("ai_tutor") },
                                label = { Text("AI Tutor Gaafadhu") },
                                leadingIcon = {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("quick_ai_chip")
                            )
                        }
                        if (profile.userRole == UserRole.PARENT || profile.userRole == UserRole.TEACHER || profile.userRole == UserRole.ADMIN) {
                            Button(
                                onClick = {
                                    onNavigateToTab(if (profile.userRole == UserRole.ADMIN) "admin" else "comms")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quick_role_action_button")
                            ) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    when (profile.userRole) {
                                        UserRole.ADMIN -> "Bulchiinsa & Gabaasa Ji'aa (PDF/Word)"
                                        UserRole.PARENT -> "Qunnamtii Barsiisaa & Mana Barumsaa"
                                        else -> "Qunnamtii Maatii & Early Warning"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Beeksisa fi Odeeffannoo Haaraa",
                    subtitle = "Odeeffannoo Mana Barumsaa Koofkeet irraa"
                )
            }

            if (announcements.isEmpty()) {
                item {
                    EmptyStateBanner(
                        icon = Icons.Filled.Campaign,
                        title = "Beeksisni haaraan hin jiru",
                        message = "Yeroo bulchiinsi mana barumsaa beeksisa baasu asitti mul'ata."
                    )
                }
            } else {
                items(announcements.take(4), key = { it.id }) { ann ->
                    AnnouncementCard(ann)
                }
            }

            item {
                KoofkeetFooterQuote()
            }
        }
    }
}

@Composable
fun AnnouncementCard(announcement: Announcement) {
    val category = AnnouncementCategory.fromRaw(announcement.category)
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (category == AnnouncementCategory.URGENT) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = category.labelOromo,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (category == AnnouncementCategory.URGENT) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = announcement.authorName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = announcement.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = announcement.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StudentPrivateAiTutorCard(
    aiState: AiTutorUiState,
    recentChats: List<AiChatMessage>,
    onAskAiTutor: (prompt: String, subject: String, mode: StudySupportMode) -> Unit,
    onOpenFullTutor: () -> Unit
) {
    val strings = stringsFor(LocalAppLanguage.current)
    val subjects = listOf("Waliigalaa", "Herrega", "Baayoolojii", "Fiiziksii", "Keemistirii", "Afaan Oromoo", "Ingiliffa")
    var selectedSubject by remember { mutableStateOf(subjects.first()) }
    var selectedMode by remember { mutableStateOf(StudySupportMode.EXPLAIN) }
    var quickPrompt by remember { mutableStateOf("") }
    val latestExchanges = remember(recentChats) {
        recentChats.sortedBy { it.createdAt?.seconds ?: 0L }.takeLast(2)
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_ai_tutor_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ai_tutor_avatar),
                        contentDescription = strings.aiTutorTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.aiTutorTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.aiTutorPrivateSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.aiTutorPrivateBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Study support mode chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val modes = listOf(
                    StudySupportMode.EXPLAIN to strings.aiTutorModeExplain,
                    StudySupportMode.HOMEWORK to strings.aiTutorModeHomework,
                    StudySupportMode.PRACTICE_QUIZ to strings.aiTutorModePractice,
                    StudySupportMode.STUDY_PLAN to strings.aiTutorModeStudyPlan
                )
                modes.forEach { (mode, label) ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        label = { Text(label) }
                    )
                }
            }

            // Subject chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                subjects.forEach { subj ->
                    FilterChip(
                        selected = selectedSubject == subj,
                        onClick = { selectedSubject = subj },
                        label = { Text(subj) }
                    )
                }
            }

            // Recent private chat preview
            if (latestExchanges.isNotEmpty() && aiState !is AiTutorUiState.Streaming) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    latestExchanges.forEach { msg ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg.prompt,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg.response,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            when (aiState) {
                is AiTutorUiState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = strings.aiTutorThinking,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is AiTutorUiState.Streaming -> {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = strings.aiTutorStreamingTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = aiState.text,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                is AiTutorUiState.Error -> {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = aiState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                else -> Unit
            }

            OutlinedTextField(
                value = quickPrompt,
                onValueChange = { quickPrompt = it },
                label = { Text(strings.aiTutorAskLabel) },
                placeholder = { Text(strings.aiTutorAskPlaceholder) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_dashboard_ai_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onOpenFullTutor,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("student_dashboard_open_full_ai")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.aiTutorOpenFullChat, style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = {
                        val clean = quickPrompt.trim()
                        if (clean.isNotEmpty()) {
                            onAskAiTutor(clean, selectedSubject, selectedMode)
                            quickPrompt = ""
                        }
                    },
                    enabled = quickPrompt.isNotBlank() &&
                        aiState !is AiTutorUiState.Loading &&
                        aiState !is AiTutorUiState.Streaming,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("student_dashboard_ai_send")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.aiTutorAskButton)
                }
            }
        }
    }
}

@Composable
private fun TeacherDailyAttendanceCard(
    students: List<StudentRegistration>,
    attendance: List<AttendanceRecord>,
    syncStatus: AttendanceSyncStatus,
    onSyncNow: () -> Unit,
    onMarkAttendance: (String, String, String, String, String, AttendanceStatus, String) -> Unit,
    onMarkAllPresent: (String, List<StudentRegistration>, List<AttendanceRecord>) -> Unit,
    onOpenFullAttendance: () -> Unit
) {
    val strings = stringsFor(LocalAppLanguage.current)
    val today = remember { AttendanceDateHelper.todayDateLabel() }
    val todayRecords = remember(attendance, today) {
        attendance.filter { it.dateLabel == today }
    }
    val todayByStudent = remember(todayRecords) {
        todayRecords.associateBy { it.studentId.ifBlank { it.studentName } }
    }
    val presentCount = todayRecords.count { it.status == AttendanceStatus.PRESENT.name }
    val absentCount = todayRecords.count { it.status == AttendanceStatus.ABSENT.name }
    val lateCount = todayRecords.count { it.status == AttendanceStatus.LATE.name }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("teacher_daily_attendance_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.HowToReg,
                        contentDescription = strings.attendanceTitle,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.attendanceTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.attendanceSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = today,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            AttendanceSyncStatusBar(
                syncStatus = syncStatus,
                onSyncNow = onSyncNow,
                buttonTag = "teacher_dash_attendance_sync_button"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text = "${strings.attendancePresent}: $presentCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        text = "${strings.attendanceAbsent}: $absentCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        text = "${strings.attendanceLate}: $lateCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (students.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    students.take(4).forEach { s ->
                        val existing = todayByStudent[s.studentUid]
                        val cur = existing?.let { AttendanceStatus.fromRaw(it.status) }
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.studentName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = s.gradeLevel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = cur == AttendanceStatus.PRESENT,
                                        onClick = {
                                            onMarkAttendance(
                                                s.studentUid,
                                                s.studentName,
                                                s.parentUid,
                                                s.gradeLevel.ifBlank { "Hunda" },
                                                today,
                                                AttendanceStatus.PRESENT,
                                                existing?.note.orEmpty()
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(strings.attendancePresent, maxLines = 1) },
                                        modifier = Modifier.testTag("teacher_dash_present_${s.studentUid}")
                                    )
                                    FilterChip(
                                        selected = cur == AttendanceStatus.ABSENT,
                                        onClick = {
                                            onMarkAttendance(
                                                s.studentUid,
                                                s.studentName,
                                                s.parentUid,
                                                s.gradeLevel.ifBlank { "Hunda" },
                                                today,
                                                AttendanceStatus.ABSENT,
                                                existing?.note.orEmpty()
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(strings.attendanceAbsent, maxLines = 1) },
                                        modifier = Modifier.testTag("teacher_dash_absent_${s.studentUid}")
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = strings.attendanceEmptyRosterMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (students.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { onMarkAllPresent(today, students, todayRecords) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("teacher_dash_mark_all_present")
                    ) {
                        Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.attendanceMarkAllPresent, style = MaterialTheme.typography.labelMedium)
                    }
                }
                TextButton(
                    onClick = onOpenFullAttendance,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("teacher_dash_open_attendance")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.attendanceOpenFullRoll, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

