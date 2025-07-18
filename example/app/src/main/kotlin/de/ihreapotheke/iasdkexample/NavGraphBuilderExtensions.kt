package de.ihreapotheke.iasdkexample

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation

fun NavGraphBuilder.hostAppNavigationGraph(navController: NavHostController) {
    navigation<HostAppRoute.StartHostApp>(
        startDestination = HostAppRoute.DefaultScreen
    ) {
        composable<HostAppRoute.DefaultScreen> {
            DefaultScreen()
        }

        composable<HostAppRoute.StartSdkScreen> {
            StartSdkScreen(navController = navController)
        }
    }
}
