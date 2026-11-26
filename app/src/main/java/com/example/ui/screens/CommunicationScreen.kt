package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Announcement
import com.example.data.model.AnnouncementCategory
import com.example.data.model.EarlyWarning
import com.example.data.model.ParentMessage
import com.example.data.model.TeacherAssignment
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.components.EmptyStateBanner
import com.example.ui.components.PrivacyIsolationNotice
import com.example.ui.components.SectionHeader

@Composable
fun CommunicationScreen(
    profile: UserProfile,
    announcements: List<Announcement>,
    parentMessages: List<ParentMessage>,
    earlyWarnings: List<EarlyWarning>,
    teachers: List<TeacherAssignment>,
    onPostAnnouncement: (String, String, AnnouncementCategory) -> Unit,
    onSendParentMessage: (String, String, String, String, String, String, String, String) -> Unit,
    onResolveWarning: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val canSeeParentChat = profile.userRole != UserRole.STUDENT
    val canSeeEarlyWarnings = profile.userRole == UserRole.TEACHER || profile.userRole == UserRole.ADMIN
    val canPostAnnouncements = profile.userRole == UserRole.TEACHER || profile.userRole == UserRole.ADMIN

    var showAnnDialog by remember { mutableStateOf(false) }
    var showMsgDialog by remember { mutableStateOf(false) }
    var replyTarget by remember { mutableStateOf<ParentMessage?>(null) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (canSeeEarlyWarnings) {
                item {
                    SectionHeader(
                        title = "Deeggarsa Dursa-Beeksisaa (Early Warning)",
                        subtitle = "Barattoota gahumsi isaanii gad bu'e dursanii gargaaruuf"
                    )
                }
                if (earlyWarnings.isEmpty()) {
                    item {
                        EmptyStateBanner(
                            icon = Icons.Filled.Warning,
                            title = "Akeekkachiisni banaa ta'e hin jiru",
                            message = "Yeroo qabxiin barataa gad bu'u sirni kun ofumaan akeekkachiisa erga."
                        )
                    }
                } else {
                    items(earlyWarnings, key = { it.id }) { warn ->
                        val isOpen = warn.status == "OPEN"
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOpen) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${warn.studentName} • ${warn.subject} (${warn.gradeLevel})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(warn.status, style = MaterialTheme.typography.labelMedium)
                                }
                                Text(warn.reason, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "Giddu-galeessa duraanii: ${"%.1f".format(warn.previousAverage)}% → Amma: ${"%.1f".format(warn.currentScore)}%",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                if (isOpen) {
                                    OutlinedButton(onClick = { onResolveWarning(warn.id) }) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Furameera (Resolve)")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (canSeeParentChat) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(
                            title = "Qunnamtii Maatii, Barsiisaa & Bulchiinsaa",
                            subtitle = "Haala barnootaa ijoollee iccitiin hordofaa",
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { showMsgDialog = true },
                            modifier = Modifier.testTag("new_parent_message_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ergi")
                        }
                    }
                }
                item {
                    PrivacyIsolationNotice("Barataan qunnamtii maatii, barsiisaa fi bulchiinsaa arguu hin danda'u (Eegumsa Odeeffannoo).")
                }
                if (parentMessages.isEmpty()) {
                    item {
                        EmptyStateBanner(
                            icon = Icons.Filled.Forum,
                            title = "Ergaan maatii hin jiru",
                            message = "Barsiisaa ykn bulchiinsa mana barumsaa qunnamuuf 'Ergi' tuqaa."
                        )
                    }
                } else {
                    items(parentMessages, key = { it.id }) { msg ->
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(msg.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.tertiaryContainer) {
                                        Text(
                                            text = "Gara ${msg.recipientRole}",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Ergamaa: ${msg.senderName} (${msg.senderRole}) • Barataa: ${msg.studentName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(msg.body, style = MaterialTheme.typography.bodyMedium)
                                if ((profile.userRole == UserRole.TEACHER || profile.userRole == UserRole.ADMIN) && msg.parentId != profile.userId) {
                                    TextButton(onClick = {
                                        replyTarget = msg
                                        showMsgDialog = true
                                    }) {
                                        Text("Deebisi (Reply to Parent)")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    PrivacyIsolationNotice(
                        "Eegumsa Odeeffannoo: Qunnamtiin maatii, barsiisaa fi bulchiinsaa akkasumas Early Warning barattootaaf dhorkaadha."
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Beeksisa fi Odeeffannoo",
                        subtitle = "Beeksisa bulchiinsa mana barumsaa hunda",
                        modifier = Modifier.weight(1f)
                    )
                    if (canPostAnnouncements) {
                        Button(
                            onClick = { showAnnDialog = true },
                            modifier = Modifier.testTag("post_announcement_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Baasi")
                        }
                    }
                }
            }

            if (announcements.isEmpty()) {
                item {
                    EmptyStateBanner(
                        icon = Icons.Filled.Campaign,
                        title = "Beeksisni hin jiru",
                        message = "Beeksisa haaraan yeroo ba'u asitti argama."
                    )
                }
            } else {
                items(announcements, key = { it.id }) { ann ->
                    AnnouncementCard(ann)
                }
            }
        }
    }

    if (showAnnDialog) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(AnnouncementCategory.GENERAL) }
        AlertDialog(
            onDismissRequest = { showAnnDialog = false },
            title = { Text("Beeksisa Haaraa Baasi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Mata-duree") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AnnouncementCategory.entries.forEach { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.labelOromo) })
                        }
                    }
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Ergaa Beeksisa") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPostAnnouncement(title, body, category)
                        showAnnDialog = false
                    },
                    enabled = title.trim().length >= 2 && body.trim().length >= 2
                ) { Text("Maxxansi") }
            },
            dismissButton = { TextButton(onClick = { showAnnDialog = false }) { Text("Dhiisi") } }
        )
    }

    if (showMsgDialog) {
        val target = replyTarget
        val isReplyToParent = target != null
        var recipientRole by remember(target) {
            mutableStateOf(if (isReplyToParent) "PARENT" else "TEACHER")
        }
        val defaultTeacher = teachers.firstOrNull()
        var recipientId by remember(target) {
            mutableStateOf(
                if (isReplyToParent) target.parentId
                else defaultTeacher?.teacherId ?: profile.adminId
            )
        }
        val targetParentId = target?.parentId ?: profile.userId
        val targetParentName = target?.parentName ?: profile.displayName
        val studentId by remember(target) {
            mutableStateOf(target?.studentId ?: profile.linkedStudentId)
        }
        var studentName by remember(target) {
            mutableStateOf(target?.studentName ?: profile.linkedStudentName)
        }
        var subject by remember(target) {
            mutableStateOf(if (isReplyToParent) "Re: ${target.subject}" else "")
        }
        var body by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showMsgDialog = false
                replyTarget = null
            },
            title = { Text(if (isReplyToParent) "Deebii Gara ${target.parentName}" else "Ergaa Iccitii Ergi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isReplyToParent) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = recipientRole == "TEACHER",
                                onClick = {
                                    recipientRole = "TEACHER"
                                    recipientId = defaultTeacher?.teacherId ?: profile.adminId
                                },
                                label = { Text("Gara Barsiisaa") }
                            )
                            FilterChip(
                                selected = recipientRole == "ADMIN",
                                onClick = {
                                    recipientRole = "ADMIN"
                                    recipientId = profile.adminId
                                },
                                label = { Text("Gara Bulchiinsaa") }
                            )
                        }
                    }
                    OutlinedTextField(value = studentName, onValueChange = { studentName = it }, label = { Text("Maqaa Barataa") }, singleLine = true)
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Mata-duree") }, singleLine = true)
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Ergaa") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSendParentMessage(
                            targetParentId,
                            targetParentName,
                            studentId,
                            studentName,
                            recipientRole,
                            recipientId.ifBlank { profile.adminId },
                            subject,
                            body
                        )
                        showMsgDialog = false
                        replyTarget = null
                    },
                    enabled = subject.trim().length >= 2 && body.isNotBlank() && studentName.trim().length >= 2
                ) { Text("Ergi") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showMsgDialog = false
                    replyTarget = null
                }) { Text("Dhiisi") }
            }
        )
    }
}
