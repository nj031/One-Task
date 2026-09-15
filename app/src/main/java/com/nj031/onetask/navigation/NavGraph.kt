package com.nj031.onetask.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nj031.onetask.ui.screens.AuthScreen
import com.nj031.onetask.ui.screens.HomeScreen

@Composable
fun OneTaskNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            val navigateHome = { navController.navigate(Screen.Home.route) }
            AuthScreen(
                onLogIn = navigateHome,
                onContinueWithGoogle = navigateHome,
                onCreateAccount = navigateHome
            )
        }
        composable(Screen.Home.route) {
            HomeScreen()
        }
    }
}
