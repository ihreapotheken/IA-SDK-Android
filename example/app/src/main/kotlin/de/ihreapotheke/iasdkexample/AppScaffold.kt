package de.ihreapotheke.iasdkexample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import de.ihreapotheken.sdk.core.navigation.SdkEntryScreen
import de.ihreapotheken.sdk.core.navigation.SdkGraph.sdkGraphProvider
import de.ihreapotheken.sdk.core.navigation.route.Route
import de.ihreapotheken.sdk.core.ui.StatusBarProtection
import de.ihreapotheken.sdk.core.ui.theme.LocalColorTokens

@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    var isAtRoot by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(BottomTab.HOME) }

    Scaffold(
        bottomBar = {
            BottomBar(navController, currentTab) {
                currentTab = it
                onBottomTabSelect(it, navController)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Route.Integration.StartScreen
            ) {
                hostAppNavigationGraph(navController)
                sdkGraphProvider()
            }
        }

        SdkEntryScreen(
            onDestinationChanged = { atRoot -> isAtRoot = atRoot },
            navController = navController,
            startRoute = HostAppRoute.DefaultScreen
        )
    }

    StatusBarProtection(LocalColorTokens.current.get("Header/bg"))
}

private fun onBottomTabSelect(selectedTab: BottomTab, navController: NavHostController) {
    val route = when (selectedTab) {
        BottomTab.HOME -> HostAppRoute.StartHostApp
        BottomTab.OTC -> Route.Otc.StartScreen
        BottomTab.PHARMACY -> Route.Pharmacy.StartScreen
        BottomTab.START_SDK -> HostAppRoute.StartSdkScreen
    }

    navController.navigate(route) {
        manageBottomNavigationBackStack()
    }
}

private fun NavOptionsBuilder.manageBottomNavigationBackStack() {
    popUpTo(0) { inclusive = true }
    launchSingleTop = true
    restoreState = true
}
