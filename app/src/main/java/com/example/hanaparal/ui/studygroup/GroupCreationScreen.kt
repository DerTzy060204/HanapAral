package com.example.hanaparal.ui.studygroup

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hanaparal.data.model.StudyGroup
import com.example.hanaparal.ui.components.LoadingOverlay
import com.example.hanaparal.ui.components.rememberBiometricLauncher
import com.example.hanaparal.viewmodel.GroupUiState
import com.example.hanaparal.viewmodel.GroupViewModel
import com.example.hanaparal.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCreationScreen(
    groupViewModel: GroupViewModel,
    mainViewModel: MainViewModel = viewModel(),
    onGroupCreated: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by groupViewModel.uiState.collectAsState()
    val appConfig by mainViewModel.appConfig.collectAsState()
    val context = LocalContext.current

    var name        by remember { mutableStateOf("") }
    var subject     by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var maxMembers  by remember { mutableStateOf(appConfig.maxMembersPerGroup.toString()) }

    var isUnlocked by remember { mutableStateOf(false) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val isSuperuser = remember(appConfig.superuserEmails, currentUser) {
        val userEmail = currentUser?.email?.lowercase() ?: ""
        appConfig.superuserEmails.any { it.lowercase() == userEmail }
    }

    val biometricLauncher = rememberBiometricLauncher(
        title = "Unlock Group Creation",
        subtitle = "Authenticate to bypass global restriction for this session",
        onSuccess = {
            isUnlocked = true
            Toast.makeText(context, "Group creation unlocked for this session!", Toast.LENGTH_SHORT).show()
        },
        onError = { _, err ->
            Toast.makeText(context, "Authentication failed: $err", Toast.LENGTH_SHORT).show()
        }
    )

    if (uiState is GroupUiState.Loading) LoadingOverlay("Creating group…")
    
    // Auto-reset error state if it happens (e.g. from background validation)
    if (uiState is GroupUiState.Error) {
        LaunchedEffect(uiState) {
            groupViewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Study Group", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Show fingerprint icon if disabled and user is superuser
                    if (!appConfig.isGroupCreationEnabled && isSuperuser && !isUnlocked) {
                        IconButton(onClick = { biometricLauncher() }) {
                            Icon(Icons.Default.Fingerprint, contentDescription = "Unlock Admin Features", tint = MaterialTheme.colorScheme.primary)
                        }
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            if (!appConfig.isGroupCreationEnabled && !isUnlocked) {
                // Red box warning (Exactly like your screenshot)
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "Group creation is currently disabled by the administrator.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Creation Form (Only shown when enabled or unlocked)
                Text(
                    text = "Fill in the details below to start a new learning community.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                CreationTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Group Name",
                    icon = Icons.Default.Title,
                    placeholder = "e.g. Exam Cram Session"
                )

                Spacer(Modifier.height(16.dp))

                CreationTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = "Subject / Topic",
                    icon = Icons.AutoMirrored.Filled.Subject,
                    placeholder = "e.g. Computer Science"
                )

                Spacer(Modifier.height(16.dp))

                CreationTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description (Optional)",
                    icon = Icons.Default.Description,
                    placeholder = "Tell members what this group is about...",
                    singleLine = false,
                    modifier = Modifier.height(120.dp)
                )

                Spacer(Modifier.height(16.dp))

                CreationTextField(
                    value = maxMembers,
                    onValueChange = { if (it.all(Char::isDigit)) maxMembers = it },
                    label = "Max Members",
                    icon = Icons.Default.Group,
                    keyboardType = KeyboardType.Number,
                    supportingText = "Default limit: ${appConfig.maxMembersPerGroup}"
                )

                Spacer(Modifier.height(40.dp))

                Button(
                    onClick = {
                        val limit = maxMembers.toIntOrNull() ?: appConfig.maxMembersPerGroup
                        val group = StudyGroup(
                            name = name.trim(),
                            subject = subject.trim(),
                            description = description.trim(),
                            maxMembers = limit
                        )
                        // Corrected: pass isUnlocked to bypassConfig
                        groupViewModel.createGroup(
                            group = group, 
                            bypassConfig = isUnlocked, 
                            onSuccess = onGroupCreated
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = name.isNotBlank() && subject.isNotBlank(),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text("Create Group", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CreationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder) } } else null,
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = keyboardType
        ),
        supportingText = supportingText?.let { { Text(it) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}
