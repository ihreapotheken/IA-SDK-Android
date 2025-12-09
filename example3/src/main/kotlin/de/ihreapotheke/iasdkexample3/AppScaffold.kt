package de.ihreapotheke.iasdkexample3

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

@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    var currentTab by remember { mutableStateOf(BottomTab.HOME) }

    Scaffold(
        bottomBar = {
            BottomBar(currentTab) {
                currentTab = it
                onBottomTabSelect(it, navController)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = HostAppRoute.StartHostApp
            ) {
                hostAppNavigationGraph()
            }
        }
    }
}

private fun onBottomTabSelect(selectedTab: BottomTab, navController: NavHostController) {
    val route = when (selectedTab) {
        BottomTab.HOME -> HostAppRoute.StartHostApp
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
