package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.navigation.AppBottomNavigation
import com.example.ui.navigation.Screen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.inventory.AddEditItemScreen
import com.example.ui.screens.inventory.InventoryListScreen
import com.example.ui.screens.inventory.ItemDetailScreen
import com.example.ui.screens.invoices.CreateInvoiceScreen
import com.example.ui.screens.invoices.InvoiceListScreen
import com.example.ui.screens.more.AuditLogScreen
import com.example.ui.screens.more.MoreMenuScreen
import com.example.ui.screens.more.SuppliersScreen
import com.example.ui.screens.more.WarehousesScreen
import com.example.ui.screens.notifications.NotificationCenterScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.scan.BarcodeScanScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.InventoryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: InventoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isArabic by viewModel.isArabicLanguage.collectAsState()

            MyApplicationTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    StockMasterApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun StockMasterApp(viewModel: InventoryViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = listOf(
        Screen.Dashboard.route,
        Screen.Inventory.route,
        Screen.Scan.route,
        Screen.Invoices.route,
        Screen.More.route
    )

    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Dashboard.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                    onNavigateToAddItem = { navController.navigate(Screen.AddEditItem.createRoute()) },
                    onNavigateToCreateInvoice = { navController.navigate(Screen.CreateInvoice.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToItem = { id -> navController.navigate(Screen.ItemDetail.createRoute(id)) },
                    onNavigateToInvoices = { navController.navigate(Screen.Invoices.route) }
                )
            }

            composable(Screen.Inventory.route) {
                InventoryListScreen(
                    viewModel = viewModel,
                    onNavigateToItemDetail = { id -> navController.navigate(Screen.ItemDetail.createRoute(id)) },
                    onNavigateToAddItem = { navController.navigate(Screen.AddEditItem.createRoute()) }
                )
            }

            composable(
                route = Screen.ItemDetail.route,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                ItemDetailScreen(
                    itemId = itemId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> navController.navigate(Screen.AddEditItem.createRoute(id)) }
                )
            }

            composable(
                route = Screen.AddEditItem.route,
                arguments = listOf(navArgument("itemId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                AddEditItemScreen(
                    itemId = itemId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Scan.route) {
                BarcodeScanScreen(
                    viewModel = viewModel,
                    onNavigateToItemDetail = { id -> navController.navigate(Screen.ItemDetail.createRoute(id)) },
                    onNavigateToAddItemWithBarcode = { code ->
                        navController.navigate(Screen.AddEditItem.createRoute())
                    },
                    onNavigateToCreateInvoiceWithItem = { id ->
                        navController.navigate(Screen.CreateInvoice.route)
                    }
                )
            }

            composable(Screen.Invoices.route) {
                InvoiceListScreen(
                    viewModel = viewModel,
                    onNavigateToCreateInvoice = { navController.navigate(Screen.CreateInvoice.route) }
                )
            }

            composable(Screen.CreateInvoice.route) {
                CreateInvoiceScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(viewModel = viewModel)
            }

            composable(Screen.Notifications.route) {
                NotificationCenterScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToItem = { id -> navController.navigate(Screen.ItemDetail.createRoute(id)) }
                )
            }

            composable(Screen.More.route) {
                MoreMenuScreen(
                    viewModel = viewModel,
                    onNavigateToWarehouses = { navController.navigate(Screen.Warehouses.route) },
                    onNavigateToSuppliers = { navController.navigate(Screen.Suppliers.route) },
                    onNavigateToAuditLog = { navController.navigate(Screen.AuditLogs.route) }
                )
            }

            composable(Screen.Warehouses.route) {
                WarehousesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Suppliers.route) {
                SuppliersScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AuditLogs.route) {
                AuditLogScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
