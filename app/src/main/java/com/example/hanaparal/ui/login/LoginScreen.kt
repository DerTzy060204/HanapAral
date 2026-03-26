package com.example.hanaparal.ui.login

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hanaparal.auth.FirebaseAuthState
import com.example.hanaparal.auth.GoogleAuthUiClient
import com.example.hanaparal.ui.components.LoadingOverlay
import com.example.hanaparal.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    googleAuthUiClient: GoogleAuthUiClient,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authState by authViewModel.authState.collectAsState()

    // Navigate away as soon as auth succeeds
    LaunchedEffect(authState) {
        if (authState is FirebaseAuthState.Authenticated) onLoginSuccess()
        if (authState is FirebaseAuthState.Error) {
            Toast.makeText(context, (authState as FirebaseAuthState.Error).message, Toast.LENGTH_LONG).show()
            authViewModel.resetState()
        }
    }

    // Launcher that receives the result from Google's One-Tap sign-in UI
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        scope.launch {
            val signInResult = googleAuthUiClient.signInWithIntent(result.data ?: return@launch)
            authViewModel.onSignInResult(signInResult)
        }
    }

    if (authState is FirebaseAuthState.Loading) {
        LoadingOverlay("Signing you in…")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── Branding ──────────────────────────────────────────────────
        Text(
            text = "HanapAral",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Find your study group.\nLearn together.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(64.dp))

        // ── Google Sign-In button ─────────────────────────────────────
        Button(
            onClick = {
                scope.launch {
                    val intentSender = googleAuthUiClient.signIn()
                    intentSender?.let {
                        googleSignInLauncher.launch(IntentSenderRequest.Builder(it).build())
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Continue with Google",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "By continuing, you agree to our Terms of Service.",
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline
        )
    }
}