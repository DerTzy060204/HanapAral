package com.example.hanaparal.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.hanaparal.auth.FirebaseAuthState
import com.example.hanaparal.auth.GoogleAuthUiClient
import com.example.hanaparal.ui.admin.AdminScreen
import com.example.hanaparal.ui.dashboard.DashboardScreen
import com.example.hanaparal.ui.login.CreateAccountScreen
import com.example.hanaparal.ui.login.LoginScreen
import com.example.hanaparal.ui.profile.ProfileScreen
import com.example.hanaparal.ui.studygroup.GroupCreationScreen
import com.example.hanaparal.ui.studygroup.GroupDetailScreen
import com.example.hanaparal.ui.studygroup.GroupListScreen
import com.example.hanaparal.viewmodel.AuthViewModel
import com.example.hanaparal.viewmodel.GroupViewModel
import com.example.hanaparal.viewmodel.MainViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    googleAuthUiClient: GoogleAuthUiClient
) {
    val authState by authViewModel.authState.collectAsState()
    val mainViewModel: MainViewModel = viewModel()

    // Handle global authentication state changes
    LaunchedEffect(authState) {
        val currentDestination = navController.currentDestination?.route
        Log.d("NavGraph", "AuthState updated: ${authState::class.simpleName} | Current destination: $currentDestination")

        when (authState) {
            is FirebaseAuthState.Unauthenticated -> {
                if (currentDestination != Screen.Login.route && currentDestination != Screen.CreateAccount.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is FirebaseAuthState.NeedsProfile -> {
                if (currentDestination != Screen.Profile.route) {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(0) { inclusive = true } // Clear everything to ensure a clean transition
                    }
                }
            }
            is FirebaseAuthState.Authenticated -> {
                // If we're on a setup or login screen, go to dashboard
                if (currentDestination == Screen.Login.route || 
                    currentDestination == Screen.CreateAccount.route || 
                    currentDestination == Screen.Profile.route ||
                    currentDestination == null) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {}
        }
    }

    // Determine initial route
    val startDestination = if (googleAuthUiClient.getSignedInUser() != null) {
        // We start at Dashboard but LaunchedEffect will redirect to Profile if needed
        Screen.Dashboard.route 
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController, 
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                googleAuthUiClient = googleAuthUiClient,
                onNavigateToCreateAccount = {
                    navController.navigate(Screen.CreateAccount.route)
                },
                onLoginSuccess = { /* Handled by LaunchedEffect */ }
            )
        }

        composable(Screen.CreateAccount.route) {
            CreateAccountScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onSuccess = { /* Handled by LaunchedEffect */ }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                googleAuthUiClient = googleAuthUiClient,
                authViewModel = authViewModel,
                mainViewModel = mainViewModel,
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToGroups = { navController.navigate(Screen.GroupList.route) },
                onNavigateToGroupDetail = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId))
                },
                onNavigateToAdmin = { navController.navigate(Screen.Admin.route) },
                onSignOut = { /* Handled by LaunchedEffect */ }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = {
                    if (authState is FirebaseAuthState.Authenticated) {
                        navController.popBackStack()
                    } else {
                        authViewModel.signOut()
                    }
                }
            )
        }

        composable(Screen.Admin.route) {
            AdminScreen(
                onBack = { navController.popBackStack() },
                mainViewModel = mainViewModel
            )
        }

        composable(Screen.GroupList.route) {
            val groupViewModel: GroupViewModel = viewModel()
            GroupListScreen(
                groupViewModel = groupViewModel,
                onGroupClick = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId))
                },
                onCreateGroup = { navController.navigate(Screen.GroupCreation.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.GroupCreation.route) {
            val groupViewModel: GroupViewModel = viewModel()
            GroupCreationScreen(
                groupViewModel = groupViewModel,
                mainViewModel = mainViewModel,
                onGroupCreated = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId)) {
                        popUpTo(Screen.GroupList.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val groupViewModel: GroupViewModel = viewModel()
            GroupDetailScreen(
                groupId = groupId,
                groupViewModel = groupViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
