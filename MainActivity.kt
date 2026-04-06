package com.example.hanaparal

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.hanaparal.auth.GoogleAuthUiClient
import com.example.hanaparal.data.NetworkStatus
import com.example.hanaparal.navigation.NavGraph
import com.example.hanaparal.ui.theme.HanapAralTheme
import com.example.hanaparal.viewmodel.AuthViewModel
import com.example.hanaparal.viewmodel.MainViewModel
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : FragmentActivity() {

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> /* Handle permission result if needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Log FCM Token for Notifications
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM_TOKEN", "Token: $token")
        }

        // Log Installation ID for In-App Messaging
        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("MY_FID", "Installation ID: " + task.result)
            } else {
                Log.e("MY_FID", "Unable to get Installation ID")
            }
        }

        setContent {
            HanapAralTheme {
                val mainViewModel: MainViewModel = viewModel()
                val authViewModel: AuthViewModel = viewModel()

                val isLoading by mainViewModel.isLoading.collectAsState()
                val networkStatus by mainViewModel.networkStatus.collectAsState()

                Column(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = networkStatus == NetworkStatus.Unavailable,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Red.copy(alpha = 0.8f))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No internet connection. Some features may not work.",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val navController = rememberNavController()
                        Box(modifier = Modifier.weight(1f)) {
                            NavGraph(
                                navController = navController,
                                authViewModel = authViewModel,
                                googleAuthUiClient = googleAuthUiClient
                            )
                        }
                    }
                }
            }
        }
    }
}
