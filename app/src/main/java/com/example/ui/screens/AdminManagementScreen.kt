package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.MonthlyReportData
import com.example.core.ReportExporter
import com.example.data.model.StudentRegistration
import com.example.data.model.TeacherAssignment
import com.example.data.model.UserRole
import com.example.ui.components.EmptyStateBanner
import com.example.ui.components.SectionHeader
import kotlinx.coroutines.launch

@Composable
fun AdminManagementScreen(
    teachers: List<TeacherAssignment>,
    students: List<StudentRegistration>,
    buildMonthlyReport: (String) -> MonthlyReportData,
    onAddTeacherAssignment: (String, String, String, String, String) -> Unit,
    onRegisterStudent: (String, String, String, String, String, String, String) -> Unit,
    onIssueRoleGrant: (String, UserRole, String, String, String, String, String) -> Unit,
    onShowStatus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedMonth by remember { mutableStateOf("Hunda") }
    val reportData = buildMonthlyReport(selectedMonth)

    var showTeacherDialog by remember { mutableStateOf(false) }
    var showStudentDialog by remember { mutableStateOf(false) }
    var showRoleGrantDialog by remember { mutableStateOf(false) }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = ReportExporter.exportPdf(context, uri, reportData)
                res.fold(
                    onSuccess = { onShowStatus("Gabaasni Ji'aa bifa PDF-iin milkaa'inaan buufameera!") },
                    onFailure = { err -> onShowStatus("PDF export hin milkoofne: ${err.localizedMessage}") }
                )
            }
        }
    }

    val docxExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = ReportExporter.exportDocx(context, uri, reportData)
                res.fold(
                    onSuccess = { onShowStatus("Gabaasni Ji'aa bifa Microsoft Word (.docx)-iin buufameera!") },
                    onFailure = { err -> onShowStatus("Word export hin milkoofne: ${err.localizedMessage}") }
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 14. Gabaasa Ji'aa (Monthly Report + PDF & Word Export)
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SectionHeader(
                            title = "Gabaasa Ji'aa (Monthly Report)",
                            subtitle = "Raawwii mana barumsaa PDF fi Microsoft Word-iin buufadhaa"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Hunda", "Sadaasa 2026", "Muddee 2026").forEach { m ->
                                FilterChip(
                                    selected = selectedMonth == m,
                                    onClick = { selectedMonth = m },
                                    label = { Text(m) }
                                )
                            }
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Barattoota Galmaa'an: ${reportData.totalStudents}", style = MaterialTheme.typography.bodyMedium)
                                Text("Barsiisota Ramadaman: ${reportData.totalTeachers}", style = MaterialTheme.typography.bodyMedium)
                                Text("Giddu-galeessa Qabxii: ${"%.1f".format(reportData.averageExamScore)}%", style = MaterialTheme.typography.bodyMedium)
                                Text("Argamaa Barattootaa: ${"%.1f".format(reportData.attendanceRatePercent)}%", style = MaterialTheme.typography.bodyMedium)
                                Text("Early Warnings: ${reportData.openEarlyWarnings} banaa / ${reportData.resolvedEarlyWarnings} furame", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { pdfExportLauncher.launch("Koofkeet_Gabaasa_Jiaa.pdf") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_pdf_button")
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PDF Buufadhu")
                            }
                            Button(
                                onClick = { docxExportLauncher.launch("Koofkeet_Gabaasa_Jiaa.docx") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_word_button")
                            ) {
                                Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Word (.docx)")
                            }
                        }
                    }
                }
            }

            // Role Grants (Teacher / Parent / Admin Google account invitations)
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mirga Fayyadamaa (Role Grants)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Imeelii Barsiisaa ykn Maatii galmeessuun gahee kennaa", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(
                            onClick = { showRoleGrantDialog = true },
                            modifier = Modifier.testTag("issue_role_grant_button")
                        ) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gahee Kenni")
                        }
                    }
                }
            }

            // 12. Ramaddii Barsiisotaa
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Ramaddii Barsiisotaa (${teachers.size})",
                        subtitle = "Barsiisota gosa barnootaa fi kutaatti ramadaa",
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { showTeacherDialog = true },
                        modifier = Modifier.testTag("assign_teacher_button")
                    ) {
                        Icon(Icons.Filled.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ramadi")
                    }
                }
            }

            if (teachers.isEmpty()) {
                item {
                    EmptyStateBanner(
                        icon = Icons.Filled.GroupAdd,
                        title = "Ramaddiin barsiisaa hin jiru",
                        message = "Barsiisaa kutaa fi gosa barnootaatti ramaduuf 'Ramadi' tuqaa."
                    )
                }
            } else {
                items(teachers, key = { it.id }) { t ->
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
                                Text(t.teacherName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("${t.subject} • ${t.gradeLevel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(t.schedule, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 13. Galmee Barattootaa
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Galmee Barattootaa (${students.size})",
                        subtitle = "Barattoota galmeessuu fi kutaa ramaduu",
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { showStudentDialog = true },
                        modifier = Modifier.testTag("register_student_button")
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galmeessi")
                    }
                }
            }

            if (students.isEmpty()) {
                item {
                    EmptyStateBanner(
                        icon = Icons.Filled.Add,
                        title = "Barataan hin galmoofne",
                        message = "Barattoota galmeessuuf 'Galmeessi' tuqaa."
                    )
                }
            } else {
                items(students, key = { it.id }) { s ->
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
                                Text("${s.studentName} (${s.studentCode})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("${s.gradeLevel} • Kutaa ${s.section}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (s.parentName.isNotBlank()) {
                                    Text("Maatii: ${s.parentName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(s.status, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTeacherDialog) {
        var name by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("Herrega") }
        var grade by remember { mutableStateOf("Kutaa 10") }
        var schedule by remember { mutableStateOf("Wiixata - Jimaata (8:00 AM)") }
        AlertDialog(
            onDismissRequest = { showTeacherDialog = false },
            title = { Text("Ramaddii Barsiisaa") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Maqaa Barsiisaa") }, singleLine = true)
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Gosa Barumsaa") }, singleLine = true)
                    OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("Kutaa") }, singleLine = true)
                    OutlinedTextField(value = schedule, onValueChange = { schedule = it }, label = { Text("Sagantaa (Schedule)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddTeacherAssignment("", name, subject, grade, schedule)
                        showTeacherDialog = false
                    },
                    enabled = name.trim().length >= 2 && subject.trim().length >= 2
                ) { Text("Ramadi") }
            },
            dismissButton = { TextButton(onClick = { showTeacherDialog = false }) { Text("Dhiisi") } }
        )
    }

    if (showStudentDialog) {
        var studentUid by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("KF-${100 + students.size}") }
        var grade by remember { mutableStateOf("Kutaa 10") }
        var section by remember { mutableStateOf("A") }
        var parentUid by remember { mutableStateOf("") }
        var parentName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showStudentDialog = false },
            title = { Text("Galmee Barataa Haaraa") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Maqaa Barataa") }, singleLine = true)
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Lakk. Galmee (ID Code)") }, singleLine = true)
                    OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("Kutaa (Grade)") }, singleLine = true)
                    OutlinedTextField(value = section, onValueChange = { section = it }, label = { Text("Daree (Section)") }, singleLine = true)
                    OutlinedTextField(value = parentName, onValueChange = { parentName = it }, label = { Text("Maqaa Maatii (Optional)") }, singleLine = true)
                    OutlinedTextField(value = parentUid, onValueChange = { parentUid = it }, label = { Text("UID Maatii (Optional)") }, singleLine = true)
                    OutlinedTextField(value = studentUid, onValueChange = { studentUid = it }, label = { Text("UID Barataa (Optional)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRegisterStudent(studentUid, name, code, grade, section, parentUid, parentName)
                        showStudentDialog = false
                    },
                    enabled = name.trim().length >= 2 && code.trim().length >= 2
                ) { Text("Galmeessi") }
            },
            dismissButton = { TextButton(onClick = { showStudentDialog = false }) { Text("Dhiisi") } }
        )
    }

    if (showRoleGrantDialog) {
        var email by remember { mutableStateOf("") }
        var fullName by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf(UserRole.TEACHER) }
        var grade by remember { mutableStateOf("Kutaa 10") }
        var subject by remember { mutableStateOf("Herrega") }
        var linkedStudentName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRoleGrantDialog = false },
            title = { Text("Gahee Fayyadamaa Kenni") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Google Email") }, singleLine = true)
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Maqaa Guutuu") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(UserRole.TEACHER, UserRole.PARENT, UserRole.ADMIN).forEach { r ->
                            FilterChip(selected = selectedRole == r, onClick = { selectedRole = r }, label = { Text(r.labelOromo) })
                        }
                    }
                    if (selectedRole == UserRole.TEACHER) {
                        OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Gosa Barumsaa") }, singleLine = true)
                        OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("Kutaa") }, singleLine = true)
                    } else if (selectedRole == UserRole.PARENT) {
                        OutlinedTextField(value = linkedStudentName, onValueChange = { linkedStudentName = it }, label = { Text("Maqaa Ilma/Intalaa") }, singleLine = true)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onIssueRoleGrant(email, selectedRole, fullName, grade, subject, "", linkedStudentName)
                        showRoleGrantDialog = false
                    },
                    enabled = email.contains("@") && fullName.trim().length >= 2
                ) { Text("Kenni") }
            },
            dismissButton = { TextButton(onClick = { showRoleGrantDialog = false }) { Text("Dhiisi") } }
        )
    }
}
