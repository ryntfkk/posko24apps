package com.example.posko24.ui.main

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.posko24.navigation.BottomNavItem
import com.example.posko24.navigation.SOS_ROUTE
import com.example.posko24.ui.chat.ChatListScreen
import com.example.posko24.ui.home.HomeScreen
import com.example.posko24.ui.orders.MyOrdersScreen
import com.example.posko24.ui.profile.ProfileScreen
import com.example.posko24.ui.provider.ProviderDashboardScreen


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    mainNavController: NavHostController,
    onCategoryClick: (String) -> Unit,
    onNavigateToConversation: (String) -> Unit,
    onOrderClick: (String) -> Unit,
    onNavigateToTransactions: (Float) -> Unit,
    onNavigateToAccountSettings: () -> Unit,
    onNavigateToAddressSettings: () -> Unit
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val userState by mainViewModel.userState.collectAsState()
    val activeRole by mainViewModel.activeRole.collectAsState()

    // Efek untuk navigasi otomatis setelah login berhasil
    LaunchedEffect(userState) {
        if (userState is UserState.Authenticated && mainViewModel.intendedRoute.value != null) {
            val route = mainViewModel.intendedRoute.value
            if (route != null) {
                if (route == SOS_ROUTE) {
                    onNavigateToConversation("admin")
                } else {
                    bottomNavController.navigate(route) {
                        popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                    }
                }
            }
            mainViewModel.intendedRoute.value = null // Reset rute tujuan
        }
    }
    LaunchedEffect(activeRole) {
        val startRoute = if (activeRole == "provider") {
            BottomNavItem.ProviderDashboard.route
        } else {
            BottomNavItem.Home.route
        }
        bottomNavController.navigate(startRoute) {
            popUpTo(bottomNavController.graph.findStartDestination().id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    val navigationItems = if (activeRole == "provider") {
        listOf(
            BottomNavItem.ProviderDashboard,
            BottomNavItem.MyOrders,
            BottomNavItem.Chats,
            BottomNavItem.Profile
        )
    } else {
        listOf(
            BottomNavItem.Home,
            BottomNavItem.MyOrders,
            BottomNavItem.Chats,
            BottomNavItem.Profile
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (userState !is UserState.Authenticated) {
                        mainViewModel.intendedRoute.value = SOS_ROUTE
                        mainNavController.navigate("login_screen")
                    } else {
                        onNavigateToConversation("admin")
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ) {
                Text(
                    "SOS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            NavigationBar {
                navigationItems.forEach { item ->
                    val isProtected = item.route in listOf("my_orders", "chats", "profile", "provider_dashboard")
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (isProtected && userState !is UserState.Authenticated) {
                                // Jika rute dilindungi dan belum login, simpan tujuan dan arahkan ke login
                                mainViewModel.intendedRoute.value = item.route
                                mainNavController.navigate("login_screen")
                            } else {
                                // Jika tidak, navigasi seperti biasa
                                bottomNavController.navigate(item.route) {
                                    popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                        label = { Text(text = item.title) }
                    )
                    if (item == BottomNavItem.MyOrders) {
                        Spacer(modifier = Modifier.width(56.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = if (activeRole == "provider") BottomNavItem.ProviderDashboard.route else BottomNavItem.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    mainViewModel = mainViewModel,
                    onCategoryClick = onCategoryClick,
                    onOrderClick = onOrderClick
                )
            }
            composable(BottomNavItem.ProviderDashboard.route) {
                val state by mainViewModel.userState.collectAsState()
                if (state is UserState.Authenticated && activeRole == "provider") {
                    ProviderDashboardScreen(activeRole = activeRole, onOrderClick = onOrderClick)
                } else {
                    LaunchedEffect(state, activeRole) {
                        if (state is UserState.Authenticated) {
                            bottomNavController.navigate(BottomNavItem.Profile.route)
                        } else {
                            mainViewModel.intendedRoute.value = BottomNavItem.ProviderDashboard.route
                            mainNavController.navigate("login_screen")
                        }
                    }
                }
            }
            composable(BottomNavItem.MyOrders.route) { MyOrdersScreen(onOrderClick = onOrderClick) }
            composable(BottomNavItem.Chats.route) { ChatListScreen(onNavigateToConversation = onNavigateToConversation) }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    mainViewModel = mainViewModel,
                    onNavigateToTransactions = onNavigateToTransactions,
                    onNavigateToAccountSettings = onNavigateToAccountSettings,
                    onNavigateToAddressSettings = onNavigateToAddressSettings
                )
            }
        }
    }
}
