package com.example.data.model

enum class UserRole(
    val id: String,
    val title: String,
    val description: String,
    val canEditInventory: Boolean,
    val canAdjustStock: Boolean,
    val canCreateInvoices: Boolean,
    val canSeePurchasePrice: Boolean,
    val canManageUsers: Boolean,
    val canAccessSettings: Boolean,
    val canViewReports: Boolean,
    val isVendorOnly: Boolean
) {
    ADMIN(
        id = "admin",
        title = "Admin / Owner",
        description = "Full system access, manage roles, audit logs, and settings",
        canEditInventory = true,
        canAdjustStock = true,
        canCreateInvoices = true,
        canSeePurchasePrice = true,
        canManageUsers = true,
        canAccessSettings = true,
        canViewReports = true,
        isVendorOnly = false
    ),
    MANAGER(
        id = "manager",
        title = "Manager",
        description = "Manage stock, suppliers, warehouses, invoices & reports",
        canEditInventory = true,
        canAdjustStock = true,
        canCreateInvoices = true,
        canSeePurchasePrice = true,
        canManageUsers = false,
        canAccessSettings = true,
        canViewReports = true,
        isVendorOnly = false
    ),
    VENDOR(
        id = "vendor",
        title = "Vendor / Supplier",
        description = "View supplied products and purchase orders only",
        canEditInventory = false,
        canAdjustStock = false,
        canCreateInvoices = false,
        canSeePurchasePrice = true,
        canManageUsers = false,
        canAccessSettings = false,
        canViewReports = false,
        isVendorOnly = true
    ),
    CASHIER(
        id = "cashier",
        title = "Cashier / Sales Staff",
        description = "Barcode checkout & sales invoices only (cost price hidden)",
        canEditInventory = false,
        canAdjustStock = false,
        canCreateInvoices = true,
        canSeePurchasePrice = false,
        canManageUsers = false,
        canAccessSettings = false,
        canViewReports = false,
        isVendorOnly = false
    ),
    AUDITOR(
        id = "auditor",
        title = "Viewer / Auditor",
        description = "Read-only access to inventory movements and audit reports",
        canEditInventory = false,
        canAdjustStock = false,
        canCreateInvoices = false,
        canSeePurchasePrice = true,
        canManageUsers = false,
        canAccessSettings = false,
        canViewReports = true,
        isVendorOnly = false
    );

    companion object {
        fun fromId(id: String): UserRole = entries.find { it.id == id } ?: ADMIN
    }
}

data class CurrentUser(
    val uid: String = "usr_admin_01",
    val name: String = "Alexander Vance",
    val email: String = "alex.vance@stockmaster.app",
    val role: UserRole = UserRole.ADMIN,
    val assignedVendorName: String = "Apex Tech Supplies" // For Vendor role filtering
)
