package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.ExamResult
import com.example.data.model.LearningMaterial
import com.example.data.model.MaterialType
import com.example.data.model.StudentRegistration
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateBanner
import com.example.ui.components.PrivacyIsolationNotice
import com.example.ui.components.SectionHeader

@Composable
fun ExamResultsTab(
    profile: UserProfile,
    canManage: Boolean,
    students: List<StudentRegistration>,
    examResults: List<ExamResult>,
    onRecordExamResult: (String, String, String, String, String, String, String, Double, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

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
                    title = "Bu'aa Qormaataa (Exam Result)",
                    subtitle = "Qabxii qormaataa keessan qofa ilaalaa",
                    modifier = Modifier.weight(1f)
                )
                if (canManage) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("record_exam_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galchi")
                    }
                }
            }
        }

        item {
            PrivacyIsolationNotice("Barataan tokko qabxii barataa biraa arguu hin danda'u. Qabxiin yoo gad bu'e Early Warning ofumaan ergama.")
        }

        if (examResults.isEmpty()) {
            item {
                EmptyStateBanner(
                    icon = Icons.Filled.Grade,
                    title = "Bu'aan qormaataa hin galmoofne",
                    message = "Yeroo barsiisaan qabxii galchu asitti mul'ata."
                )
            }
        } else {
            items(examResults, key = { it.id }) { res ->
                val isLow = res.score < 50.0
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("${res.examName} — ${res.subject}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${res.studentName} (${res.gradeLevel}) • ${res.monthLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (res.remarks.isNotBlank()) {
                                Text("Yaada: ${res.remarks}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isLow) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${"%.1f".format(res.score)}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isLow) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val defaultStudent = students.firstOrNull()
        var studentId by remember { mutableStateOf(defaultStudent?.studentUid ?: profile.userId) }
        var studentName by remember { mutableStateOf(defaultStudent?.studentName ?: profile.displayName) }
        var parentId by remember { mutableStateOf(defaultStudent?.parentUid.orEmpty()) }
        var subject by remember { mutableStateOf("Herrega") }
        var grade by remember { mutableStateOf(defaultStudent?.gradeLevel ?: "Kutaa 10") }
        var examName by remember { mutableStateOf("Qormaata Ji'aa") }
        var monthLabel by remember { mutableStateOf("Sadaasa 2026") }
        var scoreStr by remember { mutableStateOf("82") }
        var remarks by remember { mutableStateOf("Hojii gaarii") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Bu'aa Qormaataa Galchi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (students.isNotEmpty()) {
                        Text("Barataa filadhu:", style = MaterialTheme.typography.labelMedium)
                        students.take(4).forEach { s ->
                            FilterChip(
                                selected = studentId == s.studentUid,
                                onClick = {
                                    studentId = s.studentUid
                                    studentName = s.studentName
                                    parentId = s.parentUid
                                    grade = s.gradeLevel
                                },
                                label = { Text("${s.studentName} (${s.gradeLevel})") }
                            )
                        }
                    }
                    OutlinedTextField(value = studentName, onValueChange = { studentName = it }, label = { Text("Maqaa Barataa") }, singleLine = true)
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Gosa Barumsaa") }, singleLine = true)
                    OutlinedTextField(value = examName, onValueChange = { examName = it }, label = { Text("Maqaa Qormaataa") }, singleLine = true)
                    OutlinedTextField(value = monthLabel, onValueChange = { monthLabel = it }, label = { Text("Ji'a (Month)") }, singleLine = true)
                    OutlinedTextField(value = scoreStr, onValueChange = { scoreStr = it }, label = { Text("Qabxii (0-100)") }, singleLine = true)
                    OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Yaada") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val score = scoreStr.toDoubleOrNull() ?: 0.0
                        onRecordExamResult(studentId, studentName, parentId, subject, grade, examName, monthLabel, score, remarks)
                        showAddDialog = false
                    },
                    enabled = studentName.trim().length >= 2 && subject.trim().length >= 2
                ) { Text("Galmeessi") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Dhiisi") } }
        )
    }
}

@Composable
fun AttendanceTab(
    profile: UserProfile,
    canManage: Boolean,
    students: List<StudentRegistration>,
    attendance: List<AttendanceRecord>,
    onRecordAttendance: (String, String, String, String, String, AttendanceStatus, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

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
                    title = "To'annoo Argamaa (Attendance History)",
                    subtitle = "Seenaa dhufuu fi hafuu guyyaa guyyaan",
                    modifier = Modifier.weight(1f)
                )
                if (canManage) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("record_attendance_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galmeessi")
                    }
                }
            }
        }

        if (attendance.isEmpty()) {
            item {
                EmptyStateBanner(
                    icon = Icons.Filled.CheckCircle,
                    title = "Seenaan argamaa duwwaadha",
                    message = "Yeroo barsiisaan argamaa galmeessu asitti mul'ata."
                )
            }
        } else {
            items(attendance, key = { it.id }) { rec ->
                val status = AttendanceStatus.fromRaw(rec.status)
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${rec.studentName} (${rec.gradeLevel})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Guyyaa: ${rec.dateLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (rec.note.isNotBlank()) {
                                Text(rec.note, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when (status) {
                                AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primaryContainer
                                AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.errorContainer
                                AttendanceStatus.LATE -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Text(
                                text = status.labelOromo,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val defaultStudent = students.firstOrNull()
        var studentId by remember { mutableStateOf(defaultStudent?.studentUid ?: profile.userId) }
        var studentName by remember { mutableStateOf(defaultStudent?.studentName ?: profile.displayName) }
        var parentId by remember { mutableStateOf(defaultStudent?.parentUid.orEmpty()) }
        var grade by remember { mutableStateOf(defaultStudent?.gradeLevel ?: "Kutaa 10") }
        var dateLabel by remember { mutableStateOf("2026-11-22") }
        var status by remember { mutableStateOf(AttendanceStatus.PRESENT) }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Argamaa Guyyaa Galmeessi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (students.isNotEmpty()) {
                        students.take(4).forEach { s ->
                            FilterChip(
                                selected = studentId == s.studentUid,
                                onClick = {
                                    studentId = s.studentUid
                                    studentName = s.studentName
                                    parentId = s.parentUid
                                    grade = s.gradeLevel
                                },
                                label = { Text("${s.studentName} (${s.gradeLevel})") }
                            )
                        }
                    }
                    OutlinedTextField(value = studentName, onValueChange = { studentName = it }, label = { Text("Maqaa Barataa") }, singleLine = true)
                    OutlinedTextField(value = dateLabel, onValueChange = { dateLabel = it }, label = { Text("Guyyaa (Date)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AttendanceStatus.entries.forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.name) }
                            )
                        }
                    }
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Yaadannoo (Optional)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRecordAttendance(studentId, studentName, parentId, grade, dateLabel, status, note)
                        showAddDialog = false
                    },
                    enabled = studentName.trim().length >= 2 && dateLabel.trim().length >= 4
                ) { Text("Galmeessi") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Dhiisi") } }
        )
    }
}

@Composable
fun MaterialsTab(
    canManage: Boolean,
    materials: List<LearningMaterial>,
    onAddMaterial: (String, String, String, String, MaterialType, String, String) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var readingMaterial by remember { mutableStateOf<LearningMaterial?>(null) }

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
                    title = "Meeshaalee Barnootaa (PDF & Kitaabota)",
                    subtitle = "Kitaabilee fi barreeffamoota buufadhaa",
                    modifier = Modifier.weight(1f)
                )
                if (canManage) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("upload_material_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fe'i")
                    }
                }
            }
        }

        if (materials.isEmpty()) {
            item {
                EmptyStateBanner(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Meeshaan barnootaa hin jiru",
                    message = "Yeroo barsiisaan kitaaba ykn PDF fe'u asitti argattu."
                )
            }
        } else {
            items(materials, key = { it.id }) { mat ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.tertiaryContainer) {
                                Text(
                                    text = "${mat.materialType} • ${mat.subject}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(mat.gradeLevel, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(mat.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(mat.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (mat.contentBody.isNotBlank()) {
                                OutlinedButton(onClick = { readingMaterial = mat }) {
                                    Text("Dubbisi (Read Notes)")
                                }
                            }
                            if (mat.resourceUrl.isNotBlank()) {
                                Button(onClick = {
                                    val url = if (mat.resourceUrl.startsWith("http")) mat.resourceUrl else "https://${mat.resourceUrl}"
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bani / Download")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("Baayoolojii") }
        var grade by remember { mutableStateOf("Kutaa 10") }
        var type by remember { mutableStateOf(MaterialType.PDF) }
        var url by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Meeshaa Barnootaa Fe'i (Upload)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Mata-duree") }, singleLine = true)
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Gosa Barumsaa") }, singleLine = true)
                    OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("Kutaa") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MaterialType.entries.forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.labelOromo) })
                        }
                    }
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Ibsa gabaabaa") })
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Linkii PDF / Kitaabaa (Optional)") }, singleLine = true)
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Barreeffama Yaadannoo (Optional)") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddMaterial(title, desc, subject, grade, type, url, body)
                        showAddDialog = false
                    },
                    enabled = title.trim().length >= 2 && desc.trim().length >= 2
                ) { Text("Fe'i") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Dhiisi") } }
        )
    }

    readingMaterial?.let { mat ->
        AlertDialog(
            onDismissRequest = { readingMaterial = null },
            title = { Text(mat.title) },
            text = { Text(mat.contentBody, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { TextButton(onClick = { readingMaterial = null }) { Text("Cufi") } }
        )
    }
}
