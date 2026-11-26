package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AttendanceDateHelper
import com.example.core.GradeBand
import com.example.core.LocalAppLanguage
import com.example.core.SchoolCurriculum
import com.example.core.stringsFor
import com.example.data.model.AssignmentSubmission
import com.example.data.model.ExamResult
import com.example.data.model.QuizAttempt
import com.example.data.model.StudentRegistration
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.components.EmptyStateBanner
import com.example.ui.components.PrivacyIsolationNotice
import com.example.ui.components.SectionHeader

private data class StudentMarklistRow(
    val studentKey: String,
    val studentUid: String,
    val studentName: String,
    val parentUid: String,
    val gradeLevel: String,
    val subjectAverages: Map<String, Double>,
    val overallAverage: Double,
    val rawResults: List<ExamResult>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarklistTab(
    profile: UserProfile,
    canManage: Boolean,
    students: List<StudentRegistration>,
    examResults: List<ExamResult>,
    submissions: List<AssignmentSubmission>,
    quizAttempts: List<QuizAttempt>,
    onRecordGrade: (String, String, String, String, String, String, String, Double, String) -> Unit,
    onDeleteGrade: (String) -> Unit = {}
) {
    val strings = stringsFor(LocalAppLanguage.current)
    var selectedBand by remember { mutableStateOf<GradeBand?>(null) }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showRecordDialog by remember { mutableStateOf(false) }
    var preselectedStudent by remember { mutableStateOf<StudentRegistration?>(null) }

    val availableSubjects = remember(selectedBand) {
        when (selectedBand) {
            GradeBand.GRADES_1_TO_6 -> SchoolCurriculum.grades1To6Subjects
            GradeBand.GRADES_7_TO_8 -> SchoolCurriculum.grades7To8Subjects
            else -> SchoolCurriculum.generalSubjects
        }
    }

    val marklistRows = remember(students, examResults, submissions, quizAttempts, selectedBand, selectedSubject, searchQuery) {
        val grouped = linkedMapOf<String, MutableList<Pair<String, Double>>>()
        val meta = linkedMapOf<String, Triple<String, String, String>>() // key -> (uid, name, grade)
        val parentMap = linkedMapOf<String, String>()
        val rawByStudent = linkedMapOf<String, MutableList<ExamResult>>()

        students.forEach { s ->
            val key = s.studentUid.ifBlank { s.studentName }
            if (key.isNotBlank() && s.studentName.isNotBlank()) {
                meta[key] = Triple(s.studentUid, s.studentName, s.gradeLevel.ifBlank { "Grade 1" })
                parentMap[key] = s.parentUid
            }
        }

        examResults.forEach { r ->
            val key = r.studentId.ifBlank { r.studentName }
            if (key.isNotBlank()) {
                meta.putIfAbsent(key, Triple(r.studentId, r.studentName, r.gradeLevel))
                if (r.parentId.isNotBlank()) parentMap.putIfAbsent(key, r.parentId)
                grouped.getOrPut(key) { mutableListOf() }.add(r.subject to r.score)
                rawByStudent.getOrPut(key) { mutableListOf() }.add(r)
            }
        }

        submissions.filter { it.status == "GRADED" }.forEach { sub ->
            val key = sub.studentId.ifBlank { sub.studentName }
            if (key.isNotBlank()) {
                meta.putIfAbsent(key, Triple(sub.studentId, sub.studentName, ""))
                grouped.getOrPut(key) { mutableListOf() }.add(sub.subject to sub.score)
            }
        }

        quizAttempts.forEach { att ->
            val key = att.studentId.ifBlank { att.studentName }
            if (key.isNotBlank()) {
                meta.putIfAbsent(key, Triple(att.studentId, att.studentName, ""))
                grouped.getOrPut(key) { mutableListOf() }.add(att.subject to att.score)
            }
        }

        val q = searchQuery.trim().lowercase()
        meta.entries.mapNotNull { (key, info) ->
            val (uid, name, grade) = info
            if (selectedBand != null && SchoolCurriculum.bandForGrade(grade) != selectedBand) return@mapNotNull null
            if (q.isNotEmpty() && !name.lowercase().contains(q) && !grade.lowercase().contains(q)) return@mapNotNull null

            val subjectPairs = grouped[key].orEmpty()
            val subjectAvgs = subjectPairs
                .groupBy { it.first }
                .mapValues { (_, list) -> list.map { it.second }.average() }

            if (selectedSubject != null && !subjectAvgs.containsKey(selectedSubject)) {
                // Keep row if no filter, otherwise filter strictly to students with that subject
                return@mapNotNull null
            }

            val overall = if (subjectAvgs.isEmpty()) 0.0 else subjectAvgs.values.average()
            StudentMarklistRow(
                studentKey = key,
                studentUid = uid,
                studentName = name,
                parentUid = parentMap[key].orEmpty(),
                gradeLevel = grade.ifBlank { "—" },
                subjectAverages = subjectAvgs,
                overallAverage = overall,
                rawResults = rawByStudent[key].orEmpty()
            )
        }.sortedWith(compareByDescending<StudentMarklistRow> { it.overallAverage }.thenBy { it.studentName.lowercase() })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("marklist_tab_list"),
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
                    title = strings.marklistTitle,
                    subtitle = strings.marklistSubtitle,
                    modifier = Modifier.weight(1f)
                )
                if (canManage) {
                    Button(
                        onClick = {
                            preselectedStudent = students.firstOrNull()
                            showRecordDialog = true
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("marklist_record_grade_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.gradesRecordButton)
                    }
                }
            }
        }

        item {
            PrivacyIsolationNotice("Barataan tokko qabxii barataa biraa arguu hin danda'u. Qabxiin hundinuu Firestore irratti eegamaadha.")
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text(strings.attendanceSearchPlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedBand == null,
                        onClick = {
                            selectedBand = null
                            selectedSubject = null
                        },
                        label = { Text(strings.gradesBandAll) }
                    )
                    FilterChip(
                        selected = selectedBand == GradeBand.GRADES_1_TO_6,
                        onClick = {
                            selectedBand = GradeBand.GRADES_1_TO_6
                            if (selectedSubject !in SchoolCurriculum.grades1To6Subjects) selectedSubject = null
                        },
                        label = { Text(strings.gradesBand1To6) }
                    )
                    FilterChip(
                        selected = selectedBand == GradeBand.GRADES_7_TO_8,
                        onClick = {
                            selectedBand = GradeBand.GRADES_7_TO_8
                            if (selectedSubject !in SchoolCurriculum.grades7To8Subjects) selectedSubject = null
                        },
                        label = { Text(strings.gradesBand7To8) }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedSubject == null,
                        onClick = { selectedSubject = null },
                        label = { Text(strings.attendanceAllStatuses) }
                    )
                    availableSubjects.forEach { subj ->
                        FilterChip(
                            selected = selectedSubject == subj,
                            onClick = { selectedSubject = if (selectedSubject == subj) null else subj },
                            label = { Text(subj) }
                        )
                    }
                }
            }
        }

        if (marklistRows.isEmpty()) {
            item {
                EmptyStateBanner(
                    icon = Icons.Filled.FormatListNumbered,
                    title = strings.marklistEmptyTitle,
                    message = strings.marklistEmptyMessage
                )
            }
        } else {
            items(marklistRows, key = { it.studentKey }) { row ->
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.testTag("marklist_card_${row.studentKey}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row.studentName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${row.gradeLevel} • ${strings.marklistSubjectsCountLabel}: ${row.subjectAverages.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (row.subjectAverages.isNotEmpty() && row.overallAverage < 50.0) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            ) {
                                Text(
                                    text = if (row.subjectAverages.isEmpty()) "—" else "${"%.1f".format(row.overallAverage)}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (row.subjectAverages.isNotEmpty() && row.overallAverage < 50.0) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (row.subjectAverages.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                row.subjectAverages.forEach { (subj, avg) ->
                                    val isLow = avg < 50.0
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = if (isLow) {
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = "$subj: ${"%.1f".format(avg)}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isLow) {
                                                MaterialTheme.colorScheme.onErrorContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (canManage && row.rawResults.isNotEmpty()) {
                            HorizontalDivider()
                            row.rawResults.sortedByDescending { it.createdAt?.seconds ?: 0L }.take(3).forEach { r ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${r.subject} (${r.examName}) — ${"%.1f".format(r.score)}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (profile.userRole == UserRole.ADMIN || r.teacherId == profile.userId) {
                                        IconButton(
                                            onClick = { onDeleteGrade(r.id) },
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

                        if (canManage) {
                            OutlinedButton(
                                onClick = {
                                    preselectedStudent = StudentRegistration(
                                        studentUid = row.studentUid,
                                        studentName = row.studentName,
                                        gradeLevel = row.gradeLevel,
                                        parentUid = row.parentUid
                                    )
                                    showRecordDialog = true
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.gradesRecordButton)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRecordDialog) {
        RecordSubjectGradeDialog(
            students = students,
            initialStudent = preselectedStudent,
            onDismiss = { showRecordDialog = false },
            onSave = { sid, sname, pid, subj, gr, ex, mon, sc, rem ->
                onRecordGrade(sid, sname, pid, subj, gr, ex, mon, sc, rem)
                showRecordDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordSubjectGradeDialog(
    students: List<StudentRegistration>,
    initialStudent: StudentRegistration? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, Double, String) -> Unit
) {
    val strings = stringsFor(LocalAppLanguage.current)
    val defaultStudent = initialStudent ?: students.firstOrNull()
    var studentId by remember { mutableStateOf(defaultStudent?.studentUid.orEmpty()) }
    var studentName by remember { mutableStateOf(defaultStudent?.studentName.orEmpty()) }
    var parentId by remember { mutableStateOf(defaultStudent?.parentUid.orEmpty()) }
    var grade by remember { mutableStateOf(defaultStudent?.gradeLevel?.ifBlank { "Grade 1" } ?: "Grade 1") }
    var selectedBand by remember {
        mutableStateOf(
            when (SchoolCurriculum.bandForGrade(grade)) {
                GradeBand.GRADES_7_TO_8 -> GradeBand.GRADES_7_TO_8
                else -> GradeBand.GRADES_1_TO_6
            }
        )
    }
    val bandSubjects = SchoolCurriculum.subjectsForBand(selectedBand)
    var subject by remember { mutableStateOf(bandSubjects.first()) }
    var examName by remember { mutableStateOf("Class Assessment") }
    var monthLabel by remember { mutableStateOf(AttendanceDateHelper.todayDateLabel().take(7)) }
    var scoreStr by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    val parsedScore = scoreStr.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.gradesRecordButton) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (students.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        students.take(8).forEach { s ->
                            FilterChip(
                                selected = studentId == s.studentUid && studentName == s.studentName,
                                onClick = {
                                    studentId = s.studentUid
                                    studentName = s.studentName
                                    parentId = s.parentUid
                                    val resolvedGrade = s.gradeLevel.ifBlank { "Grade 1" }
                                    grade = resolvedGrade
                                    val detected = SchoolCurriculum.bandForGrade(resolvedGrade)
                                    if (detected != GradeBand.OTHER) {
                                        selectedBand = detected
                                        val nextList = SchoolCurriculum.subjectsForBand(detected)
                                        if (subject !in nextList) subject = nextList.first()
                                    }
                                },
                                label = { Text("${s.studentName} (${s.gradeLevel})") }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = studentName,
                    onValueChange = {
                        studentName = it
                        if (students.none { s -> s.studentName == it }) studentId = ""
                    },
                    label = { Text(strings.fullNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("grade_dialog_student_name")
                )
                OutlinedTextField(
                    value = grade,
                    onValueChange = {
                        grade = it
                        val detected = SchoolCurriculum.bandForGrade(it)
                        if (detected != GradeBand.OTHER) {
                            selectedBand = detected
                            val list = SchoolCurriculum.subjectsForBand(detected)
                            if (subject !in list) subject = list.first()
                        }
                    },
                    label = { Text(strings.gradeLevelLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("grade_dialog_grade")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedBand == GradeBand.GRADES_1_TO_6,
                        onClick = {
                            selectedBand = GradeBand.GRADES_1_TO_6
                            if (subject !in SchoolCurriculum.grades1To6Subjects) {
                                subject = SchoolCurriculum.grades1To6Subjects.first()
                            }
                        },
                        label = { Text(strings.gradesBand1To6) },
                        modifier = Modifier.testTag("grade_dialog_band_1_6")
                    )
                    FilterChip(
                        selected = selectedBand == GradeBand.GRADES_7_TO_8,
                        onClick = {
                            selectedBand = GradeBand.GRADES_7_TO_8
                            if (subject !in SchoolCurriculum.grades7To8Subjects) {
                                subject = SchoolCurriculum.grades7To8Subjects.first()
                            }
                        },
                        label = { Text(strings.gradesBand7To8) },
                        modifier = Modifier.testTag("grade_dialog_band_7_8")
                    )
                }

                Text(strings.gradesSelectSubject, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SchoolCurriculum.subjectsForBand(selectedBand).forEach { subj ->
                        FilterChip(
                            selected = subject == subj,
                            onClick = { subject = subj },
                            label = { Text(subj) }
                        )
                    }
                }

                OutlinedTextField(
                    value = scoreStr,
                    onValueChange = { scoreStr = it },
                    label = { Text(strings.gradesScoreLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("grade_dialog_score")
                )
                OutlinedTextField(
                    value = examName,
                    onValueChange = { examName = it },
                    label = { Text(strings.gradesAssessmentLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monthLabel,
                    onValueChange = { monthLabel = it },
                    label = { Text(strings.gradesTermLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text(strings.gradesRemarksLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        studentId,
                        studentName,
                        parentId,
                        subject,
                        grade,
                        examName,
                        monthLabel,
                        parsedScore ?: 0.0,
                        remarks
                    )
                },
                enabled = studentName.trim().length >= 2 &&
                    grade.trim().isNotEmpty() &&
                    subject.trim().length >= 2 &&
                    examName.trim().length >= 2 &&
                    monthLabel.trim().length >= 2 &&
                    parsedScore != null && parsedScore in 0.0..100.0,
                modifier = Modifier.testTag("grade_dialog_save_button")
            ) { Text(strings.attendanceSaveLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancelLabel) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateCurriculumAssignmentDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String, subject: String, gradeLevel: String, dueDateLabel: String) -> Unit
) {
    val strings = stringsFor(LocalAppLanguage.current)
    var title by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("Grade 1") }
    var selectedBand by remember { mutableStateOf(GradeBand.GRADES_1_TO_6) }
    val subjects = SchoolCurriculum.subjectsForBand(selectedBand)
    var subject by remember { mutableStateOf(subjects.first()) }
    var due by remember { mutableStateOf(AttendanceDateHelper.todayDateLabel()) }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.assignmentNewButton) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Mata-duree (Title)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("assignment_dialog_title")
                )
                OutlinedTextField(
                    value = grade,
                    onValueChange = {
                        grade = it
                        val detected = SchoolCurriculum.bandForGrade(it)
                        if (detected != GradeBand.OTHER) {
                            selectedBand = detected
                            val list = SchoolCurriculum.subjectsForBand(detected)
                            if (subject !in list) subject = list.first()
                        }
                    },
                    label = { Text(strings.gradeLevelLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("assignment_dialog_grade")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedBand == GradeBand.GRADES_1_TO_6,
                        onClick = {
                            selectedBand = GradeBand.GRADES_1_TO_6
                            if (subject !in SchoolCurriculum.grades1To6Subjects) {
                                subject = SchoolCurriculum.grades1To6Subjects.first()
                            }
                        },
                        label = { Text(strings.gradesBand1To6) }
                    )
                    FilterChip(
                        selected = selectedBand == GradeBand.GRADES_7_TO_8,
                        onClick = {
                            selectedBand = GradeBand.GRADES_7_TO_8
                            if (subject !in SchoolCurriculum.grades7To8Subjects) {
                                subject = SchoolCurriculum.grades7To8Subjects.first()
                            }
                        },
                        label = { Text(strings.gradesBand7To8) }
                    )
                }
                Text(strings.gradesSelectSubject, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SchoolCurriculum.subjectsForBand(selectedBand).forEach { subj ->
                        FilterChip(
                            selected = subject == subj,
                            onClick = { subject = subj },
                            label = { Text(subj) }
                        )
                    }
                }
                OutlinedTextField(
                    value = due,
                    onValueChange = { due = it },
                    label = { Text("Guyyaa dhumaa (Due Date)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Ibsa fi Gaaffilee") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("assignment_dialog_desc")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title.trim(), desc.trim(), subject.trim(), grade.trim(), due.trim()) },
                enabled = title.trim().length >= 2 &&
                    desc.trim().length >= 2 &&
                    subject.trim().length >= 2 &&
                    grade.trim().isNotEmpty() &&
                    due.trim().isNotEmpty(),
                modifier = Modifier.testTag("assignment_dialog_save_button")
            ) { Text(strings.attendanceSaveLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancelLabel) }
        }
    )
}

