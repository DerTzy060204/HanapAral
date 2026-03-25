package com.example.hanaparal.ui.studygroup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hanaparal.data.model.Announcement
import com.example.hanaparal.data.model.StudyGroup
import com.example.hanaparal.ui.components.ErrorDialog
import com.example.hanaparal.ui.components.LoadingOverlay
import com.example.hanaparal.viewmodel.GroupUiState
import com.example.hanaparal.viewmodel.GroupViewModel
import com.example.hanaparal.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    groupViewModel: GroupViewModel,
    mainViewModel: MainViewModel = viewModel(),
    onBack: () -> Unit
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val currentUserName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Student"

    val uiState by groupViewModel.uiState.collectAsState()
    val allGroups by groupViewModel.allGroups.collectAsState()
    val appConfig by mainViewModel.appConfig.collectAsState()
    val group = allGroups.find { it.groupId == groupId }

    val announcementsFlow = remember(groupId) { groupViewModel.getAnnouncementsFlow(groupId) }
    val announcements by announcementsFlow.collectAsState()

    val isMember = group?.memberIds?.contains(currentUserId) == true
    val isAdmin  = group?.adminId == currentUserId

    var announcementText by remember { mutableStateOf("") }
    var showLeaveDialog  by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog   by remember { mutableStateOf(false) }

    if (uiState is GroupUiState.Loading) LoadingOverlay("Processing...")
    if (uiState is GroupUiState.Error) {
        ErrorDialog(
            message = (uiState as GroupUiState.Error).message,
            onDismiss = groupViewModel::resetState
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Group?") },
            text  = { Text("You can rejoin later if the group is still open.") },
            confirmButton = {
                TextButton(onClick = {
                    groupViewModel.leaveGroup(groupId)
                    showLeaveDialog = false
                    onBack()
                }) { Text("Leave", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Group?") },
            text  = { Text("This action cannot be undone. All announcements will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    groupViewModel.deleteGroup(groupId)
                    showDeleteDialog = false
                    onBack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditDialog && group != null) {
        EditGroupDialog(
            group = group,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedGroup ->
                groupViewModel.updateGroup(updatedGroup)
                showEditDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.name ?: "Group Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Group")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Group")
                        }
                    } else if (isMember) {
                        IconButton(onClick = { showLeaveDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Leave Group")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isMember) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = announcementText,
                            onValueChange = { announcementText = it },
                            placeholder = { Text("Post an announcement…") },
                            modifier = Modifier.weight(1f),
                            maxLines = 4,
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (announcementText.isNotBlank()) {
                                    groupViewModel.postAnnouncement(
                                        Announcement(
                                            groupId    = groupId,
                                            authorId   = currentUserId,
                                            authorName = currentUserName,
                                            content    = announcementText.trim()
                                        )
                                    )
                                    announcementText = ""
                                }
                            },
                            enabled = announcementText.isNotBlank(),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                group?.let { g ->
                    GroupHeaderSection(g)

                    if (!isMember) {
                        JoinSection(g, appConfig.isJoiningGroupsEnabled, groupViewModel)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    "Announcements",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (announcements.isEmpty()) {
                item {
                    EmptyAnnouncementsState()
                }
            } else {
                items(announcements, key = { it.announcementId }) { announcement ->
                    AnnouncementCard(announcement)
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun GroupHeaderSection(group: StudyGroup) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                group.subject.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = group.subject,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Study Group",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (group.description.isNotBlank()) {
                    Text(
                        text = group.description,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoChip(
                        icon = Icons.Default.Group,
                        text = "${group.memberCount}/${group.maxMembers}",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                    InfoChip(
                        icon = Icons.Default.Person,
                        text = "Admin: ${group.adminName}",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String, containerColor: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun JoinSection(group: StudyGroup, isEnabled: Boolean, viewModel: GroupViewModel) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (isEnabled && !group.isFull) {
            Button(
                onClick = { viewModel.joinGroup(group.groupId, group.name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Join this Group", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else if (!isEnabled) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Joining groups is currently disabled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        } else if (group.isFull) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "This group is currently full.",
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EmptyAnnouncementsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Campaign,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No announcements yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            "Be the first to share something with the group!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AnnouncementCard(announcement: Announcement) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            announcement.authorName.take(1).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = announcement.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    announcement.createdAt?.let {
                        Text(
                            text = dateFormat.format(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = announcement.content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun EditGroupDialog(
    group: StudyGroup,
    onDismiss: () -> Unit,
    onConfirm: (StudyGroup) -> Unit
) {
    var name by remember { mutableStateOf(group.name) }
    var subject by remember { mutableStateOf(group.subject) }
    var description by remember { mutableStateOf(group.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Group Details") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(group.copy(name = name, subject = subject, description = description))
                },
                enabled = name.isNotBlank() && subject.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
