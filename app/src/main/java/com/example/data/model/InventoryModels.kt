package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey
    val id: String,
    val sku: String,
    val barcode: String,
    val name: String,
    val category: String,
    val description: String = "",
    val unit: String = "pcs", // pcs, kg, box, pack, liter
    val costPrice: Double,
    val salePrice: Double,
    val quantity: Int,
    val minStock: Int,
    val supplier: String = "",
    val warehouse: String = "Main Warehouse",
    val expiryDate: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean get() = quantity in 1..minStock
    val isOutOfStock: Boolean get() = quantity <= 0
    val totalValuation: Double get() = quantity * costPrice
    val totalRetailValue: Double get() = quantity * salePrice
}

enum class MovementType(val title: String, val isPositive: Boolean) {
    STOCK_IN("Stock In", true),
    STOCK_OUT("Stock Out", false),
    ADJUSTMENT("Adjustment", true), // can be positive or negative delta
    RETURN("Customer Return", true)
}

@Entity(tableName = "stock_movements")
data class StockMovement(
    @PrimaryKey
    val id: String,
    val itemId: String,
    val itemName: String,
    val type: MovementType,
    val quantityChange: Int, // e.g. +10 or -5
    val previousQuantity: Int,
    val newQuantity: Int,
    val userId: String,
    val userRole: String,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class InvoiceType(val title: String) {
    SALES("Sales Invoice"),
    PURCHASE("Purchase Order")
}

enum class InvoiceStatus(val title: String) {
    DRAFT("Draft"),
    PAID("Paid"),
    UNPAID("Unpaid"),
    PARTIALLY_RETURNED("Partially Returned"),
    RETURNED("Returned"),
    CANCELLED("Cancelled")
}

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey
    val id: String,
    val invoiceNumber: String,
    val type: InvoiceType,
    val entityName: String, // Customer name (Sales) or Vendor name (Purchase)
    val entityContact: String = "",
    val subtotal: Double,
    val taxRatePercent: Double = 15.0, // default 15%
    val taxAmount: Double,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double,
    val status: InvoiceStatus = InvoiceStatus.PAID,
    val notes: String = "",
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val itemsJson: String = "" // serialized items summary
) {
    fun parseLineItems(): List<InvoiceLineItem> {
        if (itemsJson.isBlank()) return emptyList()
        val items = mutableListOf<InvoiceLineItem>()
        val entries = itemsJson.split("##")
        for (entry in entries) {
            val parsed = InvoiceLineItem.deserialize(entry.trim())
            if (parsed != null) {
                items.add(parsed)
            } else if (entry.contains("x ")) {
                val split = entry.split("x ", limit = 2)
                val qty = split[0].trim().toIntOrNull() ?: 1
                val name = split.getOrNull(1)?.trim() ?: entry
                items.add(InvoiceLineItem(itemId = "", itemName = name, sku = "", unitPrice = 0.0, quantity = qty))
            }
        }
        return items
    }
}

data class InvoiceLineItem(
    val itemId: String,
    val itemName: String,
    val sku: String,
    val unitPrice: Double,
    val quantity: Int,
    val returnedQuantity: Int = 0,
    val total: Double = unitPrice * quantity
) {
    val remainingQuantity: Int get() = (quantity - returnedQuantity).coerceAtLeast(0)

    fun serialize(): String = "$itemId@@$itemName@@$sku@@$unitPrice@@$quantity@@$returnedQuantity"

    companion object {
        fun deserialize(str: String): InvoiceLineItem? {
            val parts = str.split("@@")
            if (parts.size >= 5) {
                return InvoiceLineItem(
                    itemId = parts[0],
                    itemName = parts[1],
                    sku = parts[2],
                    unitPrice = parts[3].toDoubleOrNull() ?: 0.0,
                    quantity = parts[4].toIntOrNull() ?: 1,
                    returnedQuantity = if (parts.size >= 6) parts[5].toIntOrNull() ?: 0 else 0
                )
            }
            return null
        }
    }
}

@Entity(tableName = "warehouses")
data class Warehouse(
    @PrimaryKey
    val id: String,
    val name: String,
    val code: String,
    val location: String,
    val capacityItems: Int = 10000
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey
    val id: String,
    val name: String,
    val contactPerson: String,
    val email: String,
    val phone: String,
    val address: String
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey
    val id: String,
    val action: String, // "ITEM_CREATED", "STOCK_ADJUSTED", "INVOICE_PAID", "ROLE_CHANGED"
    val details: String,
    val userId: String,
    val userRole: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class NotificationSeverity {
    WARNING, DANGER, INFO
}

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey
    val id: String,
    val title: String,
    val body: String,
    val severity: NotificationSeverity,
    val itemId: String? = null,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String,
    val email: String = "",
    val address: String = "",
    val totalPurchasesAmount: Double = 0.0,
    val invoicesCount: Int = 0,
    val lastPurchaseDate: Long = 0L, // 0L if never or historical timestamp
    val createdAt: Long = System.currentTimeMillis()
)

