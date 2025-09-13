package com.example.posko24.ui.main

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.posko24.data.model.ProviderProfile
import com.example.posko24.data.model.User
import com.example.posko24.data.repository.UserRepository
import com.example.posko24.navigation.BottomNavItem
import com.example.posko24.navigation.SOS_ROUTE
import com.example.posko24.ui.chat.ChatListScreen
import com.example.posko24.ui.home.HomeScreen
import com.example.posko24.ui.orders.MyOrdersScreen
import com.example.posko24.ui.profile.ProfileScreen
import com.example.posko24.ui.provider.ProviderDashboardScreen
import com.example.posko24.ui.theme.Posko24Theme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    mainNavController: NavHostController,
    onCategoryClick: (String) -> Unit,
    onNavigateToConversation: (String) -> Unit,
    onOrderClick: (String) -> Unit,
    onReviewClick: (String) -> Unit,
    onNavigateToTransactions: (Float) -> Unit,
    onNavigateToAccountSettings: () -> Unit,
    onNavigateToAddressSettings: () -> Unit
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val userState by mainViewModel.userState.collectAsState()
    val activeRole by mainViewModel.activeRole.collectAsState()

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
            mainViewModel.intendedRoute.value = null
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
            if (activeRole != "provider") {
                FloatingActionButton(
                    onClick = {
                        if (userState !is UserState.Authenticated) {
                            mainViewModel.intendedRoute.value = SOS_ROUTE
                            mainNavController.navigate("login_screen")
                        } else {
                            onNavigateToConversation("admin")
                        }
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .offset(y = 32.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Text(
                        "SOS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navigationItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    val isProtected = item.route in listOf("my_orders", "chats", "profile", "provider_dashboard")
                                    if (isProtected && userState !is UserState.Authenticated) {
                                        mainViewModel.intendedRoute.value = item.route
                                        mainNavController.navigate("login_screen")
                                    } else {
                                        bottomNavController.navigate(item.route) {
                                            popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = item.title,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
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
            composable(BottomNavItem.MyOrders.route) {
                MyOrdersScreen(
                    activeRole = activeRole,
                    onOrderClick = onOrderClick,
                    onReviewClick = onReviewClick
                )
            }
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val fakeRepository = object : UserRepository {
        override fun getUserProfile(userId: String): Flow<Result<User?>> = flowOf(Result.success(null))
        override fun getProviderProfile(providerId: String): Flow<Result<ProviderProfile?>> = flowOf(Result.success(null))
        override suspend fun updateProviderAvailability(providerId: String, isAvailable: Boolean): Flow<Result<Boolean>> = flowOf(Result.success(true))
        override suspend fun updateActiveRole(userId: String, activeRole: String): Flow<Result<Boolean>> = flowOf(Result.success(true))
        override suspend fun upgradeToProvider(): Flow<Result<Boolean>> = flowOf(Result.success(true))
        override suspend fun updateUserProfile(userId: String, data: Map<String, Any?>): Result<Unit> = Result.success(Unit)
    }

    val previewViewModel = MainViewModel(
        auth = FirebaseAuth.getInstance(),
        firestore = FirebaseFirestore.getInstance(),
        userRepository = fakeRepository
    )

    Posko24Theme {
        MainScreen(
            mainViewModel = previewViewModel,
            mainNavController = rememberNavController(),
            onCategoryClick = {},
            onNavigateToConversation = {},
            onOrderClick = {},
            onReviewClick = {},
            onNavigateToTransactions = {},
            onNavigateToAccountSettings = {},
            onNavigateToAddressSettings = {}
        )
    }
}