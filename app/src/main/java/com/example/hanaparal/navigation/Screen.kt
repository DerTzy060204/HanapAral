package com.example.hanaparal.navigation

sealed class Screen(val route: String) {
    // Auth
    object Login : Screen("login")

    // Main app
    object Dashboard : Screen("dashboard")
    object Profile : Screen("profile")
    object Admin : Screen("admin")

    // Groups
    object GroupList : Screen("group_list")
    object GroupCreation : Screen("group_creation")
    object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: String) = "group_detail/$groupId"
    }
}
