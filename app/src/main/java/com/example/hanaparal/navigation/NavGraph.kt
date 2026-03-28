package com.example.hanaparal.navigation

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
        when (authState) {
            is FirebaseAuthState.Unauthenticated -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is FirebaseAuthState.NeedsProfile -> {
                navController.navigate(Screen.Profile.route) {
                    // Prevent going back to login or dashboard if profile is mandatory
                    // but allow the back button to trigger a sign out which leads back to login
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
            is FirebaseAuthState.Authenticated -> {
                // If we were on Login or Profile(initial), move to Dashboard
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == Screen.Login.route || currentRoute == Screen.Profile.route) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
            else -> {}
        }
    }

    val startDestination = if (googleAuthUiClient.getSignedInUser() != null) {
        // We'll let the LaunchedEffect handle the specific redirection (Dashboard vs Profile)
        // but Dashboard is a safe initial "Authorized" area
        Screen.Dashboard.route
    } else {
        Screen.Login.route
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                googleAuthUiClient = googleAuthUiClient,
                onLoginSuccess = {
                    // Handled by global LaunchedEffect
                }
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
                onSignOut = {
                    // Sign out is handled by the LaunchedEffect above
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = {
                    if (authState is FirebaseAuthState.Authenticated) {
                        navController.popBackStack()
                    } else if (authState is FirebaseAuthState.NeedsProfile) {
                        // If they are on the profile setup screen but haven't finished,
                        // clicking back should sign them out and return to login.
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
