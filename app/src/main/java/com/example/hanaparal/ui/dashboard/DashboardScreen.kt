package com.example.hanaparal.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hanaparal.auth.GoogleAuthUiClient
import com.example.hanaparal.ui.components.ConfirmDialog
import com.example.hanaparal.ui.components.rememberBiometricLauncher
import com.example.hanaparal.ui.profile.ProfileViewModel
import com.example.hanaparal.viewmodel.AuthViewModel
import com.example.hanaparal.viewmodel.GroupViewModel
import com.example.hanaparal.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    googleAuthUiClient: GoogleAuthUiClient,
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    onNavigateToProfile: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToGroupDetail: (String) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onSignOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val groupViewModel: GroupViewModel = viewModel()
    val myGroups by groupViewModel.myGroups.collectAsState()
    val appConfig by mainViewModel.appConfig.collectAsState()
    val profile by profileViewModel.profile.collectAsState()
    
    var showSignOutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val user = googleAuthUiClient.getSignedInUser()
    
    // Use Firestore profile name if available, otherwise fallback to Google username
    val displayName = profile?.name?.ifBlank { user?.username } ?: user?.username ?: "Student"
    
    val isSuperuser = remember(appConfig.superuserEmails, user) {
        val userEmail = user?.email?.lowercase() ?: ""
        appConfig.superuserEmails.any { it.lowercase() == userEmail }
    }

    val biometricLauncher = rememberBiometricLauncher(
        title = "Admin Authentication",
        subtitle = "Authenticate to access superuser settings",
        onSuccess = { 
            // Auto-enable features on successful admin biometric auth
            val updatedConfig = appConfig.copy(
                isGroupCreationEnabled = true,
                isJoiningGroupsEnabled = true
            )
            mainViewModel.updateConfig(updatedConfig) { success ->
                if (success) {
                    Toast.makeText(context, "Group creation & joining enabled", Toast.LENGTH_SHORT).show()
                }
            }
            onNavigateToAdmin() 
        },
        onError = { _, err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
    )

    if (showSignOutDialog) {
        ConfirmDialog(
            title = "Sign Out",
            message = "Are you sure you want to sign out?",
            confirmLabel = "Sign Out",
            onConfirm = {
                showSignOutDialog = false
                scope.launch {
                    googleAuthUiClient.signOut()
                    authViewModel.signOut(onSignOut)
                }
            },
            onDismiss = { showSignOutDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("HanapAral", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Hello, $displayName 👋",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                actions = {
                    // Always show search for visibility
                    IconButton(onClick = onNavigateToGroups) {
                        Icon(Icons.Default.Search, contentDescription = "Find Groups")
                    }
                    if (isSuperuser) {
                        IconButton(onClick = { biometricLauncher() }) {
                            Icon(Icons.Default.Settings, contentDescription = "Admin Settings")
                        }
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = { showSignOutDialog = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToGroups,
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                text = { Text("Find Groups") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📢 Announcement",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = appConfig.globalAnnouncementHeader,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            item {
                Text(
                    text = "My Study Groups",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            if (myGroups.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📚", style = MaterialTheme.typography.displayLarge)
                            Spacer(Modifier.height(12.dp))
                            Text(text = "No groups yet", style = MaterialTheme.typography.titleMedium)
                            Text(text = "Tap 'Find Groups' to join or create one", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(myGroups, key = { it.groupId }) { group ->
                    GroupCard(
                        group = group,
                        onClick = { onNavigateToGroupDetail(group.groupId) }
                    )
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}
