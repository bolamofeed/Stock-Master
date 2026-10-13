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
    val brandsJson: String = "",
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
    val total: Double = unitPrice * quantity,
    val brandId: String = "",
    val brandName: String = "",
    val unitPriceMinor: Long = CurrencyUtils.toMinorUnits(unitPrice)
) {
    val remainingQuantity: Int get() = (quantity - returnedQuantity).coerceAtLeast(0)

    fun serialize(): String = "$itemId@@$itemName@@$sku@@$unitPrice@@$quantity@@$returnedQuantity@@$brandId@@$brandName@@$unitPriceMinor"

    companion object {
        fun deserialize(str: String): InvoiceLineItem? {
            val parts = str.split("@@")
            if (parts.size >= 5) {
                val unitPrice = parts[3].toDoubleOrNull() ?: 0.0
                val quantity = parts[4].toIntOrNull() ?: 1
                val retQty = if (parts.size >= 6) parts[5].toIntOrNull() ?: 0 else 0
                val brandId = if (parts.size >= 7) parts[6] else ""
                val brandName = if (parts.size >= 8) parts[7] else ""
                val minor = if (parts.size >= 9) parts[8].toLongOrNull() ?: CurrencyUtils.toMinorUnits(unitPrice) else CurrencyUtils.toMinorUnits(unitPrice)
                return InvoiceLineItem(
                    itemId = parts[0],
                    itemName = parts[1],
                    sku = parts[2],
                    unitPrice = unitPrice,
                    quantity = quantity,
                    returnedQuantity = retQty,
                    total = unitPrice * quantity,
                    brandId = brandId,
                    brandName = brandName,
                    unitPriceMinor = minor
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
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val lastContactedAt: Long = 0L
)

@Entity(
    tableName = "brands",
    indices = [
        androidx.room.Index(value = ["nameLower"], unique = true)
    ]
)
data class Brand(
    @PrimaryKey
    val id: String,
    val name: String,
    val nameLower: String,
    val defaultCostPriceMinor: Long, // in minor units / piastres (e.g. 1500 = 15.00)
    val defaultSalePriceMinor: Long, // in minor units / piastres (e.g. 2500 = 25.00)
    val status: Int = 1, // 1 = Active, 0 = Deactivated
    val itemCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isActive: Boolean get() = status == 1
    val defaultCostPriceMajor: Double get() = defaultCostPriceMinor / 100.0
    val defaultSalePriceMajor: Double get() = defaultSalePriceMinor / 100.0
}

@Entity(
    tableName = "item_brands",
    indices = [
        androidx.room.Index(value = ["barcode"]),
        androidx.room.Index(value = ["itemId"]),
        androidx.room.Index(value = ["brandId"])
    ]
)
data class ItemBrand(
    @PrimaryKey
    val id: String,
    val itemId: String,
    val brandId: String,
    val brandName: String,
    val barcode: String,
    val costPriceMinor: Long, // in minor units / piastres
    val salePriceMinor: Long, // in minor units / piastres
    val quantity: Int,
    val minStock: Int = 5,
    val status: Int = 1
) {
    val costPriceMajor: Double get() = costPriceMinor / 100.0
    val salePriceMajor: Double get() = salePriceMinor / 100.0
    val isLowStock: Boolean get() = quantity in 1..minStock
    val isOutOfStock: Boolean get() = quantity <= 0

    fun serialize(): String = "$id@@$itemId@@$brandId@@$brandName@@$barcode@@$costPriceMinor@@$salePriceMinor@@$quantity@@$minStock@@$status"

    companion object {
        fun deserialize(str: String): ItemBrand? {
            val parts = str.split("@@")
            if (parts.size >= 10) {
                return ItemBrand(
                    id = parts[0],
                    itemId = parts[1],
                    brandId = parts[2],
                    brandName = parts[3],
                    barcode = parts[4],
                    costPriceMinor = parts[5].toLongOrNull() ?: 0L,
                    salePriceMinor = parts[6].toLongOrNull() ?: 0L,
                    quantity = parts[7].toIntOrNull() ?: 0,
                    minStock = parts[8].toIntOrNull() ?: 5,
                    status = parts[9].toIntOrNull() ?: 1
                )
            }
            return null
        }
    }
}

object CurrencyUtils {
    fun toMinorUnits(major: Double): Long = Math.round(major * 100.0)
    fun toMajorUnits(minor: Long): Double = minor / 100.0

    fun formatMinorUnits(minor: Long, symbol: String = "$"): String {
        return "%s%.2f".format(symbol, minor / 100.0)
    }

    fun parseToMinorUnits(input: String): Long {
        val clean = input.trim().replace(",", "")
        val doubleVal = clean.toDoubleOrNull() ?: 0.0
        return Math.round(doubleVal * 100.0)
    }
}



