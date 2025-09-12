package com.example.posko24

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.posko24.navigation.BottomNavItem
import com.example.posko24.ui.auth.LoginScreen
import com.example.posko24.ui.auth.RegisterScreen
import com.example.posko24.ui.chat.ConversationScreen
import com.example.posko24.ui.main.MainScreen
import com.example.posko24.ui.main.MainViewModel
import com.example.posko24.ui.order_creation.BasicOrderScreen
import com.example.posko24.ui.orders.OrderDetailScreen
import com.example.posko24.ui.orders.ReviewScreen
import com.example.posko24.ui.provider.ProviderDetailScreen
import com.example.posko24.ui.provider.ProviderListScreen
import com.example.posko24.ui.profile.TransactionHistoryScreen
import com.example.posko24.ui.profile.AccountSettingsScreen
import com.example.posko24.ui.profile.AddressSettingsScreen
import com.example.posko24.ui.theme.Posko24Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Posko24Theme(darkTheme = false) {
            Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val mainViewModel: MainViewModel = hiltViewModel()
                    val startDestination = "main_screen"

                    NavHost(navController = navController, startDestination = startDestination) {
                        composable("login_screen") {
                            LoginScreen(
                                onLoginSuccess = { navController.popBackStack() },
                                onNavigateToRegister = { navController.navigate("register_screen") }
                            )
                        }
                        composable("register_screen") {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    navController.popBackStack("main_screen", inclusive = false)
                                },
                                onNavigateToLogin = { navController.popBackStack() }
                            )
                        }
                        composable("main_screen") {
                            MainScreen(
                                mainViewModel = mainViewModel,
                                mainNavController = navController,
                                onCategoryClick = { categoryId ->
                                    navController.navigate("provider_list_screen/$categoryId")
                                },
                                onNavigateToConversation = { orderId ->
                                    navController.navigate("conversation_screen/$orderId")
                                },
                                onOrderClick = { orderId ->
                                    navController.navigate("order_detail_screen/$orderId")
                                },
                                onReviewClick = { orderId ->
                                    navController.navigate("review_screen/$orderId")
                                },
                                onNavigateToTransactions = { balance ->
                                    navController.navigate("transaction_history_screen/$balance")
                                },
                                onNavigateToAccountSettings = {
                                    navController.navigate("account_settings_screen")
                                },
                                onNavigateToAddressSettings = {
                                    navController.navigate("address_settings_screen")
                                }
                            )
                        }
                        composable(
                            route = "provider_list_screen/{categoryId}",
                            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
                            ProviderListScreen(
                                onNavigateToProviderDetail = { providerId ->
                                    navController.navigate("provider_detail_screen/$providerId")
                                },
                                onNavigateToBasicOrder = {
                                    navController.navigate("basic_order_screen/$categoryId")
                                }
                            )
                        }
                        composable(
                            route = "provider_detail_screen/{providerId}",
                            arguments = listOf(navArgument("providerId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val providerId = backStackEntry.arguments?.getString("providerId") ?: ""
                            ProviderDetailScreen(
                                onSelectService = { serviceId, categoryId ->
                                    navController.navigate("basic_order_screen/$categoryId?providerId=$providerId&serviceId=$serviceId")
                                }
                            )
                        }
                        composable(
                            route = "basic_order_screen/{categoryId}?providerId={providerId}&serviceId={serviceId}",
                            arguments = listOf(
                                navArgument("categoryId") { type = NavType.StringType },
                                navArgument("providerId") { type = NavType.StringType; nullable = true; defaultValue = null },
                                navArgument("serviceId") { type = NavType.StringType; nullable = true; defaultValue = null }
                            )
                        ) { backStackEntry ->
                            val providerIdArg = backStackEntry.arguments?.getString("providerId")
                            val serviceIdArg = backStackEntry.arguments?.getString("serviceId")
                            BasicOrderScreen(
                                providerId = providerIdArg,
                                serviceId = serviceIdArg,
                                onOrderSuccess = { orderId ->
                                    if (orderId.isNotBlank()) {
                                        navController.navigate("order_detail_screen/$orderId")
                                    }

                                }
                            )
                        }
                        composable(
                            route = "order_detail_screen/{orderId}",
                            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                        ) {
                            OrderDetailScreen(
                                onNavigateHome = {
                                    navController.navigate("main_screen") {
                                        popUpTo("main_screen") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(
                            route = "review_screen/{orderId}",
                            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                            ReviewScreen(orderId = orderId)
                        }
                        composable(
                            route = "conversation_screen/{orderId}",
                            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                        ) {
                            ConversationScreen()
                        }
                        composable(
                            route = "transaction_history_screen/{balance}",
                            arguments = listOf(navArgument("balance") { type = NavType.FloatType })
                        ) { backStackEntry ->
                            TransactionHistoryScreen(
                                balance = backStackEntry.arguments?.getFloat("balance") ?: 0f
                            )
                        }
                        composable("account_settings_screen") {
                            AccountSettingsScreen()
                        }
                        composable("address_settings_screen") {
                            AddressSettingsScreen()
                        }
                    }
                }
            }
        }
    }

}
