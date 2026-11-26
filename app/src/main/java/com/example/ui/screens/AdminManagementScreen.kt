package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.AppLanguage
import com.example.core.LocalAppLanguage
import com.example.core.MonthlyReportData
import com.example.core.ReportExporter
import com.example.data.model.BOOTSTRAP_ADMIN_EMAIL
import com.example.data.model.RoleGrant
import com.example.data.model.StudentRegistration
import com.example.data.model.TeacherAssignment
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.components.EmptyStateBanner
import com.example.ui.components.RoleBadgePill
import com.example.ui.components.SectionHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminManagementScreen(
    currentAdminUid: String,
    users: List<UserProfile>,
    roleGrants: List<RoleGrant>,
    teachers: List<TeacherAssignment>,
    students: List<StudentRegistration>,
    availableMonths: List<String>,
    buildMonthlyReport: (String) -> MonthlyReportData,
    onAddTeacherAssignment: (String, String, String, String, String) -> Unit,
    onRegisterStudent: (String, String, String, String, String, String, String) -> Unit,
    onIssueRoleGrant: (String, UserRole, String, String, String, String, String) -> Unit,
    onUpdateUserRole: (UserProfile, UserRole, String, String) -> Unit,
    onRevokeRoleGrant: (String) -> Unit,
    onShowStatus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appLanguage = LocalAppLanguage.current

    var selectedMonth by remember(availableMonths) {
        mutableStateOf(availableMonths.firstOrNull() ?: "Hunda")
    }
    val reportData = buildMonthlyReport(selectedMonth)

    var selectedRoleFilter by remember { mutableStateOf<UserRole?>(null) }
    var userToEditRole by remember { mutableStateOf<UserProfile?>(null) }

    var showTeacherDialog by remember { mutableStateOf(false) }
    var showStudentDialog by remember { mutableStateOf(false) }
    var showRoleGrantDialog by remember { mutableStateOf(false) }

    val sortedFilteredUsers = remember(users, selectedRoleFilter) {
        users
            .filter { selectedRoleFilter == null || it.userRole == selectedRoleFilter }
            .sortedWith(
                compareBy<UserProfile> {
                    when (it.userRole) {
                        UserRole.ADMIN -> 0
                        UserRole.TEACHER -> 1
                        UserRole.PARENT -> 2
                        UserRole.STUDENT -> 3
                    }
                }.thenBy { it.displayName.lowercase() }
            )
    }

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

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = ReportExporter.exportCsv(context, uri, reportData)
                res.fold(
                    onSuccess = { onShowStatus("Gabaasni Ji'aa bifa CSV-iin milkaa'inaan buufameera!") },
                    onFailure = { err -> onShowStatus("CSV export hin milkoofne: ${err.localizedMessage}") }
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
            // User Role Management Section (Admin / Teacher / Student / Parent)
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
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
                                Icon(
                                    imageVector = Icons.Filled.ManageAccounts,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                SectionHeader(
                                    title = when (appLanguage) {
                                        AppLanguage.ENGLISH -> "User Role Management (${users.size})"
                                        AppLanguage.AMHARIC -> "የተጠቃሚ ሚና አስተዳደር (${users.size})"
                                        AppLanguage.AFAAN_OROMOO -> "Bulchiinsa Gahee Fayyadamtootaa (${users.size})"
                                    },
                                    subtitle = when (appLanguage) {
                                        AppLanguage.ENGLISH -> "Distinguish and assign Admin, Teacher, Student & Parent roles in Firestore"
                                        AppLanguage.AMHARIC -> "የአስተዳዳሪ፣ መምህር፣ ተማሪ እና ወላጅ ሚናዎችን በFirestore ያስተዳድሩ"
                                        AppLanguage.AFAAN_OROMOO -> "Account Admin, Barsiisaa, Barataa fi Maatii Firestore irratti to'adhaa"
                                    }
                                )
                            }
                        }

                        // Role counts summary strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedRoleFilter == null,
                                onClick = { selectedRoleFilter = null },
                                label = { Text("Hunda (${users.size})") },
                                modifier = Modifier.testTag("filter_role_all")
                            )
                            UserRole.entries.forEach { role ->
                                val count = users.count { it.userRole == role }
                                FilterChip(
                                    selected = selectedRoleFilter == role,
                                    onClick = {
                                        selectedRoleFilter = if (selectedRoleFilter == role) null else role
                                    },
                                    label = {
                                        val label = when (appLanguage) {
                                            AppLanguage.ENGLISH -> role.labelEnglish
                                            else -> role.labelOromo.substringBefore(" ")
                                        }
                                        Text("$label ($count)")
                                    },
                                    modifier = Modifier.testTag("filter_role_${role.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            if (sortedFilteredUsers.isEmpty()) {
                item {
                    EmptyStateBanner(
                        icon = Icons.Filled.ManageAccounts,
                        title = "Fayyadamaan hin argamne",
                        message = "Fayyadamtoonni Google-n yoo seenan asitti argamu; akkasumas 'Gahee Kenni' tuquun dursanii ramaduu dandeessu."
                    )
                }
            } else {
                items(sortedFilteredUsers, key = { "user_${it.userId}" }) { user ->
                    val isBootstrap = user.email.equals(BOOTSTRAP_ADMIN_EMAIL, ignoreCase = true)
                    val isSelf = user.userId == currentAdminUid
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isSelf) "${user.displayName} (Isin)" else user.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val metaDetail = listOfNotNull(
                                        user.subject.takeIf { it.isNotBlank() },
                                        user.gradeLevel.takeIf { it.isNotBlank() }
                                    ).joinToString(" • ")
                                    if (metaDetail.isNotBlank()) {
                                        Text(
                                            text = metaDetail,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (!isBootstrap) {
                                    OutlinedButton(
                                        onClick = { userToEditRole = user },
                                        modifier = Modifier.testTag("change_role_${user.userId}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Gahee jijjiiri",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (appLanguage == AppLanguage.ENGLISH) "Change Role" else "Jijjiiri"
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AdminPanelSettings,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Primary Admin",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                            RoleBadgePill(role = user.userRole)
                        }
                    }
                }
            }

            // Role Grants (Pre-assigned Google account invitations)
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
                            Text(
                                text = "Mirga Fayyadamaa Dursaa (Role Grants: ${roleGrants.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Imeelii Admin, Barsiisaa, Barataa ykn Maatii dursanii galmeessaa",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
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

            if (roleGrants.isNotEmpty()) {
                items(roleGrants, key = { "grant_${it.email}" }) { grant ->
                    val role = UserRole.fromRaw(grant.role)
                    val claimed = grant.claimedByUid.isNotBlank()
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
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = grant.fullName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = grant.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RoleBadgePill(role = role)
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = if (claimed) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = if (claimed) "Fudhatameera (Active)" else "Eegamaa (Pending)",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { onRevokeRoleGrant(grant.email) },
                                modifier = Modifier.testTag("revoke_grant_${grant.email}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Mirga haqi",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableMonths.forEach { m ->
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { pdfExportLauncher.launch("Koofkeet_Gabaasa_Jiaa.pdf") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_pdf_button")
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF", maxLines = 1)
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
                                Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Word", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { csvExportLauncher.launch("Koofkeet_Gabaasa_Jiaa.csv") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_csv_button")
                            ) {
                                Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CSV", maxLines = 1)
                            }
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

    userToEditRole?.let { targetUser ->
        var editedRole by remember(targetUser.userId) { mutableStateOf(targetUser.userRole) }
        var editedSubject by remember(targetUser.userId) { mutableStateOf(targetUser.subject) }
        var editedGrade by remember(targetUser.userId) { mutableStateOf(targetUser.gradeLevel) }
        AlertDialog(
            onDismissRequest = { userToEditRole = null },
            title = { Text("Gahee Jijjiiri: ${targetUser.displayName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = targetUser.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        UserRole.entries.forEach { role ->
                            FilterChip(
                                selected = editedRole == role,
                                onClick = { editedRole = role },
                                label = {
                                    val label = if (appLanguage == AppLanguage.ENGLISH) {
                                        role.labelEnglish
                                    } else {
                                        role.labelOromo.substringBefore(" ")
                                    }
                                    Text(label)
                                },
                                modifier = Modifier.testTag("edit_role_option_${role.name.lowercase()}")
                            )
                        }
                    }
                    if (editedRole == UserRole.TEACHER) {
                        OutlinedTextField(
                            value = editedSubject,
                            onValueChange = { editedSubject = it },
                            label = { Text("Gosa Barumsaa (Subject)") },
                            singleLine = true
                        )
                    }
                    if (editedRole == UserRole.TEACHER || editedRole == UserRole.STUDENT) {
                        OutlinedTextField(
                            value = editedGrade,
                            onValueChange = { editedGrade = it },
                            label = { Text("Kutaa (Grade Level)") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateUserRole(targetUser, editedRole, editedSubject, editedGrade)
                        userToEditRole = null
                    },
                    modifier = Modifier.testTag("confirm_role_change_button")
                ) { Text("Olkaa'i") }
            },
            dismissButton = {
                TextButton(onClick = { userToEditRole = null }) { Text("Dhiisi") }
            }
        )
    }

    if (showTeacherDialog) {
        var name by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("") }
        var grade by remember { mutableStateOf("") }
        var schedule by remember { mutableStateOf("") }
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
                    enabled = name.trim().length >= 2 &&
                        subject.trim().length >= 2 &&
                        grade.trim().isNotEmpty() &&
                        schedule.trim().isNotEmpty()
                ) { Text("Ramadi") }
            },
            dismissButton = { TextButton(onClick = { showTeacherDialog = false }) { Text("Dhiisi") } }
        )
    }

    if (showStudentDialog) {
        var studentUid by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var grade by remember { mutableStateOf("") }
        var section by remember { mutableStateOf("") }
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
                    enabled = name.trim().length >= 2 &&
                        code.trim().length >= 2 &&
                        grade.trim().isNotEmpty() &&
                        section.trim().isNotEmpty()
                ) { Text("Galmeessi") }
            },
            dismissButton = { TextButton(onClick = { showStudentDialog = false }) { Text("Dhiisi") } }
        )
    }

    if (showRoleGrantDialog) {
        var email by remember { mutableStateOf("") }
        var fullName by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf(UserRole.TEACHER) }
        var grade by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("") }
        var linkedStudentName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRoleGrantDialog = false },
            title = { Text("Gahee Fayyadamaa Kenni") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Google Email") }, singleLine = true)
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Maqaa Guutuu") }, singleLine = true)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        UserRole.entries.forEach { r ->
                            FilterChip(
                                selected = selectedRole == r,
                                onClick = { selectedRole = r },
                                label = { Text(r.labelOromo.substringBefore(" ")) }
                            )
                        }
                    }
                    if (selectedRole == UserRole.TEACHER) {
                        OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Gosa Barumsaa") }, singleLine = true)
                        OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("Kutaa") }, singleLine = true)
                    } else if (selectedRole == UserRole.STUDENT) {
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
                    enabled = email.contains("@") &&
                        fullName.trim().length >= 2 &&
                        (selectedRole != UserRole.TEACHER || (subject.trim().length >= 2 && grade.trim().isNotEmpty())) &&
                        (selectedRole != UserRole.STUDENT || grade.trim().isNotEmpty())
                ) { Text("Kenni") }
            },
            dismissButton = { TextButton(onClick = { showRoleGrantDialog = false }) { Text("Dhiisi") } }
        )
    }
}
