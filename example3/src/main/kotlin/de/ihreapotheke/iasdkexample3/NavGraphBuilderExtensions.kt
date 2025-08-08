package de.ihreapotheke.iasdkexample3

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation

fun NavGraphBuilder.hostAppNavigationGraph() {
    navigation<HostAppRoute.StartHostApp>(
        startDestination = HostAppRoute.DefaultScreen
    ) {
        composable<HostAppRoute.DefaultScreen> {
            DefaultScreen()
        }

        composable<HostAppRoute.StartSdkScreen> {
            StartSdkScreen()
        }
    }
}
