package de.ihreapotheke.iasdkexample

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    START_SDK("Start", Icons.Default.FavoriteBorder)
}
