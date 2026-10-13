package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppNotification
import com.example.data.model.AuditLog
import com.example.data.model.Brand
import com.example.data.model.Client
import com.example.data.model.Invoice
import com.example.data.model.Item
import com.example.data.model.ItemBrand
import com.example.data.model.StockMovement
import com.example.data.model.Supplier
import com.example.data.model.Warehouse
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): Item?

    @Query("SELECT * FROM items WHERE barcode = :barcode OR sku = :barcode LIMIT 1")
    suspend fun getItemByBarcodeOrSku(barcode: String): Item?

    @Query("SELECT * FROM items WHERE quantity <= minStock ORDER BY quantity ASC")
    fun getLowStockItems(): Flow<List<Item>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<Item>)

    @Update
    suspend fun updateItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("UPDATE items SET quantity = :newQuantity, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateQuantity(id: String, newQuantity: Int, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements WHERE itemId = :itemId ORDER BY timestamp DESC")
    fun getMovementsForItem(itemId: String): Flow<List<StockMovement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<StockMovement>)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: String): Invoice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<Invoice>)

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Query("SELECT * FROM invoices WHERE type = 'SALES' AND (LOWER(entityName) = LOWER(:entityName) OR entityContact = :entityContact) ORDER BY createdAt DESC")
    fun getSalesInvoicesForClient(entityName: String, entityContact: String): Flow<List<Invoice>>
}

@Dao
interface WarehouseDao {
    @Query("SELECT * FROM warehouses ORDER BY name ASC")
    fun getAllWarehouses(): Flow<List<Warehouse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouses(warehouses: List<Warehouse>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouse(warehouse: Warehouse)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<Supplier>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(logs: List<AuditLog>)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<AppNotification>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClientById(id: String): Client?

    @Query("SELECT * FROM clients WHERE isDeleted = 0 AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getClientByName(name: String): Client?

    @Query("SELECT * FROM clients WHERE isDeleted = 0 AND phone = :phone LIMIT 1")
    suspend fun getClientByPhone(phone: String): Client?

    @Query("SELECT * FROM clients WHERE isDeleted = 0 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchClients(query: String): Flow<List<Client>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<Client>)

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)
}

@Dao
interface BrandDao {
    @Query("SELECT * FROM brands ORDER BY name ASC")
    fun getAllBrands(): Flow<List<Brand>>

    @Query("SELECT * FROM brands WHERE status = 1 ORDER BY name ASC")
    fun getActiveBrands(): Flow<List<Brand>>

    @Query("SELECT * FROM brands WHERE id = :id LIMIT 1")
    suspend fun getBrandById(id: String): Brand?

    @Query("SELECT * FROM brands WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getBrandByNameLower(name: String): Brand?

    @Query("SELECT * FROM brands WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchBrands(query: String): Flow<List<Brand>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrand(brand: Brand)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrands(brands: List<Brand>)

    @Update
    suspend fun updateBrand(brand: Brand)

    @Delete
    suspend fun deleteBrand(brand: Brand)

    @Query("UPDATE brands SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBrandStatus(id: String, status: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE brands SET itemCount = :count, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBrandItemCount(id: String, count: Int, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface ItemBrandDao {
    @Query("SELECT * FROM item_brands WHERE itemId = :itemId ORDER BY brandName ASC")
    fun getItemBrandsForItem(itemId: String): Flow<List<ItemBrand>>

    @Query("SELECT * FROM item_brands WHERE itemId = :itemId ORDER BY brandName ASC")
    suspend fun getItemBrandsByItemId(itemId: String): List<ItemBrand>

    @Query("SELECT * FROM item_brands WHERE barcode = :barcode LIMIT 1")
    suspend fun getItemBrandByBarcode(barcode: String): ItemBrand?

    @Query("SELECT * FROM item_brands WHERE brandId = :brandId")
    suspend fun getItemBrandsForBrand(brandId: String): List<ItemBrand>

    @Query("SELECT COUNT(*) FROM item_brands WHERE brandId = :brandId")
    suspend fun countItemsForBrand(brandId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemBrand(itemBrand: ItemBrand)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemBrands(itemBrands: List<ItemBrand>)

    @Query("DELETE FROM item_brands WHERE itemId = :itemId")
    suspend fun deleteItemBrandsForItem(itemId: String)

    @Query("UPDATE item_brands SET quantity = :newQuantity WHERE id = :id")
    suspend fun updateItemBrandQuantity(id: String, newQuantity: Int)

    @Query("UPDATE item_brands SET brandName = :newName WHERE brandId = :brandId")
    suspend fun updateBrandName(brandId: String, newName: String)
}


