package com.example.posko24.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class untuk mendefinisikan item-item pada Bottom Navigation Bar.
 * Setiap objek merepresentasikan satu layar utama.
 *
 * @param route String unik untuk rute navigasi.
 * @param title Judul yang akan ditampilkan.
 * @param icon Ikon yang akan ditampilkan.
 */
sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )
    object MyOrders : BottomNavItem(
        route = "my_orders",
        title = "My Order",
        icon = Icons.Default.Build
    )
    object Chats : BottomNavItem(
        route = "chats",
        title = "Chats",
        icon = Icons.Default.Email
    )
    object Profile : BottomNavItem(
        route = "profile",
        title = "Profile",
        icon = Icons.Default.Person
    )
}
