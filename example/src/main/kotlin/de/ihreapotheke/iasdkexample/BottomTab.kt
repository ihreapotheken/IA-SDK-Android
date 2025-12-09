package de.ihreapotheke.iasdkexample

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    SDK_START_SCREEN("Start", Icons.Default.FavoriteBorder),
    SDK_SEARCH("Search", Icons.Default.Search),
    SDK_CART("Cart", Icons.Default.ShoppingCart),
    SDK_PHARMACY("Pharmacy", Icons.Default.Place),
}
