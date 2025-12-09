package de.ihreapotheke.iasdkexample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.ihreapotheke.iasdkexample.ui.theme.IASDKExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            IASDKExampleTheme {
                val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) <= 0
                val navController = rememberNavController()
                var selectedTab by remember { mutableStateOf(BottomTab.HOME) }

                val backStackEntry by navController.currentBackStackEntryAsState()
                LaunchedEffect(backStackEntry) {
                    val route = backStackEntry?.destination?.route
                    selectedTab = getTabForRoute(route) ?: selectedTab
                }

                Column(
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(navController, startDestination = HostAppRoute.MainScreen) {
                            hostapp(navController = navController)
                        }
                    }

                    if (imeVisible) {
                        BottomBar(
                            currentTab = selectedTab,
                            onTabSelected = {
                                selectedTab = it
                                onBottomTabSelect(selectedTab = it, navController = navController)
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Maps a navigation route to its corresponding bottom tab.
     * Returns null if the route doesn't correspond to a tab.
     */
    private fun getTabForRoute(route: String?): BottomTab? {
        if (route == null) return null

        return when {
            route.contains("MainScreen") && !route.contains("Sdk") -> BottomTab.HOME
            route.contains("SdkStartScreen") -> BottomTab.SDK_START_SCREEN
            route.contains("SdkSearchScreen") -> BottomTab.SDK_SEARCH
            route.contains("SdkCartScreen") -> BottomTab.SDK_CART
            route.contains("SdkPharmacyScreen") -> BottomTab.SDK_PHARMACY
            else -> null // Route doesn't correspond to a tab (e.g., DemoSettings, OverlayDemo)
        }
    }

    private fun onBottomTabSelect(selectedTab: BottomTab, navController: NavHostController) {
        when (selectedTab) {
            BottomTab.HOME -> navController.navigate(HostAppRoute.MainScreen) {
                manageBottomNavigationBackStack()
            }

            BottomTab.SDK_START_SCREEN -> navController.navigate(HostAppRoute.SdkStartScreen) {
                manageBottomNavigationBackStack()
            }

            BottomTab.SDK_SEARCH -> navController.navigate(HostAppRoute.SdkSearchScreen) {
                manageBottomNavigationBackStack()
            }

            BottomTab.SDK_CART -> navController.navigate(HostAppRoute.SdkCartScreen) {
                manageBottomNavigationBackStack()
            }

            BottomTab.SDK_PHARMACY -> navController.navigate(HostAppRoute.SdkPharmacyScreen) {
                manageBottomNavigationBackStack()
            }
        }
    }
}

fun NavOptionsBuilder.manageBottomNavigationBackStack() {
    popUpTo(0) { inclusive = true }
    launchSingleTop = true
    restoreState = true
}
