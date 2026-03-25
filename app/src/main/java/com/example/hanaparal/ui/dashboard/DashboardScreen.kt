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
    var showSignOutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val user = googleAuthUiClient.getSignedInUser()
    val isSuperuser = remember(appConfig.superuserEmails, user) {
        val userEmail = user?.email?.lowercase() ?: ""
        appConfig.superuserEmails.any { it.lowercase() == userEmail }
    }