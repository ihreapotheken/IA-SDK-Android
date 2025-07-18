package de.ihreapotheke.iasdkexample

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    OTC("OTC", Icons.Default.Search),
    PHARMACY("Pharmacy", Icons.Default.Place),

    START_SDK("Start", Icons.Default.FavoriteBorder)
}
