package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AppNotification
import com.example.data.model.AuditLog
import com.example.data.model.Brand
import com.example.data.model.Client
import com.example.data.model.CurrentUser
import com.example.data.model.Invoice
import com.example.data.model.InvoiceLineItem
import com.example.data.model.InvoiceType
import com.example.data.model.Item
import com.example.data.model.ItemBrand
import com.example.data.model.MovementType
import com.example.data.model.StockMovement
import com.example.data.model.Supplier
import com.example.data.model.UserRole
import com.example.data.model.Warehouse
import com.example.data.repository.InventoryRepository
import com.example.data.repository.MigrationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StockFilterOption(val label: String) {
    ALL("All Items"),
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock"),
    IN_STOCK("Healthy Stock")
}

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InventoryRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = InventoryRepository(db)
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    // Role state
    private val _currentUser = MutableStateFlow(
        CurrentUser(
            uid = "usr_001",
            name = "Alexander Vance",
            email = "alex.vance@stockmaster.app",
            role = UserRole.ADMIN,
            assignedVendorName = "Apex Tech Supplies"
        )
    )
    val currentUser: StateFlow<CurrentUser> = _currentUser.asStateFlow()

    // Settings state
    private val _currencySymbol = MutableStateFlow("$")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _isArabicLanguage = MutableStateFlow(false)
    val isArabicLanguage: StateFlow<Boolean> = _isArabicLanguage.asStateFlow()

    // Search and filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedWarehouse = MutableStateFlow("All")
    val selectedWarehouse: StateFlow<String> = _selectedWarehouse.asStateFlow()

    private val _stockFilter = MutableStateFlow(StockFilterOption.ALL)
    val stockFilter: StateFlow<StockFilterOption> = _stockFilter.asStateFlow()

    // Scanner state
    private val _lastScannedBarcode = MutableStateFlow<String?>(null)
    val lastScannedBarcode: StateFlow<String?> = _lastScannedBarcode.asStateFlow()

    // Raw flows from repository
    val rawItems: StateFlow<List<Item>> = repository.allItems.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val lowStockItems: StateFlow<List<Item>> = repository.lowStockItems.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allMovements: StateFlow<List<StockMovement>> = repository.allMovements.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allInvoices: StateFlow<List<Invoice>> = repository.allInvoices.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allWarehouses: StateFlow<List<Warehouse>> = repository.allWarehouses.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allSuppliers: StateFlow<List<Supplier>> = repository.allSuppliers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allBrands: StateFlow<List<Brand>> = repository.allBrands.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val activeBrands: StateFlow<List<Brand>> = repository.activeBrands.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allClients: StateFlow<List<Client>> = repository.allClients.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Clients with no invoice purchase in more than 3 months (~90 days)
    val inactiveClientsMoreThanThreeMonths: StateFlow<List<Client>> = repository.allClients.map { clients ->
        val threeMonthsAgo = System.currentTimeMillis() - (90L * 24L * 60L * 60L * 1000L)
        clients.filter { client ->
            // Inactive if lastPurchaseDate is 0 (never bought) or older than 90 days
            client.lastPurchaseDate in 1..threeMonthsAgo || client.lastPurchaseDate == 0L
        }.sortedBy { it.lastPurchaseDate }
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allAuditLogs: StateFlow<List<AuditLog>> = repository.allAuditLogs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allNotifications: StateFlow<List<AppNotification>> = repository.allNotifications.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val unreadNotificationsCount: StateFlow<Int> = repository.unreadNotificationsCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    fun saveClient(name: String, phone: String, email: String = "", address: String = "") {
        viewModelScope.launch {
            repository.recordOrUpdateClientFromInvoice(name, phone, 0.0)
        }
    }

    private data class FilterParams(
        val query: String,
        val category: String,
        val warehouse: String,
        val filter: StockFilterOption
    )

    private val _filterParamsFlow = combine(
        _searchQuery,
        _selectedCategory,
        _selectedWarehouse,
        _stockFilter
    ) { q, c, w, f -> FilterParams(q, c, w, f) }

    // Filtered items based on RBAC role + search query + category + stock level
    val filteredItems: StateFlow<List<Item>> = combine(
        rawItems,
        _currentUser,
        _filterParamsFlow
    ) { items, user, params ->
        var list = items

        // RBAC: Vendor sees only their assigned supplier items
        if (user.role == UserRole.VENDOR) {
            list = list.filter { it.supplier.equals(user.assignedVendorName, ignoreCase = true) }
        }

        // Search query filter
        if (params.query.isNotBlank()) {
            val q = params.query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.sku.lowercase().contains(q) ||
                it.barcode.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
            }
        }

        // Category filter
        if (params.category != "All") {
            list = list.filter { it.category == params.category }
        }

        // Warehouse filter
        if (params.warehouse != "All") {
            list = list.filter { it.warehouse == params.warehouse }
        }

        // Stock status filter
        when (params.filter) {
            StockFilterOption.ALL -> list
            StockFilterOption.LOW_STOCK -> list.filter { it.isLowStock }
            StockFilterOption.OUT_OF_STOCK -> list.filter { it.isOutOfStock }
            StockFilterOption.IN_STOCK -> list.filter { !it.isLowStock && !it.isOutOfStock }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Role management
    fun switchRole(newRole: UserRole) {
        val user = _currentUser.value.copy(
            role = newRole,
            name = when (newRole) {
                UserRole.ADMIN -> "Alexander Vance"
                UserRole.MANAGER -> "Sarah Jenkins"
                UserRole.VENDOR -> "Marcus Reed (Apex Supplies)"
                UserRole.CASHIER -> "Leo Miller"
                UserRole.AUDITOR -> "Elena Rostova"
            }
        )
        _currentUser.value = user
    }

    // Search and filters
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedCategory(category: String) { _selectedCategory.value = category }
    fun setSelectedWarehouse(wh: String) { _selectedWarehouse.value = wh }
    fun setStockFilter(filter: StockFilterOption) { _stockFilter.value = filter }
    fun setCurrencySymbol(currency: String) { _currencySymbol.value = currency }
    fun toggleLanguage() { _isArabicLanguage.value = !_isArabicLanguage.value }

    fun setLastScannedBarcode(code: String?) {
        _lastScannedBarcode.value = code
    }

    // CRUD operations
    fun saveItem(item: Item, isNew: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val user = _currentUser.value
            repository.saveItem(item, user.name, user.role.name, isNew)
            onDone()
        }
    }

    fun deleteItem(item: Item, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val user = _currentUser.value
            repository.deleteItem(item, user.name, user.role.name)
            onDone()
        }
    }

    fun recordStockMovement(
        item: Item,
        type: MovementType,
        quantity: Int,
        note: String,
        onResult: (Result<Item>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val result = repository.recordStockMovement(
                item = item,
                type = type,
                quantityChange = quantity,
                user = user.name,
                role = user.role.name,
                note = note
            )
            onResult(result)
        }
    }

    fun createInvoice(
        type: InvoiceType,
        entityName: String,
        entityContact: String,
        lineItems: List<InvoiceLineItem>,
        taxPercent: Double,
        discountPercent: Double,
        notes: String,
        markAsPaid: Boolean = true,
        saveCustomer: Boolean = true,
        onResult: (Result<Invoice>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val result = repository.createInvoice(
                type = type,
                entityName = entityName,
                entityContact = entityContact,
                lineItems = lineItems,
                taxPercent = taxPercent,
                discountPercent = discountPercent,
                notes = notes,
                user = user.name,
                role = user.role.name,
                markAsPaid = markAsPaid,
                saveCustomer = saveCustomer
            )
            onResult(result)
        }
    }

    fun payInvoice(
        invoiceId: String,
        paymentMethod: String,
        onResult: (Result<Invoice>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val result = repository.payInvoice(
                invoiceId = invoiceId,
                paymentMethod = paymentMethod,
                user = user.name,
                role = user.role.name
            )
            onResult(result)
        }
    }

    fun returnInvoice(
        invoiceId: String,
        itemsToReturn: List<Pair<String, Int>>,
        reason: String,
        onResult: (Result<Invoice>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val result = repository.returnInvoice(
                invoiceId = invoiceId,
                itemsToReturn = itemsToReturn,
                reason = reason,
                user = user.name,
                role = user.role.name
            )
            onResult(result)
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { repository.markNotificationRead(id) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch { repository.markAllNotificationsRead() }
    }

    fun clearNotifications() {
        viewModelScope.launch { repository.clearNotifications() }
    }

    fun addClient(name: String, phone: String, email: String = "", address: String = "", onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val client = Client(
                id = java.util.UUID.randomUUID().toString(),
                name = name.trim(),
                phone = phone.trim(),
                email = email.trim(),
                address = address.trim(),
                totalPurchasesAmount = 0.0,
                invoicesCount = 0,
                lastPurchaseDate = 0L,
                createdAt = System.currentTimeMillis()
            )
            repository.saveClient(client)
            onDone()
        }
    }

    fun updateClient(client: Client, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateClient(client)
            onDone()
        }
    }

    fun softDeleteClient(clientId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val user = _currentUser.value
            repository.softDeleteClient(clientId, user.name, user.role.name)
            onDone()
        }
    }

    fun markClientContacted(clientId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.markClientContacted(clientId)
            onDone()
        }
    }

    fun getClientInvoices(name: String, phone: String): Flow<List<Invoice>> =
        repository.getInvoicesForCustomer(name, phone)

    fun getItemBrands(itemId: String): Flow<List<ItemBrand>> =
        repository.brandRepository.getItemBrandsForItem(itemId)

    fun getItemBrandByBarcode(barcode: String, onResult: (ItemBrand?) -> Unit) {
        viewModelScope.launch {
            val itemBrand = repository.brandRepository.getItemBrandByBarcode(barcode)
            onResult(itemBrand)
        }
    }

    fun saveBrand(
        name: String,
        costPriceMinor: Long,
        salePriceMinor: Long,
        onResult: (Result<Brand>) -> Unit
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val res = repository.brandRepository.saveBrand(
                name = name,
                defaultCostPriceMinor = costPriceMinor,
                defaultSalePriceMinor = salePriceMinor,
                user = user.name,
                role = user.role.name
            )
            onResult(res)
        }
    }

    fun updateBrand(
        id: String,
        name: String,
        costPriceMinor: Long,
        salePriceMinor: Long,
        onResult: (Result<Brand>) -> Unit
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val res = repository.brandRepository.updateBrand(
                id = id,
                newName = name,
                newCostPriceMinor = costPriceMinor,
                newSalePriceMinor = salePriceMinor,
                user = user.name,
                role = user.role.name
            )
            onResult(res)
        }
    }

    fun deleteOrDeactivateBrand(brandId: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            val res = repository.brandRepository.deleteOrDeactivateBrand(
                brandId = brandId,
                user = user.name,
                role = user.role.name
            )
            onResult(res)
        }
    }

    fun reactivateBrand(brandId: String, onResult: (Result<Brand>) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            val res = repository.brandRepository.reactivateBrand(
                brandId = brandId,
                user = user.name,
                role = user.role.name
            )
            onResult(res)
        }
    }

    fun getItemsUsingBrand(brandId: String, onResult: (List<Item>) -> Unit) {
        viewModelScope.launch {
            val items = repository.brandRepository.getItemsUsingBrand(brandId)
            onResult(items)
        }
    }

    fun runBrandMigration(dryRun: Boolean, onResult: (MigrationResult) -> Unit) {
        viewModelScope.launch {
            val res = repository.brandRepository.migrateSinglePriceItemsToDefaultBrand(dryRun)
            onResult(res)
        }
    }

    fun saveItemWithBrands(
        item: Item,
        brands: List<ItemBrand>,
        isNew: Boolean,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            repository.saveItemWithBrands(
                item = item,
                brands = brands,
                user = user.name,
                role = user.role.name,
                isNew = isNew
            )
            onComplete()
        }
    }
}

