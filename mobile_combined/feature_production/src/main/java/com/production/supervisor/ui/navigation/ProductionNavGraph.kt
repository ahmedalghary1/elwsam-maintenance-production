package com.production.supervisor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.production.supervisor.ui.screens.home.ProductionHomeScreen
import com.production.supervisor.ui.screens.login.LoginScreen

@Composable
fun ProductionNavGraph(
    isLoggedIn: Boolean,
    onEmbeddedLogout: (() -> Unit)? = null,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            ProductionHomeScreen(
                onLogout = {
                    if (onEmbeddedLogout != null) onEmbeddedLogout() else
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                }
            )
        }
    }
}
