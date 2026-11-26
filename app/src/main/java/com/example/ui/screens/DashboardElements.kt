package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.GradeBand
import com.example.core.LocalAppLanguage
import com.example.core.SchoolCurriculum
import com.example.core.stringsFor
import com.example.data.model.Assignment
import com.example.data.model.AssignmentSubmission
import com.example.data.model.ExamResult
import com.example.data.model.QuizAttempt
import com.example.data.model.StudentRegistration
import com.example.data.model.UserProfile
import com.example.data.model.UserRole

@Composable
fun TeacherGradesListCard(
    profile: UserProfile,
    students: List<StudentRegistration>,
    examResults: List<ExamResult>,
    onRecordGrade: (String, String, String, String, String, String, String, Double, String) -> Unit,
    onDeleteGrade: (String) -> Unit,
    onOpenMarklist: () -> Unit
) {
    val strings = stringsFor(LocalAppLanguage.current)
    var selectedBand by remember { mutableStateOf<GradeBand?>(null) }
    var showRecordDialog by remember { mutableStateOf(false) }

    val filteredGrades = remember(examResults, selectedBand) {
        examResults
            .filter { res ->
                when (selectedBand) {
                    null -> true
                    GradeBand.GRADES_1_TO_6 -> SchoolCurriculum.bandForGrade(res.gradeLevel) == GradeBand.GRADES_1_TO_6 ||
                        res.subject in SchoolCurriculum.grades1To6Subjects
                    GradeBand.GRADES_7_TO_8 -> SchoolCurriculum.bandForGrade(res.gradeLevel) == GradeBand.GRADES_7_TO_8 ||
                        res.subject in SchoolCurriculum.grades7To8Subjects
                    GradeBand.OTHER -> true
                }
            }
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("teacher_grades_list_card")
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
                        imageVector = Icons.Filled.Grade,
                        contentDescription = strings.gradesListTitle,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.gradesListTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.gradesListSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showRecordDialog = true },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("teacher_dash_record_grade_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.gradesRecordButton, style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedBand == null,
                    onClick = { selectedBand = null },
                    label = { Text("${strings.gradesBandAll} (${examResults.size})") },
                    modifier = Modifier.testTag("teacher_grades_filter_all")
                )
                FilterChip(
                    selected = selectedBand == GradeBand.GRADES_1_TO_6,
                    onClick = { selectedBand = GradeBand.GRADES_1_TO_6 },
                    label = { Text(strings.gradesBand1To6) },
                    modifier = Modifier.testTag("teacher_grades_filter_1_6")
                )
                FilterChip(
                    selected = selectedBand == GradeBand.GRADES_7_TO_8,
                    onClick = { selectedBand = GradeBand.GRADES_7_TO_8 },
                    label = { Text(strings.gradesBand7To8) },
                    modifier = Modifier.testTag("teacher_grades_filter_7_8")
                )
            }

            if (filteredGrades.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = strings.gradesEmptyTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = strings.gradesEmptyMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("teacher_grades_listview"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredGrades.take(8).forEach { res ->
                        val isLow = res.score < 50.0
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("teacher_grade_row_${res.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = res.studentName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(
                                            shape = MaterialTheme.shapes.extraSmall,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = res.subject,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${res.gradeLevel} • ${res.examName} • ${res.monthLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (res.remarks.isNotBlank()) {
                                        Text(
                                            text = res.remarks,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = if (isLow) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${"%.1f".format(res.score)}%",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLow) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                    if (profile.userRole == UserRole.ADMIN || res.teacherId == profile.userId) {
                                        IconButton(
                                            onClick = { onDeleteGrade(res.id) },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = strings.attendanceDeleteLabel,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onOpenMarklist,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("teacher_grades_open_marklist")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.marklistOpenFull, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    if (showRecordDialog) {
        RecordSubjectGradeDialog(
            students = students,
            onDismiss = { showRecordDialog = false },
            onSave = { studentId, studentName, parentId, subject, grade, examName, monthLabel, score, remarks ->
                onRecordGrade(studentId, studentName, parentId, subject, grade, examName, monthLabel, score, remarks)
                showRecordDialog = false
            }
        )
    }
}

@Composable
fun DashboardAssignmentsCard(
    profile: UserProfile,
    assignments: List<Assignment>,
    onCreateAssignment: (String, String, String, String, String) -> Unit,
    onOpenAssignments: () -> Unit
) {
    val strings = stringsFor(LocalAppLanguage.current)
    val canManage = profile.userRole == UserRole.TEACHER || profile.userRole == UserRole.ADMIN
    var showCreateDialog by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_assignments_card")
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
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = strings.assignmentElementTitle,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.assignmentElementTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.assignmentElementSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (canManage) {
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("dashboard_new_assignment_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.assignmentNewButton, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (assignments.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(strings.assignmentEmptyTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(strings.assignmentEmptyMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    assignments.take(4).forEach { hw ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = hw.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "${hw.subject} • ${hw.gradeLevel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${hw.description} (${hw.dueDateLabel})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onOpenAssignments,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("dashboard_open_assignments_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.assignmentElementTitle, style = MaterialTheme.typography.labelMedium)
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardMarklistCard(
    students: List<StudentRegistration>,
    examResults: List<ExamResult>,
    submissions: List<AssignmentSubmission>,
    quizAttempts: List<QuizAttempt>,
    onOpenFullMarklist: () -> Unit
) {
    val strings = stringsFor(LocalAppLanguage.current)

    val summaryRows = remember(students, examResults, submissions, quizAttempts) {
        val grouped = linkedMapOf<String, MutableList<Pair<String, Double>>>()
        val meta = linkedMapOf<String, Pair<String, String>>() // key -> (name, grade)

        students.forEach { s ->
            val key = s.studentUid.ifBlank { s.studentName }
            if (key.isNotBlank() && s.studentName.isNotBlank()) {
                meta[key] = s.studentName to s.gradeLevel.ifBlank { "Grade 1" }
            }
        }
        examResults.forEach { r ->
            val key = r.studentId.ifBlank { r.studentName }
            if (key.isNotBlank()) {
                meta.putIfAbsent(key, r.studentName to r.gradeLevel)
                grouped.getOrPut(key) { mutableListOf() }.add(r.subject to r.score)
            }
        }
        submissions.filter { it.status == "GRADED" }.forEach { sub ->
            val key = sub.studentId.ifBlank { sub.studentName }
            if (key.isNotBlank()) {
                meta.putIfAbsent(key, sub.studentName to "")
                grouped.getOrPut(key) { mutableListOf() }.add(sub.subject to sub.score)
            }
        }
        quizAttempts.forEach { att ->
            val key = att.studentId.ifBlank { att.studentName }
            if (key.isNotBlank()) {
                meta.putIfAbsent(key, att.studentName to "")
                grouped.getOrPut(key) { mutableListOf() }.add(att.subject to att.score)
            }
        }

        meta.entries.map { (key, info) ->
            val subjectAvgs = grouped[key].orEmpty()
                .groupBy { it.first }
                .mapValues { (_, list) -> list.map { it.second }.average() }
            val overall = if (subjectAvgs.isEmpty()) 0.0 else subjectAvgs.values.average()
            Triple(info.first, info.second.ifBlank { "—" }, subjectAvgs to overall)
        }.sortedByDescending { it.third.second }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_marklist_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.FormatListNumbered,
                    contentDescription = strings.marklistTitle,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.marklistTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.marklistSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (summaryRows.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(strings.marklistEmptyTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(strings.marklistEmptyMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                summaryRows.take(4).forEachIndexed { idx, (name, grade, metrics) ->
                    val (subjectAvgs, overall) = metrics
                    if (idx > 0) HorizontalDivider()
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$grade • ${strings.marklistSubjectsCountLabel}: ${subjectAvgs.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (subjectAvgs.isNotEmpty() && overall < 50.0) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                }
                            ) {
                                Text(
                                    text = if (subjectAvgs.isEmpty()) "—" else "${"%.1f".format(overall)}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (subjectAvgs.isNotEmpty() && overall < 50.0) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (subjectAvgs.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                subjectAvgs.entries.take(5).forEach { (subj, avg) ->
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "$subj: ${"%.0f".format(avg)}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onOpenFullMarklist,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("dashboard_open_marklist_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.marklistOpenFull, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
