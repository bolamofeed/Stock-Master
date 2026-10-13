package com.example.ui.screens.more

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Brand
import com.example.data.model.CurrencyUtils
import com.example.data.model.Item
import com.example.data.model.UserRole
import com.example.data.repository.MigrationResult
import com.example.ui.components.BrandFormDialog
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandsScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allBrands by viewModel.allBrands.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val canManageBrands = currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.MANAGER
    val canSeeCost = currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.MANAGER

    var searchQuery by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 20

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingBrand by remember { mutableStateOf<Brand?>(null) }

    var selectedBrandForItems by remember { mutableStateOf<Brand?>(null) }
    var itemsUsingBrand by remember { mutableStateOf<List<Item>?>(null) }

    var showMigrationDialog by remember { mutableStateOf(false) }

    val filteredBrands = remember(allBrands, searchQuery) {
        if (searchQuery.isBlank()) allBrands
        else allBrands.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val totalPages = (filteredBrands.size + pageSize - 1) / pageSize
    val pagedBrands = remember(filteredBrands, currentPage) {
        val start = (currentPage * pageSize).coerceAtMost(filteredBrands.size)
        val end = (start + pageSize).coerceAtMost(filteredBrands.size)
        filteredBrands.subList(start, end)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Brands & Pricing", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${allBrands.size} brands • Minor units piastres precision",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("brands_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (canManageBrands) {
                        IconButton(
                            onClick = { showMigrationDialog = true },
                            modifier = Modifier.testTag("migration_tool_button")
                        ) {
                            Icon(Icons.Default.Build, contentDescription = "Migration Tool", tint = PrimaryBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (canManageBrands) {
                FloatingActionButton(
                    onClick = {
                        editingBrand = null
                        showAddEditDialog = true
                    },
                    containerColor = PrimaryBlue,
                    modifier = Modifier.testTag("add_brand_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Brand", tint = MaterialTheme.colorScheme.surface)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    currentPage = 0
                },
                placeholder = { Text("Search brand name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_brands_input")
            )

            // Pagination Header / Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing ${pagedBrands.size} of ${filteredBrands.size} brands",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (totalPages > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { if (currentPage > 0) currentPage-- },
                            enabled = currentPage > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev")
                        }

                        Text(
                            text = "Page ${currentPage + 1}/$totalPages",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { if (currentPage < totalPages - 1) currentPage++ },
                            enabled = currentPage < totalPages - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                        }
                    }
                }
            }

            // Brands List
            if (pagedBrands.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Sell, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No brands found", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Add new brands or import from inventory items", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pagedBrands, key = { it.id }) { brand ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedBrandForItems = brand
                                    viewModel.getItemsUsingBrand(brand.id) { items ->
                                        itemsUsingBrand = items
                                    }
                                }
                                .testTag("brand_item_card_${brand.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (brand.isActive) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(brand.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Surface(
                                            color = if (brand.isActive) EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (brand.isActive) "Active" else "Deactivated",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (brand.isActive) EmeraldGreen else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        color = PrimaryBlue.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${brand.itemCount} items linked",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Default Sale: ${CurrencyUtils.formatMinorUnits(brand.defaultSalePriceMinor, currency)}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = PrimaryBlue
                                        )
                                        if (canSeeCost) {
                                            Text(
                                                text = "Default Cost: ${CurrencyUtils.formatMinorUnits(brand.defaultCostPriceMinor, currency)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    text = "Cost hidden for ${currentUser.role.title}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    if (canManageBrands) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = {
                                                    editingBrand = brand
                                                    showAddEditDialog = true
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                            }

                                            if (brand.isActive) {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteOrDeactivateBrand(brand.id) { result ->
                                                            result.onSuccess { msg ->
                                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                            }.onFailure { err ->
                                                                Toast.makeText(context, err.message ?: "Action failed", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete/Deactivate", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                }
                                            } else {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.reactivateBrand(brand.id) { result ->
                                                            result.onSuccess {
                                                                Toast.makeText(context, "Brand '${it.name}' reactivated!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = "Reactivate", tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog 1: Add or Edit Brand
    if (showAddEditDialog) {
        BrandFormDialog(
            brand = editingBrand,
            currencySymbol = currency,
            onDismiss = { showAddEditDialog = false },
            onSave = { name, costMinor, saleMinor ->
                if (editingBrand != null) {
                    viewModel.updateBrand(
                        id = editingBrand!!.id,
                        name = name,
                        costPriceMinor = costMinor,
                        salePriceMinor = saleMinor
                    ) { result ->
                        result.onSuccess {
                            Toast.makeText(context, "Brand updated and propagated everywhere!", Toast.LENGTH_SHORT).show()
                            showAddEditDialog = false
                        }.onFailure { err ->
                            Toast.makeText(context, err.message ?: "Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    viewModel.saveBrand(
                        name = name,
                        costPriceMinor = costMinor,
                        salePriceMinor = saleMinor
                    ) { result ->
                        result.onSuccess {
                            Toast.makeText(context, "Brand created successfully!", Toast.LENGTH_SHORT).show()
                            showAddEditDialog = false
                        }.onFailure { err ->
                            Toast.makeText(context, err.message ?: "Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    // Dialog 2: View Linked Items for Brand
    if (selectedBrandForItems != null) {
        val brand = selectedBrandForItems!!
        AlertDialog(
            onDismissRequest = {
                selectedBrandForItems = null
                itemsUsingBrand = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = PrimaryBlue)
                    Text("Items using '${brand.name}'", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (itemsUsingBrand == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (itemsUsingBrand!!.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No inventory items currently linked to this brand.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(itemsUsingBrand!!) { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("SKU: ${item.sku} • Category: ${item.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(
                                            "${item.quantity} ${item.unit}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = PrimaryBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    selectedBrandForItems = null
                    itemsUsingBrand = null
                }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog 3: Brand Migration Tool (Dry-Run / Live)
    if (showMigrationDialog) {
        var isDryRun by remember { mutableStateOf(true) }
        var isRunning by remember { mutableStateOf(false) }
        var migrationResult by remember { mutableStateOf<MigrationResult?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isRunning) showMigrationDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = PrimaryBlue)
                    Text("Brand Migration Tool", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Scans all inventory items and converts legacy single-price items into a default brand row with individual barcode and pricing.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isDryRun = !isDryRun }
                    ) {
                        Checkbox(
                            checked = isDryRun,
                            onCheckedChange = { isDryRun = it },
                            modifier = Modifier.testTag("migration_dry_run_checkbox")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("Dry-Run Simulation Mode", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Checks items without modifying database", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (isRunning) {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (migrationResult != null) {
                        val res = migrationResult!!
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (res.dryRun) PrimaryBlue.copy(alpha = 0.1f) else EmeraldGreen.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    if (res.dryRun) "[DRY-RUN COMPLETE]" else "[MIGRATION EXECUTED SUCCESSFULLY]",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (res.dryRun) PrimaryBlue else EmeraldGreen
                                )
                                Text("Inspected: ${res.itemsInspected} items | Migrated: ${res.itemsMigrated} items", fontSize = 11.sp)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(res.details) { line ->
                                Text(line, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRunning = true
                        viewModel.runBrandMigration(dryRun = isDryRun) { result ->
                            isRunning = false
                            migrationResult = result
                        }
                    },
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDryRun) PrimaryBlue else EmeraldGreen),
                    modifier = Modifier.testTag("run_migration_btn")
                ) {
                    Text(if (isDryRun) "Run Dry-Run Simulation" else "Execute Real Migration")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showMigrationDialog = false }, enabled = !isRunning) {
                    Text("Close")
                }
            }
        )
    }
}
