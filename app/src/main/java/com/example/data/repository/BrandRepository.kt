package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.AuditLog
import com.example.data.model.Brand
import com.example.data.model.CurrencyUtils
import com.example.data.model.Item
import com.example.data.model.ItemBrand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

data class MigrationResult(
    val dryRun: Boolean,
    val itemsInspected: Int,
    val itemsMigrated: Int,
    val details: List<String>
)

class BrandRepository(private val db: AppDatabase) {

    val allBrands: Flow<List<Brand>> = db.brandDao().getAllBrands()
    val activeBrands: Flow<List<Brand>> = db.brandDao().getActiveBrands()

    fun searchBrands(query: String): Flow<List<Brand>> = db.brandDao().searchBrands(query)

    fun getItemBrandsForItem(itemId: String): Flow<List<ItemBrand>> =
        db.itemBrandDao().getItemBrandsForItem(itemId)

    suspend fun getItemBrandsByItemId(itemId: String): List<ItemBrand> =
        db.itemBrandDao().getItemBrandsByItemId(itemId)

    suspend fun getItemBrandByBarcode(barcode: String): ItemBrand? =
        db.itemBrandDao().getItemBrandByBarcode(barcode.trim())

    suspend fun getBrandById(id: String): Brand? = db.brandDao().getBrandById(id)

    /**
     * Sanitizes brand name, stripping HTML, special symbols, and extra whitespace.
     */
    fun sanitizeBrandName(rawName: String): String {
        return rawName.trim().replace(Regex("[<>\"';\\\\]"), "").replace(Regex("\\s+"), " ")
    }

    /**
     * Add a new brand with validation, uniqueness checks, and audit logging.
     */
    suspend fun saveBrand(
        name: String,
        defaultCostPriceMinor: Long,
        defaultSalePriceMinor: Long,
        user: String = "Admin",
        role: String = "ADMIN"
    ): Result<Brand> {
        val cleanName = sanitizeBrandName(name)
        if (cleanName.length < 2 || cleanName.length > 50) {
            return Result.failure(IllegalArgumentException("Brand name must be between 2 and 50 characters."))
        }

        val existing = db.brandDao().getBrandByNameLower(cleanName.lowercase())
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Brand '$cleanName' already exists."))
        }

        val brandId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val brand = Brand(
            id = brandId,
            name = cleanName,
            nameLower = cleanName.lowercase(),
            defaultCostPriceMinor = defaultCostPriceMinor.coerceAtLeast(0L),
            defaultSalePriceMinor = defaultSalePriceMinor.coerceAtLeast(0L),
            status = 1,
            itemCount = 0,
            createdAt = now,
            updatedAt = now
        )

        db.brandDao().insertBrand(brand)

        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            action = "BRAND_CREATED",
            details = "Brand '$cleanName' created with cost: ${CurrencyUtils.formatMinorUnits(defaultCostPriceMinor)} and sale: ${CurrencyUtils.formatMinorUnits(defaultSalePriceMinor)}",
            userId = user,
            userRole = role
        )
        db.auditLogDao().insertAuditLog(log)

        return Result.success(brand)
    }

    /**
     * Update an existing brand. Renaming performs batched updates across all ItemBrand records.
     */
    suspend fun updateBrand(
        id: String,
        newName: String,
        newCostPriceMinor: Long,
        newSalePriceMinor: Long,
        user: String = "Admin",
        role: String = "ADMIN"
    ): Result<Brand> {
        val existing = db.brandDao().getBrandById(id)
            ?: return Result.failure(IllegalArgumentException("Brand not found"))

        val cleanName = sanitizeBrandName(newName)
        if (cleanName.length < 2 || cleanName.length > 50) {
            return Result.failure(IllegalArgumentException("Brand name must be between 2 and 50 characters."))
        }

        val duplicate = db.brandDao().getBrandByNameLower(cleanName.lowercase())
        if (duplicate != null && duplicate.id != id) {
            return Result.failure(IllegalArgumentException("Brand '$cleanName' already exists."))
        }

        val nameChanged = existing.name != cleanName
        val costChanged = existing.defaultCostPriceMinor != newCostPriceMinor
        val saleChanged = existing.defaultSalePriceMinor != newSalePriceMinor
        val now = System.currentTimeMillis()

        val updatedBrand = existing.copy(
            name = cleanName,
            nameLower = cleanName.lowercase(),
            defaultCostPriceMinor = newCostPriceMinor.coerceAtLeast(0L),
            defaultSalePriceMinor = newSalePriceMinor.coerceAtLeast(0L),
            updatedAt = now
        )

        db.withTransaction {
            db.brandDao().updateBrand(updatedBrand)

            if (nameChanged) {
                // Batched write across all item_brands using this brandId
                db.itemBrandDao().updateBrandName(brandId = id, newName = cleanName)
            }
        }

        val changes = mutableListOf<String>()
        if (nameChanged) changes.add("Renamed from '${existing.name}' to '$cleanName'")
        if (costChanged) changes.add("Cost: ${CurrencyUtils.formatMinorUnits(existing.defaultCostPriceMinor)} -> ${CurrencyUtils.formatMinorUnits(newCostPriceMinor)}")
        if (saleChanged) changes.add("Sale: ${CurrencyUtils.formatMinorUnits(existing.defaultSalePriceMinor)} -> ${CurrencyUtils.formatMinorUnits(newSalePriceMinor)}")

        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            action = "BRAND_UPDATED",
            details = "Brand '$cleanName' updated: ${changes.joinToString(", ")}",
            userId = user,
            userRole = role
        )
        db.auditLogDao().insertAuditLog(log)

        return Result.success(updatedBrand)
    }

    /**
     * Delete or deactivate brand:
     * If the brand is used by any items, deactivates instead of deleting and blocks hard deletion.
     * Only hard deletes if 0 items are linked.
     */
    suspend fun deleteOrDeactivateBrand(
        brandId: String,
        user: String = "Admin",
        role: String = "ADMIN"
    ): Result<String> {
        val brand = db.brandDao().getBrandById(brandId)
            ?: return Result.failure(IllegalArgumentException("Brand not found"))

        val usedCount = db.itemBrandDao().countItemsForBrand(brandId)
        if (usedCount > 0) {
            // Deactivate instead of deleting
            db.brandDao().updateBrandStatus(brandId, status = 0)
            val log = AuditLog(
                id = UUID.randomUUID().toString(),
                action = "BRAND_DEACTIVATED",
                details = "Brand '${brand.name}' deactivated because it is linked to $usedCount inventory items (hard delete blocked).",
                userId = user,
                userRole = role
            )
            db.auditLogDao().insertAuditLog(log)
            return Result.success("Brand '${brand.name}' is in use by $usedCount items. It has been deactivated instead of deleted.")
        } else {
            // Safe to hard delete
            db.brandDao().deleteBrand(brand)
            val log = AuditLog(
                id = UUID.randomUUID().toString(),
                action = "BRAND_DELETED",
                details = "Brand '${brand.name}' hard deleted (0 items linked).",
                userId = user,
                userRole = role
            )
            db.auditLogDao().insertAuditLog(log)
            return Result.success("Brand '${brand.name}' successfully deleted.")
        }
    }

    /**
     * Reactivate brand
     */
    suspend fun reactivateBrand(brandId: String, user: String = "Admin", role: String = "ADMIN"): Result<Brand> {
        val brand = db.brandDao().getBrandById(brandId)
            ?: return Result.failure(IllegalArgumentException("Brand not found"))

        db.brandDao().updateBrandStatus(brandId, status = 1)
        val updated = brand.copy(status = 1)

        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            action = "BRAND_ACTIVATED",
            details = "Brand '${brand.name}' reactivated.",
            userId = user,
            userRole = role
        )
        db.auditLogDao().insertAuditLog(log)

        return Result.success(updated)
    }

    /**
     * Query all items that use a specific brand.
     */
    suspend fun getItemsUsingBrand(brandId: String): List<Item> {
        val itemBrands = db.itemBrandDao().getItemBrandsForBrand(brandId)
        val itemIds = itemBrands.map { it.itemId }.distinct()
        val allItems = db.itemDao().getAllItems().first()
        return allItems.filter { it.id in itemIds }
    }

    /**
     * Saves brand rows for an item transactionally and recalculates item counts on brands.
     */
    suspend fun saveItemBrands(
        itemId: String,
        brands: List<ItemBrand>,
        user: String = "Admin",
        role: String = "ADMIN"
    ) {
        db.withTransaction {
            db.itemBrandDao().deleteItemBrandsForItem(itemId)
            if (brands.isNotEmpty()) {
                db.itemBrandDao().insertItemBrands(brands)
            }

            // Recalculate item counts for affected brands
            val allBrandsList = db.brandDao().getAllBrands().first()
            for (b in allBrandsList) {
                val count = db.itemBrandDao().countItemsForBrand(b.id)
                db.brandDao().updateBrandItemCount(b.id, count)
            }
        }
    }

    /**
     * Migration tool: Turns existing single-price items without brand rows
     * into one default brand row. Supports dry-run mode first.
     */
    suspend fun migrateSinglePriceItemsToDefaultBrand(dryRun: Boolean): MigrationResult {
        val items = db.itemDao().getAllItems().first()
        val details = mutableListOf<String>()
        var countMigrated = 0

        // Ensure default brand exists
        val defaultBrandName = "Standard Brand"
        var defaultBrand = db.brandDao().getBrandByNameLower(defaultBrandName.lowercase())
        if (defaultBrand == null && !dryRun) {
            defaultBrand = Brand(
                id = UUID.randomUUID().toString(),
                name = defaultBrandName,
                nameLower = defaultBrandName.lowercase(),
                defaultCostPriceMinor = 1000L,
                defaultSalePriceMinor = 1500L,
                status = 1,
                itemCount = 0
            )
            db.brandDao().insertBrand(defaultBrand)
        }

        for (item in items) {
            val existingBrands = db.itemBrandDao().getItemBrandsByItemId(item.id)
            if (existingBrands.isEmpty()) {
                countMigrated++
                val costMinor = CurrencyUtils.toMinorUnits(item.costPrice)
                val saleMinor = CurrencyUtils.toMinorUnits(item.salePrice)
                val targetBrandName = defaultBrand?.name ?: defaultBrandName
                val targetBrandId = defaultBrand?.id ?: "default-brand-id"

                if (dryRun) {
                    details.add("[Dry-Run] Item '${item.name}' (${item.sku}) would create brand row: '$targetBrandName' with Barcode '${item.barcode}', Qty ${item.quantity}, Cost ${CurrencyUtils.formatMinorUnits(costMinor)}, Sale ${CurrencyUtils.formatMinorUnits(saleMinor)}")
                } else {
                    val newRow = ItemBrand(
                        id = UUID.randomUUID().toString(),
                        itemId = item.id,
                        brandId = targetBrandId,
                        brandName = targetBrandName,
                        barcode = item.barcode.ifEmpty { "BRD-${(1000000000L..9999999999L).random()}" },
                        costPriceMinor = costMinor,
                        salePriceMinor = saleMinor,
                        quantity = item.quantity,
                        minStock = item.minStock,
                        status = 1
                    )
                    db.itemBrandDao().insertItemBrand(newRow)

                    val updatedItem = item.copy(
                        brandsJson = newRow.serialize(),
                        updatedAt = System.currentTimeMillis()
                    )
                    db.itemDao().updateItem(updatedItem)
                    details.add("[Migrated] Item '${item.name}' successfully linked to default brand '$targetBrandName'")
                }
            }
        }

        if (!dryRun && defaultBrand != null) {
            val totalCount = db.itemBrandDao().countItemsForBrand(defaultBrand.id)
            db.brandDao().updateBrandItemCount(defaultBrand.id, totalCount)

            val log = AuditLog(
                id = UUID.randomUUID().toString(),
                action = "MIGRATION_SINGLE_PRICE_ITEMS",
                details = "Migrated $countMigrated single-price items into brand rows under '$defaultBrandName'.",
                userId = "SystemMigration",
                userRole = "ADMIN"
            )
            db.auditLogDao().insertAuditLog(log)
        }

        return MigrationResult(
            dryRun = dryRun,
            itemsInspected = items.size,
            itemsMigrated = countMigrated,
            details = details
        )
    }
}
