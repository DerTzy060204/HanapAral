package com.example.hanaparal.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hanaparal.data.model.StudentProfile
import com.example.hanaparal.ui.components.ErrorDialog
import com.example.hanaparal.ui.components.LoadingOverlay
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile by viewModel.profile.collectAsState()

    // Pre-fill form fields once the profile loads from Firestore
    var name    by remember { mutableStateOf("") }
    var course  by remember { mutableStateOf("") }
    var yearLevel by remember { mutableStateOf("1") }
    var bio     by remember { mutableStateOf("") }

    LaunchedEffect(profile) {
        profile?.let {
            name      = it.name
            course    = it.course
            yearLevel = it.yearLevel.toString()
            bio       = it.bio
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Saved) {
            onBack()
        }
    }

    if (uiState is ProfileUiState.Loading) LoadingOverlay("Saving profile…")
    if (uiState is ProfileUiState.Error) {
        ErrorDialog(
            message = (uiState as ProfileUiState.Error).message,
            onDismiss = viewModel::resetState
        )
    }

    val currentUser = FirebaseAuth.getInstance().currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.SemiBold) },
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar placeholder with initials
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (currentUser?.displayName?.take(1) ?: "?").uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = currentUser?.email ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            // ── Form fields ───────────────────────────────────────────
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = course,
                onValueChange = { course = it },
                label = { Text("Course / Program") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = yearLevel,
                onValueChange = { if (it.length <= 1 && it.all(Char::isDigit)) yearLevel = it },
                label = { Text("Year Level") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Short Bio") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 4
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    val uid = currentUser?.uid ?: return@Button
                    
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        val token = if (task.isSuccessful) task.result else ""
                        
                        viewModel.saveProfile(
                            StudentProfile(
                                userId    = uid,
                                name      = name.trim(),
                                email     = currentUser.email ?: "",
                                course    = course.trim(),
                                yearLevel = yearLevel.toIntOrNull() ?: 1,
                                photoUrl  = currentUser.photoUrl?.toString() ?: "",
                                bio       = bio.trim(),
                                fcmToken  = token
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Save Profile", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
