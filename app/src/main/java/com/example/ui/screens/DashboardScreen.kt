package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.AnnouncementCategory
import com.example.data.model.Assignment
import com.example.data.model.AssignmentSubmission
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.EarlyWarning
import com.example.data.model.ExamResult
import com.example.data.model.LearningMaterial
import com.example.data.model.Quiz
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.components.EmptyStateBanner
import com.example.ui.components.KoofkeetFooterQuote
import com.example.ui.components.MetricStatCard
import com.example.ui.components.RoleBadgePill
import com.example.ui.components.SectionHeader

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
    modifier: Modifier = Modifier
) {
    val openWarnings = earlyWarnings.filter { it.status == "OPEN" }
    val avgScore = if (examResults.isEmpty()) 0.0 else examResults.map { it.score }.average()
    val presentPct = if (attendance.isEmpty()) 100.0
    else (attendance.count { it.status == AttendanceStatus.PRESENT.name }.toDouble() / attendance.size) * 100.0

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
                    title = "Cuunfaa Waliigalaa (Dashboard)",
                    subtitle = "Haala hojii barnootaa bakka tokko irraa hordofaa"
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
