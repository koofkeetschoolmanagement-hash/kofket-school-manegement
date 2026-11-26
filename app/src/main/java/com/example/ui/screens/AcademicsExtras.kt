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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
    onRecordExamResult: (String, String, String, String, String, String, String, Double, String) -> Unit,
    onDeleteExamResult: (String) -> Unit = {}
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            if (canManage && (profile.userRole == com.example.data.model.UserRole.ADMIN || res.teacherId == profile.userId)) {
                                IconButton(
                                    onClick = { onDeleteExamResult(res.id) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Haqi",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        RecordSubjectGradeDialog(
            students = students,
            onDismiss = { showAddDialog = false },
            onSave = { studentId, studentName, parentId, subject, grade, examName, monthLabel, score, remarks ->
                onRecordExamResult(studentId, studentName, parentId, subject, grade, examName, monthLabel, score, remarks)
                showAddDialog = false
            }
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
        var subject by remember { mutableStateOf("") }
        var grade by remember { mutableStateOf("") }
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
                    enabled = title.trim().length >= 2 &&
                        desc.trim().length >= 2 &&
                        subject.trim().length >= 2 &&
                        grade.trim().isNotEmpty()
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
