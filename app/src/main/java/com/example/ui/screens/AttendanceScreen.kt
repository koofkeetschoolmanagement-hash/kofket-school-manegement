package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.core.AttendanceDateHelper
import com.example.core.ReportExporter
import kotlinx.coroutines.launch
import com.example.core.LocalAppLanguage
import com.example.core.LocalizedLoginStrings
import com.example.core.stringsFor
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceSyncStatus
import com.example.data.model.StudentRegistration
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.components.EmptyStateBanner
import com.example.ui.components.PrivacyIsolationNotice
import com.example.ui.components.SectionHeader

private enum class AttendanceViewMode {
    ROLL_CALL,
    HISTORY
}

private data class RosterRowItem(
    val studentId: String,
    val studentName: String,
    val parentId: String,
    val gradeLevel: String,
    val section: String
)

/**
 * Multi-token smart search across student name, grade, date, note, and localized attendance status.
 */
fun matchesAttendanceSmartQuery(
    query: String,
    studentName: String,
    gradeLevel: String,
    statusRaw: String,
    dateLabel: String = "",
    note: String = ""
): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return true
    val tokens = trimmed.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
    val normStatus = statusRaw.trim().uppercase()
    return tokens.all { token ->
        studentName.lowercase().contains(token) ||
            gradeLevel.lowercase().contains(token) ||
            dateLabel.lowercase().contains(token) ||
            note.lowercase().contains(token) ||
            normStatus.lowercase().contains(token) ||
            (token in listOf("present", "argamaa", "argame", "ተገኝቷል", "jira", "qooddate") && normStatus == "PRESENT") ||
            (token in listOf("absent", "dhibame", "hafe", "ቀሪ", "bade", "hin jirre") && normStatus == "ABSENT") ||
            (token in listOf("late", "turte", "ture", "ዘግይቷል", "barfate") && normStatus == "LATE") ||
            (token in listOf("unmarked", "hin galmoofne", "galmee hin qabne", "ያልተመዘገበ", "haaraa", "empty") && normStatus == "UNMARKED")
    }
}

@Composable
fun AttendanceTab(
    profile: UserProfile,
    canManage: Boolean,
    students: List<StudentRegistration>,
    attendance: List<AttendanceRecord>,
    onRecordAttendance: (String, String, String, String, String, AttendanceStatus, String) -> Unit,
    onMarkAllPresent: (String, List<StudentRegistration>, List<AttendanceRecord>) -> Unit = { _, _, _ -> },
    onDeleteAttendance: (String) -> Unit = {},
    syncStatus: AttendanceSyncStatus = AttendanceSyncStatus(),
    onSyncAttendance: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = stringsFor(LocalAppLanguage.current)
    var viewMode by remember(canManage) {
        mutableStateOf(if (canManage) AttendanceViewMode.ROLL_CALL else AttendanceViewMode.HISTORY)
    }
    var selectedDate by remember { mutableStateOf(AttendanceDateHelper.todayDateLabel()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableStateOf<String?>(null) }
    var rollCallStatusFilter by remember { mutableStateOf<String?>(null) } // null = All, "PRESENT", "ABSENT", "LATE", "UNMARKED"
    var historyStatusFilter by remember { mutableStateOf<AttendanceStatus?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var noteDialogTarget by remember { mutableStateOf<Pair<RosterRowItem, AttendanceRecord?>?>(null) }

    val mergedRoster = remember(students, attendance) {
        val byId = linkedMapOf<String, RosterRowItem>()
        students.forEach { s ->
            val key = s.studentUid.ifBlank { s.studentName }
            if (key.isNotBlank() && s.studentName.isNotBlank()) {
                byId[key] = RosterRowItem(
                    studentId = s.studentUid.ifBlank { key },
                    studentName = s.studentName,
                    parentId = s.parentUid,
                    gradeLevel = s.gradeLevel.ifBlank { "Hunda" },
                    section = s.section
                )
            }
        }
        attendance.forEach { rec ->
            val key = rec.studentId.ifBlank { rec.studentName }
            if (key.isNotBlank() && key !in byId && rec.studentName.isNotBlank()) {
                byId[key] = RosterRowItem(
                    studentId = rec.studentId.ifBlank { key },
                    studentName = rec.studentName,
                    parentId = rec.parentId,
                    gradeLevel = rec.gradeLevel.ifBlank { "Hunda" },
                    section = ""
                )
            }
        }
        byId.values.sortedBy { it.studentName.lowercase() }
    }

    val grades = remember(mergedRoster) {
        mergedRoster.map { it.gradeLevel }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val recordsForSelectedDate = remember(attendance, selectedDate) {
        attendance.filter { it.dateLabel == selectedDate }
    }

    val recordByStudentForDate = remember(recordsForSelectedDate) {
        recordsForSelectedDate.associateBy { it.studentId.ifBlank { it.studentName } }
    }

    val filteredRoster = remember(mergedRoster, selectedGrade, rollCallStatusFilter, searchQuery, recordByStudentForDate) {
        val q = searchQuery.trim()
        mergedRoster.filter { row ->
            val record = recordByStudentForDate[row.studentId]
            val currentStatus = record?.status ?: "UNMARKED"
            val matchesGrade = (selectedGrade == null || row.gradeLevel == selectedGrade)
            val matchesStatusFilter = when (rollCallStatusFilter) {
                null -> true
                "UNMARKED" -> record == null
                else -> currentStatus == rollCallStatusFilter
            }
            val matchesQuery = matchesAttendanceSmartQuery(
                query = q,
                studentName = row.studentName,
                gradeLevel = row.gradeLevel,
                statusRaw = currentStatus,
                dateLabel = selectedDate,
                note = record?.note.orEmpty()
            )
            matchesGrade && matchesStatusFilter && matchesQuery
        }
    }

    val sortedHistory = remember(attendance, historyStatusFilter, searchQuery) {
        val q = searchQuery.trim()
        attendance
            .filter { rec ->
                val matchesStatus = (historyStatusFilter == null || AttendanceStatus.fromRaw(rec.status) == historyStatusFilter)
                val matchesQuery = matchesAttendanceSmartQuery(
                    query = q,
                    studentName = rec.studentName,
                    gradeLevel = rec.gradeLevel,
                    statusRaw = rec.status,
                    dateLabel = rec.dateLabel,
                    note = rec.note
                )
                matchesStatus && matchesQuery
            }
            .sortedWith(
                compareByDescending<AttendanceRecord> { it.dateLabel }
                    .thenByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                    .thenBy { it.studentName.lowercase() }
            )
    }

    val rollCallRecordsToExport = remember(filteredRoster, recordByStudentForDate, selectedDate) {
        filteredRoster.map { row ->
            recordByStudentForDate[row.studentId] ?: AttendanceRecord(
                id = "roll_${row.studentId}_$selectedDate",
                studentId = row.studentId,
                studentName = row.studentName,
                parentId = row.parentId,
                gradeLevel = row.gradeLevel,
                dateLabel = selectedDate,
                status = "UNMARKED",
                note = ""
            )
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val records = if (viewMode == AttendanceViewMode.ROLL_CALL) rollCallRecordsToExport else sortedHistory
                val title = if (viewMode == AttendanceViewMode.ROLL_CALL) {
                    "Koofkeet School Attendance Roll Call - $selectedDate"
                } else {
                    "Koofkeet School Attendance History (${records.size} Records)"
                }
                val res = ReportExporter.exportAttendanceCsv(context, uri, records, title)
                if (res.isSuccess) {
                    Toast.makeText(context, strings.attendanceExportSuccessToast, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(
                        context,
                        "${strings.exportFailedPrefix}: ${res.exceptionOrNull()?.message.orEmpty()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val records = if (viewMode == AttendanceViewMode.ROLL_CALL) rollCallRecordsToExport else sortedHistory
                val title = if (viewMode == AttendanceViewMode.ROLL_CALL) {
                    "KOOFKEET SCHOOL - ARGAMAA ($selectedDate)"
                } else {
                    "KOOFKEET SCHOOL - ATTENDANCE HISTORY"
                }
                val res = ReportExporter.exportAttendancePdf(context, uri, records, title)
                if (res.isSuccess) {
                    Toast.makeText(context, strings.attendanceExportSuccessToast, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(
                        context,
                        "${strings.exportFailedPrefix}: ${res.exceptionOrNull()?.message.orEmpty()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = strings.attendanceTitle,
                subtitle = strings.attendanceSubtitle
            )
        }

        item {
            PrivacyIsolationNotice(
                if (canManage) strings.attendancePrivacyTeacherNote else strings.attendancePrivacyStudentNote
            )
        }

        item {
            AttendanceSyncStatusBar(
                syncStatus = syncStatus,
                onSyncNow = onSyncAttendance,
                buttonTag = "attendance_tab_sync_button"
            )
        }

        if (canManage) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = viewMode == AttendanceViewMode.ROLL_CALL,
                        onClick = { viewMode = AttendanceViewMode.ROLL_CALL },
                        leadingIcon = { Icon(Icons.Filled.HowToReg, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text(strings.attendanceRollCallTab) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("attendance_mode_roll_call")
                    )
                    FilterChip(
                        selected = viewMode == AttendanceViewMode.HISTORY,
                        onClick = { viewMode = AttendanceViewMode.HISTORY },
                        leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text("${strings.attendanceHistoryTab} (${attendance.size})") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("attendance_mode_history")
                    )
                }
            }
        }

        if (viewMode == AttendanceViewMode.ROLL_CALL && canManage) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            IconButton(
                                onClick = { selectedDate = AttendanceDateHelper.shiftDateLabel(selectedDate, -1) },
                                modifier = Modifier.size(48.dp).testTag("attendance_prev_day")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = selectedDate,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (selectedDate != AttendanceDateHelper.todayDateLabel()) {
                                    TextButton(
                                        onClick = { selectedDate = AttendanceDateHelper.todayDateLabel() },
                                        modifier = Modifier.testTag("attendance_today_button")
                                    ) {
                                        Text(strings.attendanceToday)
                                    }
                                }
                            }

                            IconButton(
                                onClick = { selectedDate = AttendanceDateHelper.shiftDateLabel(selectedDate, 1) },
                                modifier = Modifier.size(48.dp).testTag("attendance_next_day")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                            }
                        }

                        val presentToday = recordsForSelectedDate.count { it.status == AttendanceStatus.PRESENT.name }
                        val absentToday = recordsForSelectedDate.count { it.status == AttendanceStatus.ABSENT.name }
                        val lateToday = recordsForSelectedDate.count { it.status == AttendanceStatus.LATE.name }
                        val unmarkedToday = (mergedRoster.size - recordsForSelectedDate.size).coerceAtLeast(0)

                        AttendanceCountsStrip(
                            presentCount = presentToday,
                            absentCount = absentToday,
                            lateCount = lateToday,
                            unmarkedCount = unmarkedToday,
                            strings = strings
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val batchTargets = filteredRoster.map { row ->
                                        StudentRegistration(
                                            studentUid = row.studentId,
                                            studentName = row.studentName,
                                            gradeLevel = row.gradeLevel,
                                            section = row.section,
                                            parentUid = row.parentId
                                        )
                                    }
                                    onMarkAllPresent(selectedDate, batchTargets, recordsForSelectedDate)
                                },
                                enabled = filteredRoster.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("mark_all_present_button")
                            ) {
                                Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.attendanceMarkAllPresent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { showAddDialog = true },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("record_attendance_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.attendanceAddManual, maxLines = 1)
                            }
                        }
                    }
                }
            }

            if (grades.size > 1) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedGrade == null,
                            onClick = { selectedGrade = null },
                            label = { Text(strings.attendanceAllGrades) }
                        )
                        grades.forEach { gr ->
                            FilterChip(
                                selected = selectedGrade == gr,
                                onClick = { selectedGrade = if (selectedGrade == gr) null else gr },
                                label = { Text(gr) }
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("attendance_clear_search_button")
                                    ) {
                                        Icon(
                                            Icons.Filled.Clear,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            placeholder = {
                                Text(
                                    text = strings.attendanceSmartSearchHint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("attendance_search_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = rollCallStatusFilter == null,
                                onClick = { rollCallStatusFilter = null },
                                label = { Text(strings.attendanceAllStatuses) },
                                modifier = Modifier.testTag("filter_status_all")
                            )
                            FilterChip(
                                selected = rollCallStatusFilter == AttendanceStatus.PRESENT.name,
                                onClick = {
                                    rollCallStatusFilter = if (rollCallStatusFilter == AttendanceStatus.PRESENT.name) null else AttendanceStatus.PRESENT.name
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text(strings.attendancePresent) },
                                modifier = Modifier.testTag("filter_status_present")
                            )
                            FilterChip(
                                selected = rollCallStatusFilter == AttendanceStatus.ABSENT.name,
                                onClick = {
                                    rollCallStatusFilter = if (rollCallStatusFilter == AttendanceStatus.ABSENT.name) null else AttendanceStatus.ABSENT.name
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text(strings.attendanceAbsent) },
                                modifier = Modifier.testTag("filter_status_absent")
                            )
                            FilterChip(
                                selected = rollCallStatusFilter == AttendanceStatus.LATE.name,
                                onClick = {
                                    rollCallStatusFilter = if (rollCallStatusFilter == AttendanceStatus.LATE.name) null else AttendanceStatus.LATE.name
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text(strings.attendanceLate) },
                                modifier = Modifier.testTag("filter_status_late")
                            )
                            FilterChip(
                                selected = rollCallStatusFilter == "UNMARKED",
                                onClick = {
                                    rollCallStatusFilter = if (rollCallStatusFilter == "UNMARKED") null else "UNMARKED"
                                },
                                label = { Text(strings.attendanceFilterUnmarked) },
                                modifier = Modifier.testTag("filter_status_unmarked")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "${strings.attendanceResultsFoundPrefix}${filteredRoster.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val filename = "koofkeet_attendance_${selectedDate.replace("-", "_")}.csv"
                                        csvLauncher.launch(filename)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .heightIn(min = 40.dp)
                                        .testTag("attendance_export_csv_button")
                                ) {
                                    Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.attendanceExportCsvButton, style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = {
                                        val filename = "koofkeet_attendance_${selectedDate.replace("-", "_")}.pdf"
                                        pdfLauncher.launch(filename)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .heightIn(min = 40.dp)
                                        .testTag("attendance_export_pdf_button")
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.attendanceExportPdfButton, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            if (filteredRoster.isEmpty()) {
                item {
                    EmptyStateBanner(
                        icon = Icons.Filled.HowToReg,
                        title = strings.attendanceEmptyRosterTitle,
                        message = strings.attendanceEmptyRosterMessage
                    )
                }
            } else {
                items(filteredRoster, key = { it.studentId }) { row ->
                    val existing = recordByStudentForDate[row.studentId]
                    val currentStatus = existing?.let { AttendanceStatus.fromRaw(it.status) }
                    DailyStudentRollCard(
                        student = row,
                        currentStatus = currentStatus,
                        note = existing?.note.orEmpty(),
                        strings = strings,
                        onMarkStatus = { newStatus ->
                            onRecordAttendance(
                                row.studentId,
                                row.studentName,
                                row.parentId,
                                row.gradeLevel,
                                selectedDate,
                                newStatus,
                                existing?.note.orEmpty()
                            )
                        },
                        onEditNote = { noteDialogTarget = row to existing }
                    )
                }
            }
        } else {
            // Attendance History mode (for Teachers/Admins switching to History, and always for Students/Parents)
            item {
                val totalCount = attendance.size
                val presentCount = attendance.count { it.status == AttendanceStatus.PRESENT.name }
                val absentCount = attendance.count { it.status == AttendanceStatus.ABSENT.name }
                val lateCount = attendance.count { it.status == AttendanceStatus.LATE.name }
                val rate = if (totalCount == 0) 100.0 else (presentCount.toDouble() / totalCount) * 100.0

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            Text(
                                text = strings.attendanceRateLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (totalCount == 0) "—" else "${"%.1f".format(rate)}%",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        AttendanceCountsStrip(
                            presentCount = presentCount,
                            absentCount = absentCount,
                            lateCount = lateCount,
                            unmarkedCount = null,
                            strings = strings
                        )
                    }
                }
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("attendance_history_clear_search_button")
                                    ) {
                                        Icon(
                                            Icons.Filled.Clear,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            placeholder = {
                                Text(
                                    text = strings.attendanceSmartSearchHint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("attendance_history_search_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = historyStatusFilter == null,
                                onClick = { historyStatusFilter = null },
                                label = { Text(strings.attendanceAllStatuses) },
                                modifier = Modifier.testTag("filter_history_status_all")
                            )
                            AttendanceStatus.entries.forEach { st ->
                                FilterChip(
                                    selected = historyStatusFilter == st,
                                    onClick = { historyStatusFilter = if (historyStatusFilter == st) null else st },
                                    label = { Text(localizedAttendanceStatus(st, strings)) },
                                    modifier = Modifier.testTag("filter_history_status_${st.name.lowercase()}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "${strings.attendanceResultsFoundPrefix}${sortedHistory.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val filename = "koofkeet_attendance_history_${AttendanceDateHelper.todayDateLabel().replace("-", "_")}.csv"
                                        csvLauncher.launch(filename)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .heightIn(min = 40.dp)
                                        .testTag("attendance_history_export_csv_button")
                                ) {
                                    Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.attendanceExportCsvButton, style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = {
                                        val filename = "koofkeet_attendance_history_${AttendanceDateHelper.todayDateLabel().replace("-", "_")}.pdf"
                                        pdfLauncher.launch(filename)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .heightIn(min = 40.dp)
                                        .testTag("attendance_history_export_pdf_button")
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.attendanceExportPdfButton, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            if (sortedHistory.isEmpty()) {
                item {
                    EmptyStateBanner(
                        icon = Icons.Filled.CheckCircle,
                        title = strings.attendanceNoHistoryTitle,
                        message = strings.attendanceNoHistoryMessage
                    )
                }
            } else {
                items(sortedHistory, key = { it.id }) { rec ->
                    val status = AttendanceStatus.fromRaw(rec.status)
                    val canModifyThis = canManage &&
                        (profile.userRole == UserRole.ADMIN || rec.teacherId == profile.userId)
                    AttendanceHistoryCard(
                        record = rec,
                        status = status,
                        strings = strings,
                        canModify = canModifyThis,
                        onUpdateStatus = { newStatus ->
                            onRecordAttendance(
                                rec.studentId,
                                rec.studentName,
                                rec.parentId,
                                rec.gradeLevel,
                                rec.dateLabel,
                                newStatus,
                                rec.note
                            )
                        },
                        onDelete = { onDeleteAttendance(rec.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ManualAttendanceDialog(
            students = students,
            defaultDate = selectedDate,
            strings = strings,
            onDismiss = { showAddDialog = false },
            onSave = { sid, sname, pid, gr, dt, st, note ->
                onRecordAttendance(sid, sname, pid, gr, dt, st, note)
                selectedDate = dt.trim()
                showAddDialog = false
            }
        )
    }

    noteDialogTarget?.let { (student, existing) ->
        var noteText by remember(student.studentId, selectedDate) { mutableStateOf(existing?.note.orEmpty()) }
        var status by remember(student.studentId, selectedDate) {
            mutableStateOf(existing?.let { AttendanceStatus.fromRaw(it.status) } ?: AttendanceStatus.PRESENT)
        }
        AlertDialog(
            onDismissRequest = { noteDialogTarget = null },
            title = { Text("${strings.attendanceNoteDialogTitle}: ${student.studentName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$selectedDate • ${student.gradeLevel}", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AttendanceStatus.entries.forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(localizedAttendanceStatus(st, strings)) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text(strings.attendanceNoteLabel) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onRecordAttendance(
                        student.studentId,
                        student.studentName,
                        student.parentId,
                        student.gradeLevel,
                        selectedDate,
                        status,
                        noteText
                    )
                    noteDialogTarget = null
                }) { Text(strings.attendanceSaveLabel) }
            },
            dismissButton = {
                TextButton(onClick = { noteDialogTarget = null }) { Text(strings.cancelLabel) }
            }
        )
    }
}

@Composable
private fun AttendanceCountsStrip(
    presentCount: Int,
    absentCount: Int,
    lateCount: Int,
    unmarkedCount: Int?,
    strings: LocalizedLoginStrings
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CountBadge(
            label = strings.attendancePresent,
            count = presentCount,
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer
        )
        CountBadge(
            label = strings.attendanceAbsent,
            count = absentCount,
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer
        )
        CountBadge(
            label = strings.attendanceLate,
            count = lateCount,
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer
        )
        if (unmarkedCount != null) {
            CountBadge(
                label = strings.attendanceUnmarked,
                count = unmarkedCount,
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CountBadge(
    label: String,
    count: Int,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color
) {
    Surface(shape = MaterialTheme.shapes.small, color = container) {
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun DailyStudentRollCard(
    student: RosterRowItem,
    currentStatus: AttendanceStatus?,
    note: String,
    strings: LocalizedLoginStrings,
    onMarkStatus: (AttendanceStatus) -> Unit,
    onEditNote: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("roll_card_${student.studentId}")
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
                        text = student.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val subtitle = if (student.section.isNotBlank()) {
                        "${student.gradeLevel} • ${student.section}"
                    } else {
                        student.gradeLevel
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (currentStatus) {
                            AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primaryContainer
                            AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.errorContainer
                            AttendanceStatus.LATE -> MaterialTheme.colorScheme.secondaryContainer
                            null -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = currentStatus?.let { localizedAttendanceStatus(it, strings) } ?: strings.attendanceUnmarked,
                            style = MaterialTheme.typography.labelMedium,
                            color = when (currentStatus) {
                                AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.onPrimaryContainer
                                AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.onErrorContainer
                                AttendanceStatus.LATE -> MaterialTheme.colorScheme.onSecondaryContainer
                                null -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    IconButton(
                        onClick = onEditNote,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EditNote,
                            contentDescription = strings.attendanceNoteDialogTitle,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (note.isNotBlank()) {
                Text(
                    text = "${strings.attendanceNoteLabel}: $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttendanceStatusButton(
                    label = strings.attendancePresent,
                    icon = Icons.Filled.CheckCircle,
                    selected = currentStatus == AttendanceStatus.PRESENT,
                    selectedContainer = MaterialTheme.colorScheme.primary,
                    selectedContent = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_present_${student.studentId}"),
                    onClick = { onMarkStatus(AttendanceStatus.PRESENT) }
                )
                AttendanceStatusButton(
                    label = strings.attendanceAbsent,
                    icon = Icons.Filled.Cancel,
                    selected = currentStatus == AttendanceStatus.ABSENT,
                    selectedContainer = MaterialTheme.colorScheme.error,
                    selectedContent = MaterialTheme.colorScheme.onError,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_absent_${student.studentId}"),
                    onClick = { onMarkStatus(AttendanceStatus.ABSENT) }
                )
                AttendanceStatusButton(
                    label = strings.attendanceLate,
                    icon = Icons.Filled.Schedule,
                    selected = currentStatus == AttendanceStatus.LATE,
                    selectedContainer = MaterialTheme.colorScheme.secondary,
                    selectedContent = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_late_${student.studentId}"),
                    onClick = { onMarkStatus(AttendanceStatus.LATE) }
                )
            }
        }
    }
}

@Composable
private fun AttendanceStatusButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    selectedContainer: androidx.compose.ui.graphics.Color,
    selectedContent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = selectedContainer,
                contentColor = selectedContent
            ),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = modifier.heightIn(min = 48.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = modifier.heightIn(min = 48.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AttendanceHistoryCard(
    record: AttendanceRecord,
    status: AttendanceStatus,
    strings: LocalizedLoginStrings,
    canModify: Boolean,
    onUpdateStatus: (AttendanceStatus) -> Unit,
    onDelete: () -> Unit
) {
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
                        text = "${record.studentName} (${record.gradeLevel})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = record.dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (status) {
                            AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primaryContainer
                            AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.errorContainer
                            AttendanceStatus.LATE -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Text(
                            text = localizedAttendanceStatus(status, strings),
                            style = MaterialTheme.typography.labelMedium,
                            color = when (status) {
                                AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.onPrimaryContainer
                                AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.onErrorContainer
                                AttendanceStatus.LATE -> MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    if (canModify) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = strings.attendanceDeleteLabel,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            if (record.note.isNotBlank()) {
                Text(
                    text = record.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (canModify) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AttendanceStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { if (status != st) onUpdateStatus(st) },
                            label = { Text(localizedAttendanceStatus(st, strings)) },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualAttendanceDialog(
    students: List<StudentRegistration>,
    defaultDate: String,
    strings: LocalizedLoginStrings,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, AttendanceStatus, String) -> Unit
) {
    val defaultStudent = students.firstOrNull()
    var studentId by remember { mutableStateOf(defaultStudent?.studentUid.orEmpty()) }
    var studentName by remember { mutableStateOf(defaultStudent?.studentName.orEmpty()) }
    var parentId by remember { mutableStateOf(defaultStudent?.parentUid.orEmpty()) }
    var grade by remember { mutableStateOf(defaultStudent?.gradeLevel.orEmpty()) }
    var dateLabel by remember { mutableStateOf(defaultDate) }
    var status by remember { mutableStateOf(AttendanceStatus.PRESENT) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.attendanceTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (students.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        students.take(6).forEach { s ->
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
                }
                OutlinedTextField(
                    value = studentName,
                    onValueChange = {
                        studentName = it
                        if (students.none { s -> s.studentName == it }) studentId = ""
                    },
                    label = { Text(strings.fullNameLabel) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    label = { Text(strings.gradeLevelLabel) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = dateLabel,
                    onValueChange = { dateLabel = it },
                    label = { Text("YYYY-MM-DD") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AttendanceStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(localizedAttendanceStatus(st, strings)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.attendanceNoteLabel) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(studentId, studentName, parentId, grade, dateLabel, status, note) },
                enabled = studentName.trim().length >= 2 && grade.trim().isNotEmpty() && dateLabel.trim().length >= 4
            ) { Text(strings.attendanceSaveLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancelLabel) } }
    )
}

fun localizedAttendanceStatus(status: AttendanceStatus, strings: LocalizedLoginStrings): String = when (status) {
    AttendanceStatus.PRESENT -> strings.attendancePresent
    AttendanceStatus.ABSENT -> strings.attendanceAbsent
    AttendanceStatus.LATE -> strings.attendanceLate
}
