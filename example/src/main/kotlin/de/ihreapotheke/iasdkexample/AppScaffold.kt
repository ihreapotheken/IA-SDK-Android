package de.ihreapotheke.iasdkexample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import de.ihreapotheken.sdk.integrations.api.IaSdk
import kotlinx.coroutines.launch

@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    var isAtRoot by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(BottomTab.HOME) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .imePadding()
            .navigationBarsPadding()
            .windowInsetsPadding(WindowInsets.systemBars),
        bottomBar = {
            BottomBar(currentTab) {
                currentTab = it
                coroutineScope.launch {

                    onBottomTabSelect(it, navController)
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Route.Integration.Root
            ) {
                hostAppNavigationGraph()
                sdkGraphProvider()
            }
        }

        SdkEntryScreen(
            onDestinationChanged = { atRoot -> isAtRoot = atRoot },
            navController = navController,
            startRoute = HostAppRoute.StartHostApp
        )
    }

    StatusBarProtection(LocalColorTokens.current.get("Header/bg"))
}

private fun onBottomTabSelect(selectedTab: BottomTab, navController: NavHostController) {
    when (selectedTab) {
        BottomTab.HOME -> navController.navigate(HostAppRoute.StartHostApp) { manageBottomNavigationBackStack() }
        BottomTab.START_SDK -> navController.navigate(HostAppRoute.StartSdkScreen) { manageBottomNavigationBackStack() }
        BottomTab.OTC -> IaSdk.openSearchScreen { manageBottomNavigationBackStack() }
        BottomTab.PHARMACY -> IaSdk.openPharmacyScreen { manageBottomNavigationBackStack() }
        BottomTab.CART -> IaSdk.openCartScreen { manageBottomNavigationBackStack() }
    }
}

private fun NavOptionsBuilder.manageBottomNavigationBackStack() {
    popUpTo(0) { inclusive = true }
    launchSingleTop = true
    restoreState = true
}
