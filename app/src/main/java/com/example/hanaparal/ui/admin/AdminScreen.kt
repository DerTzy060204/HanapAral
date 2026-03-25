package com.example.hanaparal.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hanaparal.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    mainViewModel: MainViewModel = viewModel()
) {
    val appConfig by mainViewModel.appConfig.collectAsState()
    val context = LocalContext.current

    var header by remember { mutableStateOf(appConfig.globalAnnouncementHeader) }
    var maxMembers by remember { mutableStateOf(appConfig.maxMembersPerGroup.toString()) }
    var isCreationEnabled by remember { mutableStateOf(appConfig.isGroupCreationEnabled) }
    var isJoiningEnabled by remember { mutableStateOf(appConfig.isJoiningGroupsEnabled) }

    // Sync local state when appConfig changes (e.g. on first load)
    LaunchedEffect(appConfig) {
        header = appConfig.globalAnnouncementHeader
        maxMembers = appConfig.maxMembersPerGroup.toString()
        isCreationEnabled = appConfig.isGroupCreationEnabled
        isJoiningEnabled = appConfig.isJoiningGroupsEnabled
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Console", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                "Global Configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "These settings affect all users in real-time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // ── Announcement Section ─────────────────────────────────────
            AdminSectionHeader(icon = Icons.Default.Campaign, title = "Announcements")
            OutlinedTextField(
                value = header,
                onValueChange = { header = it },
                label = { Text("Global Announcement Message") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(24.dp))

            // ── Group Settings Section ───────────────────────────────────
            AdminSectionHeader(icon = Icons.Default.Group, title = "Group Management")

            OutlinedTextField(
                value = maxMembers,
                onValueChange = { if (it.all(Char::isDigit)) maxMembers = it },
                label = { Text("Max Members per Group") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Toggles
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    AdminSwitchRow(
                        label = "Allow Group Creation",
                        description = "Enable or disable new study groups",
                        checked = isCreationEnabled,
                        onCheckedChange = { isCreationEnabled = it }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    AdminSwitchRow(
                        label = "Allow Joining Groups",
                        description = "Enable or disable joining existing groups",
                        checked = isJoiningEnabled,
                        onCheckedChange = { isJoiningEnabled = it }
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = {
                    val updatedConfig = appConfig.copy(
                        globalAnnouncementHeader = header,
                        maxMembersPerGroup = maxMembers.toIntOrNull() ?: appConfig.maxMembersPerGroup,
                        isGroupCreationEnabled = isCreationEnabled,
                        isJoiningGroupsEnabled = isJoiningEnabled
                    )
                    mainViewModel.updateConfig(updatedConfig) { success ->
                        if (success) {
                            Toast.makeText(context, "Settings updated successfully!", Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            Toast.makeText(context, "Failed to update settings.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Configuration", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AdminSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AdminSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}