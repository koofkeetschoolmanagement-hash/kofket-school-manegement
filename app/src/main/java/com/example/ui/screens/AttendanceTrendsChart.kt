package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.AttendanceDateHelper
import com.example.core.LocalAppLanguage
import com.example.core.stringsFor
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.StudentRegistration

data class DailyAttendanceTrend(
    val dateLabel: String,
    val dayNumber: String,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val totalCount: Int,
    val attendanceRate: Double
)

@Composable
fun TeacherAttendanceTrendsChartCard(
    attendance: List<AttendanceRecord>,
    students: List<StudentRegistration>,
    onOpenAttendance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = stringsFor(LocalAppLanguage.current)
    val currentMonth = remember { AttendanceDateHelper.currentMonthLabel() }
    val availableGrades = remember(attendance, students) {
        val fromAtt = attendance.map { it.gradeLevel.trim() }.filter { it.isNotBlank() && !it.equals("Hunda", true) }
        val fromStud = students.map { it.gradeLevel.trim() }.filter { it.isNotBlank() && !it.equals("Hunda", true) }
        listOf("Hunda") + (fromAtt + fromStud).distinct().sorted()
    }
    var selectedGrade by remember { mutableStateOf("Hunda") }

    val monthRecords = remember(attendance, currentMonth, selectedGrade) {
        attendance.filter { rec ->
            val matchesMonth = rec.dateLabel.startsWith(currentMonth)
            val matchesGrade = selectedGrade.equals("Hunda", ignoreCase = true) ||
                rec.gradeLevel.equals(selectedGrade, ignoreCase = true)
            matchesMonth && matchesGrade
        }
    }

    val dailyTrends = remember(monthRecords) {
        val grouped = monthRecords.groupBy { it.dateLabel }
        grouped.entries
            .map { (date, records) ->
                val present = records.count { it.status == AttendanceStatus.PRESENT.name }
                val absent = records.count { it.status == AttendanceStatus.ABSENT.name }
                val late = records.count { it.status == AttendanceStatus.LATE.name }
                val total = records.size
                val rate = if (total > 0) (present.toDouble() / total.toDouble()) * 100.0 else 0.0
                val dayNum = date.takeLast(2).trimStart('0').ifEmpty { date.takeLast(2) }
                DailyAttendanceTrend(
                    dateLabel = date,
                    dayNumber = dayNum,
                    presentCount = present,
                    absentCount = absent,
                    lateCount = late,
                    totalCount = total,
                    attendanceRate = rate
                )
            }
            .sortedBy { it.dateLabel }
    }

    var selectedIndex by remember(dailyTrends) {
        mutableIntStateOf(if (dailyTrends.isNotEmpty()) dailyTrends.lastIndex else -1)
    }

    val avgMonthlyRate = remember(dailyTrends) {
        if (dailyTrends.isEmpty()) 0.0 else dailyTrends.map { it.attendanceRate }.average()
    }
    val peakDay = remember(dailyTrends) {
        dailyTrends.maxByOrNull { it.attendanceRate }
    }
    val lowDay = remember(dailyTrends) {
        dailyTrends.minByOrNull { it.attendanceRate }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("attendance_trends_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
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
                        imageVector = Icons.Filled.ShowChart,
                        contentDescription = strings.attendanceTrendsTitle,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.attendanceTrendsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.attendanceTrendsSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentMonth,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Grade Filter Chips if multiple grades exist
            if (availableGrades.size > 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableGrades.forEach { grade ->
                        FilterChip(
                            selected = selectedGrade == grade,
                            onClick = { selectedGrade = grade },
                            label = {
                                Text(if (grade == "Hunda") strings.attendanceTrendsFilterAllGrades else grade)
                            },
                            modifier = Modifier.testTag("attendance_grade_chip_$grade")
                        )
                    }
                }
            }

            // High-Level KPI Summary Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = strings.attendanceTrendsMonthAverage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${"%.1f".format(avgMonthlyRate)}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("attendance_trend_stat_avg")
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = strings.attendanceTrendsDaysTracked,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${dailyTrends.size} Days",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (peakDay != null) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = strings.attendanceTrendsPeakDay,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${"%.0f".format(peakDay.attendanceRate)}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Interactive Attendance Trend Canvas Chart
            if (dailyTrends.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        AttendanceTrendCanvas(
                            trends = dailyTrends,
                            selectedIndex = selectedIndex,
                            onSelectIndex = { selectedIndex = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .testTag("attendance_trends_chart")
                        )
                    }
                }

                // Selected Day Inspector Card
                val active = if (selectedIndex in dailyTrends.indices) dailyTrends[selectedIndex] else null
                if (active != null) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Guyyaa (Date): ${active.dateLabel}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Total Students Checked: ${active.totalCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text(
                                        text = "${strings.attendancePresent}: ${active.presentCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.errorContainer) {
                                    Text(
                                        text = "${strings.attendanceAbsent}: ${active.absentCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                if (active.lateCount > 0) {
                                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text(
                                            text = "${strings.attendanceLate}: ${active.lateCount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = strings.attendanceTrendsEmptyNotice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bottom Navigation & Full Attendance Roll Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onOpenAttendance,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("attendance_chart_open_full")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.attendanceOpenFullRoll,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceTrendCanvas(
    trends: List<DailyAttendanceTrend>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val errorColor = MaterialTheme.colorScheme.error
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = modifier.pointerInput(trends) {
            detectTapGestures { tapOffset ->
                if (trends.isEmpty()) return@detectTapGestures
                val slotWidth = size.width / trends.size.toFloat()
                val clickedIndex = (tapOffset.x / slotWidth).toInt().coerceIn(0, trends.lastIndex)
                onSelectIndex(clickedIndex)
            }
        }
    ) {
        val w = size.width
        val h = size.height
        val bottomAxisHeight = 22.dp.toPx()
        val chartHeight = h - bottomAxisHeight
        val count = trends.size
        if (count == 0) return@Canvas

        // Draw horizontal grid lines at 25%, 50%, 75%, 100%
        val gridFractions = listOf(0.25f, 0.5f, 0.75f, 1.0f)
        gridFractions.forEach { frac ->
            val y = chartHeight * (1f - frac)
            drawLine(
                color = outlineColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }

        val slotWidth = w / count.toFloat()
        val barWidth = (slotWidth * 0.55f).coerceIn(8.dp.toPx(), 28.dp.toPx())

        val points = mutableListOf<Offset>()

        // 1. Draw Columns/Bars
        trends.forEachIndexed { i, t ->
            val centerX = (i + 0.5f) * slotWidth
            val rateFraction = (t.attendanceRate / 100.0).toFloat().coerceIn(0f, 1f)
            val barHeight = chartHeight * rateFraction
            val topY = chartHeight - barHeight

            val isSelected = i == selectedIndex
            val barColor = when {
                t.attendanceRate >= 80.0 -> primaryColor
                t.attendanceRate >= 60.0 -> secondaryColor
                else -> errorColor
            }

            // Draw bar
            val barTopLeft = Offset(centerX - barWidth / 2f, topY)
            val barSize = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx()))

            val brush = Brush.verticalGradient(
                colors = listOf(
                    barColor.copy(alpha = if (isSelected) 1f else 0.85f),
                    barColor.copy(alpha = if (isSelected) 0.65f else 0.35f)
                ),
                startY = topY,
                endY = chartHeight
            )

            drawRoundRect(
                brush = brush,
                topLeft = barTopLeft,
                size = barSize,
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            if (isSelected) {
                drawRoundRect(
                    color = primaryColor,
                    topLeft = barTopLeft,
                    size = barSize,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            points.add(Offset(centerX, topY))
        }

        // 2. Draw Trend Line connecting points
        if (points.size > 1) {
            val path = Path()
            path.moveTo(points.first().x, points.first().y)
            for (j in 1 until points.size) {
                val prev = points[j - 1]
                val curr = points[j]
                val midX = (prev.x + curr.x) / 2f
                path.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        // 3. Draw Dots on top of bars
        points.forEachIndexed { idx, pt ->
            val isSelected = idx == selectedIndex
            val dotRadius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx()
            drawCircle(
                color = Color.White,
                radius = dotRadius + 1.5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = if (isSelected) secondaryColor else primaryColor,
                radius = dotRadius,
                center = pt
            )
        }
    }
}
