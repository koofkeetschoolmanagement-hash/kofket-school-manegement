package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.core.AttendanceDateHelper
import com.example.core.CalendarSyncHelper
import com.example.core.LocalAppLanguage
import com.example.core.MonthlyReportData
import com.example.core.ReportExporter
import com.example.core.stringsFor
import com.example.data.model.Assignment
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceSyncStatus
import com.example.data.model.CalendarEventCategory
import com.example.data.model.ExamResult
import com.example.data.model.SchoolCalendarEvent
import com.example.data.model.SyncPhase
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.components.EmptyStateBanner
import com.example.ui.components.SectionHeader
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AttendanceSyncStatusBar(
    syncStatus: AttendanceSyncStatus,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier,
    buttonTag: String = "attendance_sync_button"
) {
    val strings = stringsFor(LocalAppLanguage.current)
    val timeLabel = remember(syncStatus.lastSyncedMillis) {
        if (syncStatus.lastSyncedMillis > 0L) {
            SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(syncStatus.lastSyncedMillis))
        } else {
            ""
        }
    }
    val isSyncing = syncStatus.phase == SyncPhase.SYNCING
    val isPending = syncStatus.phase == SyncPhase.OFFLINE_PENDING || syncStatus.pendingWriteCount > 0

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = when {
            isPending -> MaterialTheme.colorScheme.secondaryContainer
            syncStatus.isFromCache -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isSyncing -> CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    isPending -> Icon(
                        imageVector = Icons.Filled.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    syncStatus.isFromCache -> Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Filled.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = when {
                            isSyncing -> strings.attendanceSyncSyncing
                            isPending -> "${strings.attendanceSyncPendingPrefix}${syncStatus.pendingWriteCount}"
                            else -> strings.attendanceSyncSynced
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (timeLabel.isNotBlank()) {
                        Text(
                            text = "${strings.attendanceSyncLastLabel}: $timeLabel • ${syncStatus.totalRecords}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onSyncNow,
                enabled = !isSyncing,
                modifier = Modifier
                    .heightIn(min = 42.dp)
                    .testTag(buttonTag)
            ) {
                Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.attendanceSyncButton, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun ExportReportCard(
    availableMonths: List<String>,
    buildMonthlyReport: (String) -> MonthlyReportData,
    examResults: List<ExamResult>,
    attendance: List<AttendanceRecord>,
    onShowStatus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = stringsFor(LocalAppLanguage.current)
    var selectedMonth by remember(availableMonths) {
        mutableStateOf(availableMonths.firstOrNull() ?: "Hunda")
    }
    val reportData = remember(selectedMonth, examResults, attendance) {
        buildMonthlyReport(selectedMonth)
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                ReportExporter.exportPdf(context, uri, reportData).fold(
                    onSuccess = { onShowStatus("${strings.exportSuccessPrefix}PDF") },
                    onFailure = { err -> onShowStatus("${strings.exportFailedPrefix}${err.localizedMessage}") }
                )
            }
        }
    }

    val docxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    ) { uri ->
        if (uri != null) {
            scope.launch {
                ReportExporter.exportDocx(context, uri, reportData).fold(
                    onSuccess = { onShowStatus("${strings.exportSuccessPrefix}Word (.docx)") },
                    onFailure = { err -> onShowStatus("${strings.exportFailedPrefix}${err.localizedMessage}") }
                )
            }
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val filteredExams = examResults.filter {
                    selectedMonth.equals("Hunda", ignoreCase = true) || it.monthLabel.contains(selectedMonth, ignoreCase = true)
                }
                val filteredAtt = attendance.filter {
                    selectedMonth.equals("Hunda", ignoreCase = true) || it.dateLabel.startsWith(selectedMonth, ignoreCase = true)
                }
                ReportExporter.exportCsv(context, uri, reportData, filteredExams, filteredAtt).fold(
                    onSuccess = { onShowStatus("${strings.exportSuccessPrefix}CSV") },
                    onFailure = { err -> onShowStatus("${strings.exportFailedPrefix}${err.localizedMessage}") }
                )
            }
        }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_export_report_card")
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
                    imageVector = Icons.Filled.Download,
                    contentDescription = strings.exportReportTitle,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.exportReportTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.exportReportSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (availableMonths.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableMonths.forEach { m ->
                        FilterChip(
                            selected = selectedMonth == m,
                            onClick = { selectedMonth = m },
                            label = { Text(m) }
                        )
                    }
                }
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${reportData.totalStudents} Students • ${reportData.totalExamsRecorded} Grades",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Avg ${"%.1f".format(reportData.averageExamScore)}% • Att ${"%.0f".format(reportData.attendanceRatePercent)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { pdfLauncher.launch("Koofkeet_Report_${selectedMonth}.pdf") },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("export_report_pdf_button")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.exportPdfButton, maxLines = 1)
                }
                Button(
                    onClick = { docxLauncher.launch("Koofkeet_Report_${selectedMonth}.docx") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("export_report_word_button")
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.exportWordButton, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { csvLauncher.launch("Koofkeet_Report_${selectedMonth}.csv") },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("export_report_csv_button")
                ) {
                    Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.exportCsvButton, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun DashboardCalendarSyncCard(
    profile: UserProfile,
    events: List<SchoolCalendarEvent>,
    assignments: List<Assignment>,
    onCreateEvent: (String, String, String, String, String, CalendarEventCategory, String, String) -> Unit,
    onSyncAcademics: () -> Unit,
    onOpenFullCalendar: () -> Unit,
    onShowStatus: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = stringsFor(LocalAppLanguage.current)
    val canManage = profile.userRole == UserRole.TEACHER || profile.userRole == UserRole.ADMIN
    var showAddDialog by remember { mutableStateOf(false) }

    val icsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/calendar")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                CalendarSyncHelper.exportCalendarIcs(context, uri, events, assignments).fold(
                    onSuccess = { onShowStatus(strings.calendarIcsSuccessToast) },
                    onFailure = { err -> onShowStatus("${strings.exportFailedPrefix}${err.localizedMessage}") }
                )
            }
        }
    }

    val sortedEvents = remember(events) {
        events.sortedBy { it.eventDateLabel }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_calendar_sync_card")
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
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = strings.calendarTitle,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.calendarTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.calendarSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (canManage) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("dashboard_add_calendar_event")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.calendarAddEventButton, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canManage) {
                    OutlinedButton(
                        onClick = onSyncAcademics,
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("calendar_sync_academics_button")
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.calendarSyncAcademicsButton, style = MaterialTheme.typography.labelMedium)
                    }
                }
                OutlinedButton(
                    onClick = { icsLauncher.launch("Kofket_School_Calendar.ics") },
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("calendar_export_ics_button")
                ) {
                    Icon(Icons.Filled.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.calendarExportIcsButton, style = MaterialTheme.typography.labelMedium)
                }
            }

            if (sortedEvents.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(strings.calendarEmptyTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(strings.calendarEmptyMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortedEvents.take(4).forEach { ev ->
                        val cat = CalendarEventCategory.fromRaw(ev.category)
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
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = ev.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${ev.eventDateLabel} • ${ev.startTimeLabel}-${ev.endTimeLabel} • ${cat.labelOromo}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val intent = CalendarSyncHelper.buildDeviceCalendarInsertIntent(ev)
                                        runCatching { context.startActivity(intent) }
                                            .onFailure {
                                                onShowStatus(strings.calendarAddToDeviceButton)
                                            }
                                    },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.EventAvailable,
                                        contentDescription = strings.calendarAddToDeviceButton,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
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
                    onClick = onOpenFullCalendar,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("dashboard_open_calendar_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.calendarOpenFull, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    if (showAddDialog) {
        CreateCalendarEventDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, date, start, end, cat, grade, subj ->
                onCreateEvent(title, desc, date, start, end, cat, grade, subj)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarSyncTab(
    profile: UserProfile,
    canManage: Boolean,
    events: List<SchoolCalendarEvent>,
    assignments: List<Assignment>,
    onCreateEvent: (String, String, String, String, String, CalendarEventCategory, String, String) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onSyncAcademics: () -> Unit,
    onShowStatus: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = stringsFor(LocalAppLanguage.current)
    val appLanguage = LocalAppLanguage.current
    var selectedCategory by remember { mutableStateOf<CalendarEventCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val icsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/calendar")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                CalendarSyncHelper.exportCalendarIcs(context, uri, events, assignments).fold(
                    onSuccess = { onShowStatus(strings.calendarIcsSuccessToast) },
                    onFailure = { err -> onShowStatus("${strings.exportFailedPrefix}${err.localizedMessage}") }
                )
            }
        }
    }

    val filtered = remember(events, selectedCategory) {
        events
            .filter { selectedCategory == null || CalendarEventCategory.fromRaw(it.category) == selectedCategory }
            .sortedBy { it.eventDateLabel }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_sync_tab_list"),
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
                    title = strings.calendarTitle,
                    subtitle = strings.calendarSubtitle,
                    modifier = Modifier.weight(1f)
                )
                if (canManage) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("calendar_tab_add_event_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.calendarAddEventButton)
                    }
                }
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canManage) {
                    OutlinedButton(
                        onClick = onSyncAcademics,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.calendarSyncAcademicsButton)
                    }
                }
                OutlinedButton(
                    onClick = { icsLauncher.launch("Kofket_School_Calendar.ics") },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.calendarExportIcsButton)
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text(strings.attendanceAllStatuses) }
                )
                CalendarEventCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = {
                            Text(if (appLanguage == AppLanguage.ENGLISH) cat.labelEnglish else cat.labelOromo)
                        }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateBanner(
                    icon = Icons.Filled.CalendarMonth,
                    title = strings.calendarEmptyTitle,
                    message = strings.calendarEmptyMessage
                )
            }
        } else {
            items(filtered, key = { it.id }) { ev ->
                val cat = CalendarEventCategory.fromRaw(ev.category)
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${if (appLanguage == AppLanguage.ENGLISH) cat.labelEnglish else cat.labelOromo} • ${ev.gradeLevel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "${ev.eventDateLabel} (${ev.startTimeLabel}-${ev.endTimeLabel})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(ev.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (ev.description.isNotBlank()) {
                            Text(
                                text = ev.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val intent = CalendarSyncHelper.buildDeviceCalendarInsertIntent(ev)
                                    runCatching { context.startActivity(intent) }
                                        .onFailure { onShowStatus(strings.calendarAddToDeviceButton) }
                                },
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Icon(Icons.Filled.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.calendarAddToDeviceButton, style = MaterialTheme.typography.labelMedium)
                            }
                            if (canManage && (profile.userRole == UserRole.ADMIN || ev.createdByUid == profile.userId)) {
                                IconButton(
                                    onClick = { onDeleteEvent(ev.id) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = strings.attendanceDeleteLabel,
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
        CreateCalendarEventDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, date, start, end, cat, grade, subj ->
                onCreateEvent(title, desc, date, start, end, cat, grade, subj)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CreateCalendarEventDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, CalendarEventCategory, String, String) -> Unit
) {
    val strings = stringsFor(LocalAppLanguage.current)
    val appLanguage = LocalAppLanguage.current
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(AttendanceDateHelper.todayDateLabel()) }
    var startTime by remember { mutableStateOf("08:30") }
    var endTime by remember { mutableStateOf("09:30") }
    var category by remember { mutableStateOf(CalendarEventCategory.GENERAL) }
    var grade by remember { mutableStateOf("Hunda") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.calendarAddEventButton) },
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
                    modifier = Modifier.fillMaxWidth().testTag("calendar_dialog_title")
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Guyyaa (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalendarEventCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = {
                                Text(if (appLanguage == AppLanguage.ENGLISH) cat.labelEnglish else cat.labelOromo)
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    label = { Text(strings.gradeLevelLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(strings.gradesSelectSubject) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Ibsa (Description)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(title, description, date, startTime, endTime, category, grade, subject)
                },
                enabled = title.trim().length >= 2 && date.trim().length >= 4,
                modifier = Modifier.testTag("calendar_dialog_save_button")
            ) { Text(strings.attendanceSaveLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancelLabel) }
        }
    )
}
