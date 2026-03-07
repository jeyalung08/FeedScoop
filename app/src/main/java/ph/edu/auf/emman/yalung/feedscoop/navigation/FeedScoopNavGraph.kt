// FILE: navigation/FeedScoopNavGraph.kt
package ph.edu.auf.emman.yalung.feedscoop.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ph.edu.auf.emman.yalung.feedscoop.ui.screens.*
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.DeviceViewModel
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.InventoryViewModel
import ph.edu.auf.emman.yalung.feedscoop.ui.viewmodel.OrderViewModel

@Composable
fun FeedScoopNavGraph(navController: NavHostController = rememberNavController()) {

    // All ViewModels created ONCE here and passed explicitly to every screen.
    // This guarantees:
    //   - One Firebase listener for inventory (InventoryViewModel)
    //   - One Firebase listener for orders (OrderViewModel)
    //   - One BLE/device state shared across Device, Calibration, and Weighing (DeviceViewModel)
    val inventoryViewModel: InventoryViewModel = hiltViewModel()
    val orderViewModel: OrderViewModel         = hiltViewModel()
    val deviceViewModel: DeviceViewModel       = hiltViewModel()

    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen(navController)
        }

        composable("dashboard") {
            DashboardScreen(navController)
        }

        // ── Products ────────────────────────────────────────────────
        composable("available_products") {
            AvailableProductsScreen(navController, inventoryViewModel)
        }

        composable(
            route = "add_edit_product?productId={productId}",
            arguments = listOf(navArgument("productId") {
                type = NavType.StringType
                defaultValue = ""
                nullable = true
            })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            AddEditProductScreen(
                navController      = navController,
                productId          = productId,
                inventoryViewModel = inventoryViewModel
            )
        }

        // ── Order Processing ────────────────────────────────────────
        composable(
            route = "real_time_weighing/{productId}/{productName}/{brand}/{pricePerKilo}",
            arguments = listOf(
                navArgument("productId")    { type = NavType.StringType },
                navArgument("productName")  { type = NavType.StringType },
                navArgument("brand")        { type = NavType.StringType },
                navArgument("pricePerKilo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId    = backStackEntry.arguments?.getString("productId")    ?: ""
            val productName  = backStackEntry.arguments?.getString("productName")  ?: ""
            val brand        = backStackEntry.arguments?.getString("brand")        ?: ""
            val pricePerKilo = backStackEntry.arguments?.getString("pricePerKilo")
                ?.toDoubleOrNull() ?: 0.0
            RealTimeWeighingScreen(
                navController      = navController,
                productId          = productId,
                productName        = productName,
                brand              = brand,
                pricePerKilo       = pricePerKilo,
                orderViewModel     = orderViewModel,
                deviceViewModel    = deviceViewModel,      // FIX: shared instance
                inventoryViewModel = inventoryViewModel    // FIX: shared instance
            )
        }

        composable("order_result_popup") {
            OrderResultPopup(navController, orderViewModel)
        }

        composable("order_summary") {
            OrderSummaryScreen(navController, orderViewModel)
        }

        // ── Inventory ───────────────────────────────────────────────
        composable("inventory_management") {
            InventoryManagementScreen(navController, inventoryViewModel)
        }

        // ── History & Analytics ─────────────────────────────────────
        composable("orders_history") {
            OrdersHistoryScreen(navController, orderViewModel)
        }

        composable("analytics") {
            AnalyticsScreen(navController, orderViewModel)
        }

        // ── Device / Settings ───────────────────────────────────────
        composable("device_connection") {
            DeviceConnectionScreen(navController, deviceViewModel)  // FIX: shared instance
        }

        composable("calibration") {
            CalibrationScreen(navController, deviceViewModel)        // FIX: shared instance
        }

        composable("settings") {
            SettingsScreen(navController)
        }

        composable("reports_export") {
            ReportsExportScreen(navController, orderViewModel)
        }

        composable("error_offline") {
            ErrorScreen(navController)
        }

        composable("loading_state") {
            LoadingScreen(navController)
        }
    }
}