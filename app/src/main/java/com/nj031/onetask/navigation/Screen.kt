package com.nj031.onetask.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Home : Screen("home")
}
