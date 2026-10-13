package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Inventory : Screen("inventory")
    object ItemDetail : Screen("item_detail/{itemId}") {
        fun createRoute(itemId: String) = "item_detail/$itemId"
    }
    object AddEditItem : Screen("add_edit_item?itemId={itemId}") {
        fun createRoute(itemId: String? = null) = if (itemId != null) "add_edit_item?itemId=$itemId" else "add_edit_item"
    }
    object Scan : Screen("scan")
    object Invoices : Screen("invoices")
    object CreateInvoice : Screen("create_invoice")
    object Reports : Screen("reports")
    object Notifications : Screen("notifications")
    object More : Screen("more")
    object Warehouses : Screen("warehouses")
    object Suppliers : Screen("suppliers")
    object Customers : Screen("customers")
    object Brands : Screen("brands")
    object AuditLogs : Screen("audit_logs")
}

