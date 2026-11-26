package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.LocalAppLanguage
import com.example.core.stringsFor
import com.example.ui.components.SectionHeader

@Composable
fun UserManualScreen(
    modifier: Modifier = Modifier
) {
    val currentLang = LocalAppLanguage.current
    val strings = stringsFor(currentLang)

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
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_manual_header_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = "Maanuwaalii Manajimantii Mana Barumsaa Koofkeet",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Professional User Manual & System Architecture Guide",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Qopheessaan Fayyeeraa Taaddasaa",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            item {
                ManualSectionCard(
                    title = "1. Caasaa Faayilootaa fi Architecture (Files & Architecture)",
                    icon = Icons.Filled.Description,
                    content = "• Core Models & Data: SchoolModels.kt, AppLocalization.kt (Afaan Oromoo, English, Amharic translations).\n" +
                            "• Repositories: SchoolRepository.kt, CommunicationAndRecordsRepository.kt (Firestore Cloud & offline caching).\n" +
                            "• ViewModel & State: SchoolViewModel.kt manages reactive flows for students, grades, attendance, AI chats, and calendar events.\n" +
                            "• Screens & UI (Jetpack Compose): DashboardScreen.kt, AcademicsScreen.kt, MarklistScreen.kt, AttendanceScreen.kt, AdminManagementScreen.kt, AiTutorScreen.kt.\n" +
                            "• Export & Sync Utilities: ReportExporter.kt (PDF, Word, CSV exports), CalendarSyncHelper.kt (.ics & device calendar sync)."
                )
            }

            item {
                ManualSectionCard(
                    title = "2. Bulchiinsa fi Galmee (Admin & User Role Management)",
                    icon = Icons.Filled.Security,
                    content = "• Google Sign-In & Auth: Secure authentication via Jetpack Credential Manager linked to Firestore user profiles.\n" +
                            "• Role Distinction: Automatically distinguishes between Admin, Teacher, Student, and Parent accounts.\n" +
                            "• Navigation Guard: Checks user role from Firestore upon login and redirects to the appropriate dashboard securely.\n" +
                            "• Profile Setup: Triggers after first login to configure display name, grade level, and language preference (Oromoo, English, Amharic).\n" +
                            "• Admin Management Screen: Allows admins to issue role grants, register students, assign subjects to teachers, and export monthly reports."
                )
            }

            item {
                ManualSectionCard(
                    title = "3. To'annoo Argamaa fi Sync (Attendance & Cloud Sync)",
                    icon = Icons.Filled.CheckCircle,
                    content = "• Daily Attendance Tracking: Teachers can mark students as Present, Absent, or Late daily with deterministic Firestore doc IDs.\n" +
                            "• Batch Actions: One-tap 'Mark All Present' for fast roll-calling.\n" +
                            "• Attendance Sync Status Bar: Tracks cloud connectivity in real time (Synced, Offline Pending writes count, or Syncing phase).\n" +
                            "• Manual Server Sync: Teachers and admins can trigger manual syncAttendanceWithServer anytime."
                )
            }

            item {
                ManualSectionCard(
                    title = "4. Qabxii fi Curriculum (Grades & Curriculum)",
                    icon = Icons.Filled.School,
                    content = "• Official Curriculum Subjects: Exact Grade 1-6 subjects (Afaan Oromoo, English, Afaan Amaaraa, Saayinsii Naannoo, Herrega, Gadaafi Safuu, Og-aartii, FJQ) and Grade 7-8 subjects.\n" +
                            "• Teacher Grade Recording: Teachers record assessment scores and remarks into Firestore.\n" +
                            "• Marklist Table View: Comprehensive student grades list view with automatic averages and filtering.\n" +
                            "• Assignments & Quizzes: Interactive homework creation and student submission tracking."
                )
            }

            item {
                ManualSectionCard(
                    title = "5. AI Tutor • Gargaarsa Barattootaa (Private AI Tutor)",
                    icon = Icons.Filled.AutoAwesome,
                    content = "• Firebase AI Logic Integration: Powered by Firebase AI Logic (Firebase.ai) for fast, secure cloud AI responses.\n" +
                            "• Private Student Support: All AI interactions are securely stored in private student subcollections (users/{uid}/ai_chats).\n" +
                            "• Study Support Modes: Supports multiple modes including Explain Concept, Practice Quiz, and Summarize Notes.\n" +
                            "• Multilingual & Grade-Aware: Tailors responses to the student's grade level and preferred language."
                    )
            }

            item {
                ManualSectionCard(
                    title = "6. Kalandarii fi Gabaasa Baasuu (Calendar & Export)",
                    icon = Icons.Filled.CalendarMonth,
                    content = "• Multi-Format Report Export: Export monthly executive and student performance reports as PDF, Word (.docx), or CSV files.\n" +
                            "• School Calendar Events: Firestore collection /school_events/{eventId} for school-wide calendar coordination.\n" +
                            "• Academic Sync: One-tap synchronization of assignments and quizzes to the school calendar.\n" +
                            "• Device Calendar & .ics Export: Zero-permission insertion into Android system calendar and RFC 5545 .ics file export."
                )
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Goolaba & Galata (Conclusion & Attribution)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• Enterprise-Grade Architecture: Built with Kotlin, Jetpack Compose M3, Firestore, and Firebase AI Logic.\n" +
                                    "• Fully Localized: Complete Afaan Oromoo, English, and Amharic support.\n" +
                                    "• Secure & Reliable: Strict security rules, offline persistence, and zero-permission report/calendar exports.\n" +
                                    "• Qopheessaan (Author): Qopheessaan Fayyeeraa Taaddasaa\n" +
                                    "• Thank you for using Koofkeet School App Management System!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualSectionCard(
    title: String,
    icon: ImageVector,
    content: String
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
