package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.AppNotification
import com.example.data.model.AuditLog
import com.example.data.model.Client
import com.example.data.model.Invoice
import com.example.data.model.InvoiceLineItem
import com.example.data.model.InvoiceStatus
import com.example.data.model.InvoiceType
import com.example.data.model.Item
import com.example.data.model.MovementType
import com.example.data.model.NotificationSeverity
import com.example.data.model.StockMovement
import com.example.data.model.Supplier
import com.example.data.model.UserRole
import com.example.data.model.Warehouse
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class InventoryRepository(private val db: AppDatabase) {

    val brandRepository = BrandRepository(db)
    val allBrands: Flow<List<com.example.data.model.Brand>> = brandRepository.allBrands
    val activeBrands: Flow<List<com.example.data.model.Brand>> = brandRepository.activeBrands

    val allItems: Flow<List<Item>> = db.itemDao().getAllItems()
    val lowStockItems: Flow<List<Item>> = db.itemDao().getLowStockItems()
    val allMovements: Flow<List<StockMovement>> = db.stockMovementDao().getAllMovements()
    val allInvoices: Flow<List<Invoice>> = db.invoiceDao().getAllInvoices()
    val allWarehouses: Flow<List<Warehouse>> = db.warehouseDao().getAllWarehouses()
    val allSuppliers: Flow<List<Supplier>> = db.supplierDao().getAllSuppliers()
    val allClients: Flow<List<Client>> = db.clientDao().getAllClients()
    val allAuditLogs: Flow<List<AuditLog>> = db.auditLogDao().getAllAuditLogs()
    val allNotifications: Flow<List<AppNotification>> = db.notificationDao().getAllNotifications()
    val unreadNotificationsCount: Flow<Int> = db.notificationDao().getUnreadCount()

    suspend fun saveClient(client: Client) {
        db.clientDao().insertClient(client)
    }

    suspend fun updateClient(client: Client) {
        db.clientDao().updateClient(client)
    }

    suspend fun softDeleteClient(clientId: String, user: String = "Admin", role: String = "ADMIN") {
        val client = db.clientDao().getClientById(clientId) ?: return
        db.clientDao().updateClient(client.copy(isDeleted = true))
        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            action = "CUSTOMER_DELETED",
            details = "Soft deleted customer: ${client.name} (${client.phone})",
            userId = user,
            userRole = role
        )
        db.auditLogDao().insertAuditLog(log)
    }

    suspend fun markClientContacted(clientId: String) {
        val client = db.clientDao().getClientById(clientId) ?: return
        db.clientDao().updateClient(client.copy(lastContactedAt = System.currentTimeMillis()))
    }

    fun getInvoicesForCustomer(name: String, phone: String): Flow<List<Invoice>> =
        db.invoiceDao().getSalesInvoicesForClient(name, phone)

    suspend fun recordOrUpdateClientFromInvoice(name: String, phone: String, invoiceAmount: Double) {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        if (cleanName.isBlank()) return

        db.withTransaction {
            val existing = db.clientDao().getClientByName(cleanName)
                ?: if (cleanPhone.isNotBlank()) db.clientDao().getClientByPhone(cleanPhone) else null

            val now = System.currentTimeMillis()
            if (existing != null) {
                val updated = existing.copy(
                    phone = if (cleanPhone.isNotBlank()) cleanPhone else existing.phone,
                    totalPurchasesAmount = existing.totalPurchasesAmount + invoiceAmount,
                    invoicesCount = existing.invoicesCount + 1,
                    lastPurchaseDate = now,
                    isDeleted = false
                )
                db.clientDao().updateClient(updated)
            } else {
                val newClient = Client(
                    id = UUID.randomUUID().toString(),
                    name = cleanName,
                    phone = cleanPhone,
                    totalPurchasesAmount = invoiceAmount,
                    invoicesCount = 1,
                    lastPurchaseDate = now,
                    createdAt = now,
                    isDeleted = false
                )
                db.clientDao().insertClient(newClient)
            }
        }
    }

    suspend fun getItemById(id: String): Item? = db.itemDao().getItemById(id)

    suspend fun getItemByBarcodeOrSku(barcode: String): Item? =
        db.itemDao().getItemByBarcodeOrSku(barcode.trim())

    suspend fun saveItem(item: Item, user: String, role: String, isNew: Boolean) {
        db.itemDao().insertItem(item)

        val action = if (isNew) "ITEM_CREATED" else "ITEM_UPDATED"
        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            action = action,
            details = "${if (isNew) "Added" else "Updated"} item ${item.name} (${item.sku}) - Qty: ${item.quantity}",
            userId = user,
            userRole = role
        )
        db.auditLogDao().insertAuditLog(log)

        checkAndTriggerLowStockAlert(item)
    }

    suspend fun saveItemWithBrands(
        item: Item,
        brands: List<com.example.data.model.ItemBrand>,
        user: String,
        role: String,
        isNew: Boolean
    ) {
        val totalQty = if (brands.isNotEmpty()) brands.sumOf { it.quantity } else item.quantity
        val serializedBrands = brands.joinToString("##") { it.serialize() }
        val itemToSave = item.copy(
            quantity = totalQty,
            brandsJson = serializedBrands,
            updatedAt = System.currentTimeMillis()
        )

        db.withTransaction {
            db.itemDao().insertItem(itemToSave)
            brandRepository.saveItemBrands(item.id, brands, user, role)
        }

        val action = if (isNew) "ITEM_CREATED" else "ITEM_UPDATED"
        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            action = action,
            details = "${if (isNew) "Added" else "Updated"} item ${itemToSave.name} (${itemToSave.sku}) with ${brands.size} brands - Total Qty: $totalQty",
            userId = user,
            userRole = role
        )
        db.auditLogDao().insertAuditLog(log)

        checkAndTriggerLowStockAlert(itemToSave)
    }

    suspend fun deleteItem(item: Item, user: String, role: String) {
        db.itemDao().deleteItem(item)
        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            action = "ITEM_DELETED",
            details = "Deleted product ${item.name} (${item.sku})",
            userId = user,
            userRole = role
        )
        db.auditLogDao().insertAuditLog(log)
    }

    suspend fun recordStockMovement(
        item: Item,
        type: MovementType,
        quantityChange: Int, // positive number of units
        user: String,
        role: String,
        note: String
    ): Result<Item> {
        val currentQty = item.quantity
        val newQty = when (type) {
            MovementType.STOCK_IN, MovementType.RETURN -> currentQty + quantityChange
            MovementType.STOCK_OUT -> currentQty - quantityChange
            MovementType.ADJUSTMENT -> quantityChange // direct set
        }

        if (newQty < 0) {
            return Result.failure(IllegalArgumentException("Insufficient stock! Available: $currentQty"))
        }

        val delta = when (type) {
            MovementType.STOCK_IN, MovementType.RETURN -> quantityChange
            MovementType.STOCK_OUT -> -quantityChange
            MovementType.ADJUSTMENT -> newQty - currentQty
        }

        val updatedItem = item.copy(quantity = newQty, updatedAt = System.currentTimeMillis())
        db.itemDao().updateItem(updatedItem)

        val movement = StockMovement(
            id = UUID.randomUUID().toString(),
            itemId = item.id,
            itemName = item.name,
            type = type,
            quantityChange = delta,
            previousQuantity = currentQty,
            newQuantity = newQty,
            userId = user,
            userRole = role,
            note = note.ifEmpty { "${type.title} movement" }
        )
        db.stockMovementDao().insertMovement(movement)

        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            action = "STOCK_MOVED",
            details = "${type.title} for ${item.name}: $currentQty -> $newQty ($note)",
            userId = user,
            userRole = role
        )
        db.auditLogDao().insertAuditLog(log)

        checkAndTriggerLowStockAlert(updatedItem)

        return Result.success(updatedItem)
    }

    suspend fun createInvoice(
        type: InvoiceType,
        entityName: String,
        entityContact: String,
        lineItems: List<InvoiceLineItem>,
        taxPercent: Double,
        discountPercent: Double,
        notes: String,
        user: String,
        role: String,
        markAsPaid: Boolean = true,
        saveCustomer: Boolean = true
    ): Result<Invoice> {
        if (lineItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("Invoice must contain at least 1 item"))
        }

        val subtotal = lineItems.sumOf { it.total }
        val discountAmount = subtotal * (discountPercent / 100.0)
        val discountedSubtotal = subtotal - discountAmount
        val taxAmount = discountedSubtotal * (taxPercent / 100.0)
        val total = discountedSubtotal + taxAmount

        val invoiceNumber = "${if (type == InvoiceType.SALES) "INV" else "PO"}-${System.currentTimeMillis() % 1000000}"

        val summaryItems = lineItems.joinToString("##") { it.serialize() }

        val invoice = Invoice(
            id = UUID.randomUUID().toString(),
            invoiceNumber = invoiceNumber,
            type = type,
            entityName = entityName.ifEmpty { if (type == InvoiceType.SALES) "Cash Customer" else "Main Supplier" },
            entityContact = entityContact,
            subtotal = subtotal,
            taxRatePercent = taxPercent,
            taxAmount = taxAmount,
            discountPercent = discountPercent,
            discountAmount = discountAmount,
            totalAmount = total,
            status = if (markAsPaid) InvoiceStatus.PAID else InvoiceStatus.UNPAID,
            notes = notes,
            createdBy = user,
            createdAt = System.currentTimeMillis(),
            itemsJson = summaryItems
        )

        db.withTransaction {
            db.invoiceDao().insertInvoice(invoice)

            // Transactionally create or update client record when saveCustomer toggle is active
            if (type == InvoiceType.SALES && saveCustomer) {
                recordOrUpdateClientFromInvoice(invoice.entityName, invoice.entityContact, invoice.totalAmount)
            }
        }

        // If confirmed/paid, update inventory automatically
        if (markAsPaid) {
            applyInvoiceStockUpdate(invoice, lineItems, user, role)
        }

        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            action = "INVOICE_CREATED",
            details = "${invoice.type.title} $invoiceNumber created for ${invoice.entityName} - Total: $%.2f".format(total),
            userId = user,
            userRole = role
        )
        db.auditLogDao().insertAuditLog(log)

        return Result.success(invoice)
    }

    suspend fun payInvoice(
        invoiceId: String,
        paymentMethod: String,
        user: String,
        role: String
    ): Result<Invoice> {
        val invoice = db.invoiceDao().getInvoiceById(invoiceId)
            ?: return Result.failure(IllegalArgumentException("Invoice not found"))

        if (invoice.status == InvoiceStatus.PAID) {
            return Result.success(invoice)
        }

        val updatedInvoice = invoice.copy(status = InvoiceStatus.PAID)
        db.invoiceDao().updateInvoice(updatedInvoice)

        // Deduct/Add stock if invoice was unpaid
        val items = invoice.parseLineItems()
        applyInvoiceStockUpdate(updatedInvoice, items, user, role)

        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            action = "INVOICE_PAID",
            details = "Invoice #${invoice.invoiceNumber} marked PAID via $paymentMethod. Total: $%.2f".format(invoice.totalAmount),
            userId = user,
            userRole = role
        )
        db.auditLogDao().insertAuditLog(log)

        return Result.success(updatedInvoice)
    }

    suspend fun returnInvoice(
        invoiceId: String,
        itemsToReturn: List<Pair<String, Int>>,
        reason: String,
        user: String,
        role: String
    ): Result<Invoice> {
        val invoice = db.invoiceDao().getInvoiceById(invoiceId)
            ?: return Result.failure(IllegalArgumentException("Invoice not found"))

        val currentLineItems = invoice.parseLineItems().toMutableList()
        if (currentLineItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("No items found in this invoice to return"))
        }

        var totalReturnedDelta = 0

        for ((itemId, returnQty) in itemsToReturn) {
            if (returnQty <= 0) continue
            val index = currentLineItems.indexOfFirst { it.itemId == itemId }
            if (index >= 0) {
                val current = currentLineItems[index]
                val allowedReturn = (current.quantity - current.returnedQuantity).coerceAtLeast(0)
                val actualReturnQty = returnQty.coerceAtMost(allowedReturn)

                if (actualReturnQty > 0) {
                    currentLineItems[index] = current.copy(
                        returnedQuantity = current.returnedQuantity + actualReturnQty
                    )
                    totalReturnedDelta += actualReturnQty

                    // Restock inventory in the warehouse
                    val item = db.itemDao().getItemById(itemId)
                    if (item != null) {
                        val movementType = if (invoice.type == InvoiceType.SALES) MovementType.RETURN else MovementType.STOCK_OUT
                        recordStockMovement(
                            item = item,
                            type = movementType,
                            quantityChange = actualReturnQty,
                            user = user,
                            role = role,
                            note = "Return: ${invoice.invoiceNumber} ($reason)"
                        )
                    }
                }
            }
        }

        if (totalReturnedDelta == 0) {
            return Result.failure(IllegalArgumentException("No valid quantities were selected for return"))
        }

        val allFullyReturned = currentLineItems.all { it.remainingQuantity == 0 }
        val newStatus = if (allFullyReturned) InvoiceStatus.RETURNED else InvoiceStatus.PARTIALLY_RETURNED

        val updatedItemsJson = currentLineItems.joinToString("##") { it.serialize() }
        val updatedInvoice = invoice.copy(
            status = newStatus,
            itemsJson = updatedItemsJson
        )
        db.invoiceDao().updateInvoice(updatedInvoice)

        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            action = "INVOICE_RETURNED",
            details = "Processed ${if (allFullyReturned) "Full" else "Partial"} return ($totalReturnedDelta items) for #${invoice.invoiceNumber}. Reason: $reason",
            userId = user,
            userRole = role
        )
        db.auditLogDao().insertAuditLog(log)

        return Result.success(updatedInvoice)
    }

    private suspend fun applyInvoiceStockUpdate(
        invoice: Invoice,
        lineItems: List<InvoiceLineItem>,
        user: String,
        role: String
    ) {
        for (line in lineItems) {
            val item = db.itemDao().getItemById(line.itemId) ?: continue
            val isSales = invoice.type == InvoiceType.SALES
            val delta = if (isSales) -line.quantity else line.quantity

            if (line.brandId.isNotBlank()) {
                val itemBrands = db.itemBrandDao().getItemBrandsByItemId(item.id)
                val targetBrand = itemBrands.find { it.brandId == line.brandId }
                if (targetBrand != null) {
                    val newBrandQty = (targetBrand.quantity + delta).coerceAtLeast(0)
                    db.itemBrandDao().updateItemBrandQuantity(targetBrand.id, newBrandQty)

                    // Track low-stock alert per brand
                    if (newBrandQty <= targetBrand.minStock) {
                        val brandAlert = AppNotification(
                            id = UUID.randomUUID().toString(),
                            title = if (newBrandQty == 0) "Brand Out of Stock" else "Brand Low Stock Warning",
                            body = "Brand '${line.brandName}' for product '${item.name}' has $newBrandQty units left (Min: ${targetBrand.minStock}).",
                            severity = if (newBrandQty == 0) NotificationSeverity.DANGER else NotificationSeverity.WARNING,
                            itemId = item.id
                        )
                        db.notificationDao().insertNotification(brandAlert)
                    }
                }
                // Roll up quantity per item
                val updatedBrands = db.itemBrandDao().getItemBrandsByItemId(item.id)
                val totalQty = updatedBrands.sumOf { it.quantity }
                val updatedItem = item.copy(quantity = totalQty, updatedAt = System.currentTimeMillis())
                db.itemDao().updateItem(updatedItem)
                checkAndTriggerLowStockAlert(updatedItem)
            } else {
                val movementType = if (isSales) MovementType.STOCK_OUT else MovementType.STOCK_IN
                recordStockMovement(
                    item = item,
                    type = movementType,
                    quantityChange = line.quantity,
                    user = user,
                    role = role,
                    note = "Automated from ${invoice.invoiceNumber}"
                )
            }
        }
    }

    private suspend fun checkAndTriggerLowStockAlert(item: Item) {
        if (item.isOutOfStock) {
            val notification = AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Out of Stock Alert!",
                body = "${item.name} (${item.sku}) is completely OUT OF STOCK (0 units left).",
                severity = NotificationSeverity.DANGER,
                itemId = item.id
            )
            db.notificationDao().insertNotification(notification)
        } else if (item.isLowStock) {
            val notification = AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Low Stock Warning",
                body = "${item.name} (${item.sku}) has reached ${item.quantity} ${item.unit} (Min limit: ${item.minStock}).",
                severity = NotificationSeverity.WARNING,
                itemId = item.id
            )
            db.notificationDao().insertNotification(notification)
        }
    }

    suspend fun markNotificationRead(id: String) = db.notificationDao().markAsRead(id)
    suspend fun markAllNotificationsRead() = db.notificationDao().markAllAsRead()
    suspend fun clearNotifications() = db.notificationDao().clearAll()

    suspend fun checkAndSeedInitialData() {
        val currentItems = db.itemDao().getAllItems().first()
        if (currentItems.isNotEmpty()) return

        // Preload Warehouses
        val warehouses = listOf(
            Warehouse("wh_1", "Main Central Hub", "WH-CENTRAL", "Building 4, Industrial Zone, New York", 25000),
            Warehouse("wh_2", "West Coast Depot", "WH-WEST", "Sector 9, Docklands, San Francisco", 15000),
            Warehouse("wh_3", "Showroom Floor", "WH-STORE", "Retail Ave Suite 102", 5000)
        )
        db.warehouseDao().insertWarehouses(warehouses)

        // Preload Suppliers
        val suppliers = listOf(
            Supplier("sup_1", "Apex Tech Supplies", "Marcus Reed", "m.reed@apextech.io", "+1 (555) 019-2831", "San Jose, CA"),
            Supplier("sup_2", "Global Nordic Goods", "Astrid Lind", "astrid@nordicgoods.se", "+1 (555) 832-9901", "Stockholm, Sweden"),
            Supplier("sup_3", "Quantum Logistics Ltd", "David Chen", "chen@quantumlog.com", "+1 (555) 441-2099", "Singapore Hub")
        )
        db.supplierDao().insertSuppliers(suppliers)

        // Preload Realistic Items
        val items = listOf(
            Item(
                id = "item_1",
                sku = "TECH-LPT-16",
                barcode = "8901234567890",
                name = "ProBook Ultra 16\"",
                category = "Electronics",
                description = "High-performance Intel i9 32GB RAM developer laptop with 1TB SSD",
                unit = "pcs",
                costPrice = 1150.00,
                salePrice = 1699.00,
                quantity = 14,
                minStock = 5,
                supplier = "Apex Tech Supplies",
                warehouse = "Main Central Hub",
                expiryDate = ""
            ),
            Item(
                id = "item_2",
                sku = "TECH-MOU-WL",
                barcode = "8901234567891",
                name = "ErgoLaser Wireless Mouse",
                category = "Accessories",
                description = "Rechargeable Bluetooth ergonomic mouse with silent switches",
                unit = "pcs",
                costPrice = 22.50,
                salePrice = 49.99,
                quantity = 4, // Low stock!
                minStock = 10,
                supplier = "Apex Tech Supplies",
                warehouse = "Showroom Floor",
                expiryDate = ""
            ),
            Item(
                id = "item_3",
                sku = "OFF-DSK-ADJ",
                barcode = "8901234567892",
                name = "Motorized Standing Desk",
                category = "Furniture",
                description = "Dual-motor height adjustable solid oak desk 160x80cm",
                unit = "pcs",
                costPrice = 280.00,
                salePrice = 549.00,
                quantity = 8,
                minStock = 3,
                supplier = "Global Nordic Goods",
                warehouse = "West Coast Depot",
                expiryDate = ""
            ),
            Item(
                id = "item_4",
                sku = "NET-CAB-C6",
                barcode = "8901234567893",
                name = "Cat6 Shielded Cable 50m",
                category = "Networking",
                description = "Pure copper 10Gbps high speed ethernet spool",
                unit = "spool",
                costPrice = 18.00,
                salePrice = 35.00,
                quantity = 0, // Out of stock!
                minStock = 8,
                supplier = "Quantum Logistics Ltd",
                warehouse = "Main Central Hub",
                expiryDate = ""
            ),
            Item(
                id = "item_5",
                sku = "AUD-HDP-ANC",
                barcode = "8901234567894",
                name = "AcousticStudio ANC Headset",
                category = "Audio",
                description = "Active noise cancelling studio monitor headphones with spatial audio",
                unit = "pcs",
                costPrice = 110.00,
                salePrice = 219.00,
                quantity = 22,
                minStock = 6,
                supplier = "Apex Tech Supplies",
                warehouse = "Main Central Hub",
                expiryDate = ""
            ),
            Item(
                id = "item_6",
                sku = "SUP-BAT-LIO",
                barcode = "8901234567895",
                name = "Industrial LiFePO4 12V Battery",
                category = "Power",
                description = "100Ah deep cycle backup power module for server racks",
                unit = "pcs",
                costPrice = 320.00,
                salePrice = 480.00,
                quantity = 3, // Low stock!
                minStock = 5,
                supplier = "Quantum Logistics Ltd",
                warehouse = "Main Central Hub",
                expiryDate = "2029-12-31"
            )
        )
        db.itemDao().insertItems(items)

        // Preload Initial Movements
        val movements = listOf(
            StockMovement(
                id = UUID.randomUUID().toString(),
                itemId = "item_1",
                itemName = "ProBook Ultra 16\"",
                type = MovementType.STOCK_IN,
                quantityChange = 20,
                previousQuantity = 0,
                newQuantity = 20,
                userId = "Alexander Vance",
                userRole = "ADMIN",
                note = "Initial stock intake batch #481"
            ),
            StockMovement(
                id = UUID.randomUUID().toString(),
                itemId = "item_1",
                itemName = "ProBook Ultra 16\"",
                type = MovementType.STOCK_OUT,
                quantityChange = -6,
                previousQuantity = 20,
                newQuantity = 14,
                userId = "Marcus Cashier",
                userRole = "CASHIER",
                note = "Sales Invoice #INV-2026-101"
            ),
            StockMovement(
                id = UUID.randomUUID().toString(),
                itemId = "item_2",
                itemName = "ErgoLaser Wireless Mouse",
                type = MovementType.STOCK_OUT,
                quantityChange = -16,
                previousQuantity = 20,
                newQuantity = 4,
                userId = "Marcus Cashier",
                userRole = "CASHIER",
                note = "Bulk company workstation order"
            )
        )
        db.stockMovementDao().insertMovements(movements)

        // Preload Sample Invoices
        val sampleInvoice = Invoice(
            id = UUID.randomUUID().toString(),
            invoiceNumber = "INV-2026-101",
            type = InvoiceType.SALES,
            entityName = "Horizon Design Studios",
            entityContact = "+1 (555) 721-0022",
            subtotal = 10194.00,
            taxRatePercent = 15.0,
            taxAmount = 1529.10,
            discountPercent = 5.0,
            discountAmount = 509.70,
            totalAmount = 11213.40,
            status = InvoiceStatus.PAID,
            notes = "Delivered to Horizon HQ with warranty papers.",
            createdBy = "Alexander Vance",
            createdAt = System.currentTimeMillis() - 86400000L * 2,
            itemsJson = InvoiceLineItem("item_1", "ProBook Ultra 16\"", "TECH-LPT-16", 1699.00, 6).serialize()
        )

        val sampleUnpaidInvoice = Invoice(
            id = UUID.randomUUID().toString(),
            invoiceNumber = "INV-2026-102",
            type = InvoiceType.SALES,
            entityName = "Vanguard Creative Labs",
            entityContact = "+1 (555) 349-8812",
            subtotal = 99.98,
            taxRatePercent = 15.0,
            taxAmount = 15.00,
            discountPercent = 0.0,
            discountAmount = 0.0,
            totalAmount = 114.98,
            status = InvoiceStatus.UNPAID,
            notes = "Pending Net-30 payment clearance.",
            createdBy = "Sarah Jenkins",
            createdAt = System.currentTimeMillis() - 86400000L * 1,
            itemsJson = InvoiceLineItem("item_2", "ErgoLaser Wireless Mouse", "TECH-MOU-WL", 49.99, 2).serialize()
        )
        db.invoiceDao().insertInvoices(listOf(sampleInvoice, sampleUnpaidInvoice))

        // Preload Sample Clients (including active and clients inactive for > 3 months)
        val now = System.currentTimeMillis()
        val oneMonth = 30L * 24L * 60L * 60L * 1000L
        val initialClients = listOf(
            Client(
                id = UUID.randomUUID().toString(),
                name = "Horizon Design Studios",
                phone = "+1 (555) 721-0022",
                email = "billing@horizondesign.io",
                address = "742 Evergreen Terrace, Tech District",
                totalPurchasesAmount = 11213.40,
                invoicesCount = 3,
                lastPurchaseDate = now - 86400000L * 2, // 2 days ago (Active)
                createdAt = now - oneMonth * 5
            ),
            Client(
                id = UUID.randomUUID().toString(),
                name = "Vanguard Creative Labs",
                phone = "+1 (555) 349-8812",
                email = "operations@vanguardlabs.co",
                address = "120 Innovation Way, Suite 400",
                totalPurchasesAmount = 114.98,
                invoicesCount = 1,
                lastPurchaseDate = now - 86400000L * 1, // 1 day ago (Active)
                createdAt = now - oneMonth * 2
            ),
            Client(
                id = UUID.randomUUID().toString(),
                name = "Apex Logistics International",
                phone = "+1 (555) 902-1433",
                email = "procurement@apexlogistics.com",
                address = "88 Harbor Boulevard, Cargo Terminal B",
                totalPurchasesAmount = 4520.00,
                invoicesCount = 2,
                lastPurchaseDate = now - (oneMonth * 4), // 4 months ago (> 3 months inactive!)
                createdAt = now - (oneMonth * 8)
            ),
            Client(
                id = UUID.randomUUID().toString(),
                name = "Silverline Retail Group",
                phone = "+1 (555) 674-8891",
                email = "orders@silverlinestores.net",
                address = "400 Commercial Center, Mall Level 2",
                totalPurchasesAmount = 8340.50,
                invoicesCount = 4,
                lastPurchaseDate = now - (oneMonth * 5), // 5 months ago (> 3 months inactive!)
                createdAt = now - (oneMonth * 10)
            ),
            Client(
                id = UUID.randomUUID().toString(),
                name = "BlueSky Coworking Hub",
                phone = "+1 (555) 412-7700",
                email = "hello@blueskyhub.space",
                address = "15 Silicon Row, Floor 3",
                totalPurchasesAmount = 1890.00,
                invoicesCount = 1,
                lastPurchaseDate = now - (oneMonth * 3 + 86400000L * 15), // ~3.5 months ago (> 3 months inactive!)
                createdAt = now - (oneMonth * 6)
            ),
            Client(
                id = UUID.randomUUID().toString(),
                name = "Metro Tech Repairs",
                phone = "+1 (555) 233-9011",
                email = "info@metrotechrepairs.com",
                address = "909 Main St",
                totalPurchasesAmount = 670.00,
                invoicesCount = 1,
                lastPurchaseDate = now - (oneMonth * 1), // 1 month ago (Active)
                createdAt = now - (oneMonth * 3)
            ),
            Client(
                id = UUID.randomUUID().toString(),
                name = "Delta Electronics Corp",
                phone = "+1 (555) 881-2299",
                email = "contact@deltaelec.org",
                address = "55 Industrial Boulevard, Zone 4",
                totalPurchasesAmount = 14200.00,
                invoicesCount = 5,
                lastPurchaseDate = now - (oneMonth * 13), // 13 months ago (> 12 months inactive!)
                createdAt = now - (oneMonth * 18)
            )
        )
        db.clientDao().insertClients(initialClients)

        // Preload Notifications for Low Stock & Out of Stock
        val initialNotifications = listOf(
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Out of Stock Alert!",
                body = "Cat6 Shielded Cable 50m (NET-CAB-C6) is at 0 spools! Urgent reorder needed.",
                severity = NotificationSeverity.DANGER,
                itemId = "item_4",
                isRead = false,
                timestamp = System.currentTimeMillis() - 3600000L * 4
            ),
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Low Stock Warning",
                body = "ErgoLaser Wireless Mouse has 4 units left (Threshold: 10).",
                severity = NotificationSeverity.WARNING,
                itemId = "item_2",
                isRead = false,
                timestamp = System.currentTimeMillis() - 3600000L * 2
            ),
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Low Stock Warning",
                body = "Industrial LiFePO4 12V Battery has 3 units left (Threshold: 5).",
                severity = NotificationSeverity.WARNING,
                itemId = "item_6",
                isRead = false,
                timestamp = System.currentTimeMillis() - 3600000L * 1
            )
        )
        db.notificationDao().insertNotifications(initialNotifications)

        // Preload Audit Log
        val initialAuditLogs = listOf(
            AuditLog(
                id = UUID.randomUUID().toString(),
                action = "SYSTEM_INITIALIZED",
                details = "StockMaster warehouse database provisioned with initial catalog.",
                userId = "System",
                userRole = "ADMIN"
            )
        )
        db.auditLogDao().insertAuditLogs(initialAuditLogs)
    }
}
