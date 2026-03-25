package com.example.hanaparal

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.hanaparal.auth.GoogleAuthUiClient
import com.example.hanaparal.navigation.NavGraph
import com.example.hanaparal.ui.theme.HanapAralTheme
import com.example.hanaparal.viewmodel.AuthViewModel
import com.example.hanaparal.viewmodel.MainViewModel
import com.google.android.gms.auth.api.identity.Identity

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

        setContent {
            HanapAralTheme {
                val mainViewModel: MainViewModel = viewModel()
                val authViewModel: AuthViewModel = viewModel()

                val isLoading by mainViewModel.isLoading.collectAsState()

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val navController = rememberNavController()
                    NavGraph(
                        navController       = navController,
                        authViewModel       = authViewModel,
                        googleAuthUiClient  = googleAuthUiClient
                    )
                }
            }
        }
    }
}
