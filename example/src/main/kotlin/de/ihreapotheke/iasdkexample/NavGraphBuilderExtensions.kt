package de.ihreapotheke.iasdkexample

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import de.ihreapotheken.sdk.integrations.api.view.IaSdkScreen
import de.ihreapotheken.sdk.integrations.api.view.SdkEntryPoint

fun NavGraphBuilder.hostapp(navController: NavHostController) {
    composable<HostAppRoute.MainScreen> {
        StartSdkScreen(
            openSdkStartScreen = {
                navController.navigate(HostAppRoute.SdkStartScreen) {
                    manageBottomNavigationBackStack()
                }
            },
        )
    }

    composable<HostAppRoute.SdkStartScreen> {
        IaSdkScreen(
            sdkEntryPoint = SdkEntryPoint.StartScreen,
            onNavigateToEntryPoint = createEntryPointNavigationHandler(navController)
        )
    }

    composable<HostAppRoute.SdkSearchScreen> {
        IaSdkScreen(
            sdkEntryPoint = SdkEntryPoint.SearchScreen,
            onNavigateToEntryPoint = createEntryPointNavigationHandler(navController)
        )
    }

    composable<HostAppRoute.SdkCartScreen> {
        IaSdkScreen(
            sdkEntryPoint = SdkEntryPoint.CartScreen,
            onNavigateToEntryPoint = createEntryPointNavigationHandler(navController)
        )
    }

    composable<HostAppRoute.SdkPharmacyScreen> {
        IaSdkScreen(
            sdkEntryPoint = SdkEntryPoint.PharmacyScreen,
            onNavigateToEntryPoint = createEntryPointNavigationHandler(navController)
        )
    }
}

/**
 * Helper function to create a navigation callback for SDK entry points.
 * This allows the host app to intercept SDK navigation and handle it via bottom tabs.
 *
 * Note: We don't clear the back stack here (unlike bottom tab clicks) because we want
 * to preserve navigation history when the SDK navigates between screens. The tab state
 * will be automatically synced by observing the NavController's current destination.
 *
 * @param navController The host app's navigation controller
 * @return A callback that returns true if the navigation was handled, false otherwise
 */
private fun createEntryPointNavigationHandler(
    navController: NavHostController
): (SdkEntryPoint) -> Boolean = { targetEntryPoint ->
    when (targetEntryPoint) {
        SdkEntryPoint.StartScreen -> {
            navController.navigate(HostAppRoute.SdkStartScreen)
            true
        }

        SdkEntryPoint.SearchScreen -> {
            navController.navigate(HostAppRoute.SdkSearchScreen)
            true
        }

        SdkEntryPoint.CartScreen -> {
            navController.navigate(HostAppRoute.SdkCartScreen)
            true
        }

        SdkEntryPoint.PharmacyScreen -> {
            navController.navigate(HostAppRoute.SdkPharmacyScreen)
            true
        }

        else -> false // Let SDK handle other entry points internally
    }
}
