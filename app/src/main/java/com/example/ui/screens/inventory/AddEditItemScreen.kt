package com.example.ui.screens.inventory

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyUtils
import com.example.data.model.Item
import com.example.data.model.ItemBrand
import com.example.ui.components.BrandDropdownField
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.InventoryViewModel
import java.util.UUID

data class BrandRowState(
    val rowId: String = UUID.randomUUID().toString(),
    var brandId: String = "",
    var brandName: String = "",
    var barcode: String = "",
    var costPriceText: String = "0.00",
    var salePriceText: String = "0.00",
    var quantityText: String = "1",
    var minStockText: String = "5"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(
    itemId: String?,
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.rawItems.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()
    val existingItem = items.find { it.id == itemId }
    val isNew = existingItem == null

    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var sku by remember { mutableStateOf(existingItem?.sku ?: "") }
    var barcode by remember { mutableStateOf(existingItem?.barcode ?: "") }
    var category by remember { mutableStateOf(existingItem?.category ?: "Electronics") }
    var description by remember { mutableStateOf(existingItem?.description ?: "") }
    var unit by remember { mutableStateOf(existingItem?.unit ?: "pcs") }

    // Standalone fallback pricing when 0 brand rows exist
    var fallbackCostPrice by remember { mutableStateOf(existingItem?.costPrice?.toString() ?: "0.00") }
    var fallbackSalePrice by remember { mutableStateOf(existingItem?.salePrice?.toString() ?: "0.00") }
    var fallbackQuantity by remember { mutableStateOf(existingItem?.quantity?.toString() ?: "10") }
    var minStock by remember { mutableStateOf(existingItem?.minStock?.toString() ?: "5") }

    var supplier by remember { mutableStateOf(existingItem?.supplier ?: "Apex Tech Supplies") }
    var warehouse by remember { mutableStateOf(existingItem?.warehouse ?: "Main Central Hub") }
    var expiryDate by remember { mutableStateOf(existingItem?.expiryDate ?: "") }

    val brandRows = remember { mutableStateListOf<BrandRowState>() }
    var isInitialized by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load existing brand rows for this item
    LaunchedEffect(existingItem) {
        if (!isInitialized && existingItem != null) {
            val serialized = existingItem.brandsJson
            if (serialized.isNotBlank()) {
                val parsed = serialized.split("##").mapNotNull { ItemBrand.deserialize(it) }
                brandRows.clear()
                for (b in parsed) {
                    brandRows.add(
                        BrandRowState(
                            rowId = b.id,
                            brandId = b.brandId,
                            brandName = b.brandName,
                            barcode = b.barcode,
                            costPriceText = "%.2f".format(b.costPriceMinor / 100.0),
                            salePriceText = "%.2f".format(b.salePriceMinor / 100.0),
                            quantityText = b.quantity.toString(),
                            minStockText = b.minStock.toString()
                        )
                    )
                }
            }
            isInitialized = true
        }
    }

    fun generateRandomSkuAndBarcode() {
        val randomSuffix = (1000..9999).random()
        val catCode = category.take(3).uppercase().ifEmpty { "ITM" }
        sku = "$catCode-ITM-$randomSuffix"
        barcode = "890${(1000000000L..9999999999L).random()}"
    }

    fun addNewBrandRow() {
        val randomBarcode = "890${(1000000000L..9999999999L).random()}"
        brandRows.add(
            BrandRowState(
                rowId = UUID.randomUUID().toString(),
                brandId = "",
                brandName = "",
                barcode = randomBarcode,
                costPriceText = fallbackCostPrice,
                salePriceText = fallbackSalePrice,
                quantityText = "5",
                minStockText = minStock
            )
        )
    }

    val hasBrands = brandRows.isNotEmpty()
    val totalBrandQuantity = if (hasBrands) {
        brandRows.sumOf { it.quantityText.toIntOrNull() ?: 0 }
    } else {
        fallbackQuantity.toIntOrNull() ?: 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isNew) "Add New Inventory Item" else "Edit Item",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("add_item_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .background(MaterialTheme.colorScheme.background)
                .testTag("add_edit_item_form"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Item Name
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_name_input")
                )
            }

            // SKU & Auto-Generate
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU Code *") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_sku_input")
                    )

                    OutlinedButton(
                        onClick = { generateRandomSkuAndBarcode() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("auto_generate_sku_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto", fontSize = 12.sp)
                    }
                }
            }

            // Master Barcode
            item {
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Primary Barcode / EAN-13 *") },
                    placeholder = { Text("e.g. 8901234567890") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_barcode_input")
                )
            }

            // Category & Unit fields
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_category_input")
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (pcs/kg/box)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_unit_input")
                    )
                }
            }

            // SECTION: Brands & Prices (Directly below Category and Unit)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                Icon(Icons.Default.Sell, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Brands & Prices", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        if (hasBrands) "${brandRows.size} brand variation(s) assigned"
                                        else "Assign multiple brands with distinct prices & barcodes",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { addNewBrandRow() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("add_brand_row_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Brand", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!hasBrands) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                    Text(
                                        "No brand rows added yet. Tap 'Add Brand' above to configure brand-specific pricing and stock, or use fallback single pricing below.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Render each Brand Row when present
            if (hasBrands) {
                itemsIndexed(brandRows, key = { _, row -> row.rowId }) { index, row ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("brand_row_card_$index"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header of Brand Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = PrimaryBlue.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Brand Row #${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { brandRows.removeAt(index) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("delete_brand_row_$index")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove row", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }

                            // 1. Reusable Brand Dropdown (with in-form Add/Edit brand dialogs)
                            BrandDropdownField(
                                selectedBrandId = row.brandId,
                                onBrandSelected = { selectedBrand ->
                                    val updated = row.copy(
                                        brandId = selectedBrand.id,
                                        brandName = selectedBrand.name,
                                        costPriceText = "%.2f".format(selectedBrand.defaultCostPriceMinor / 100.0),
                                        salePriceText = "%.2f".format(selectedBrand.defaultSalePriceMinor / 100.0)
                                    )
                                    brandRows[index] = updated
                                },
                                viewModel = viewModel,
                                label = "Select Brand *"
                            )

                            // 2. Cost Price & Selling Price
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = row.costPriceText,
                                    onValueChange = { newCost ->
                                        brandRows[index] = row.copy(costPriceText = newCost)
                                    },
                                    label = { Text("Cost Price ($currency)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("brand_cost_input_$index")
                                )

                                OutlinedTextField(
                                    value = row.salePriceText,
                                    onValueChange = { newSale ->
                                        brandRows[index] = row.copy(salePriceText = newSale)
                                    },
                                    label = { Text("Selling Price ($currency) *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("brand_sale_input_$index")
                                )
                            }

                            // 3. Quantity & Dedicated Brand Barcode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = row.quantityText,
                                    onValueChange = { newQty ->
                                        brandRows[index] = row.copy(quantityText = newQty.filter { c -> c.isDigit() })
                                    },
                                    label = { Text("Qty") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .testTag("brand_qty_input_$index")
                                )

                                OutlinedTextField(
                                    value = row.barcode,
                                    onValueChange = { newBar ->
                                        brandRows[index] = row.copy(barcode = newBar.trim())
                                    },
                                    label = { Text("Brand Barcode *") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1.8f)
                                        .testTag("brand_barcode_input_$index")
                                )

                                OutlinedButton(
                                    onClick = {
                                        val newBar = "890${(1000000000L..9999999999L).random()}"
                                        brandRows[index] = row.copy(barcode = newBar)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("auto_brand_barcode_$index")
                                ) {
                                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                // Standalone Cost Price & Sale Price (Shown ONLY when 0 brand rows exist)
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fallbackCostPrice,
                            onValueChange = { fallbackCostPrice = it },
                            label = { Text("Cost Price ($currency) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("item_cost_price_input")
                        )

                        OutlinedTextField(
                            value = fallbackSalePrice,
                            onValueChange = { fallbackSalePrice = it },
                            label = { Text("Selling Price ($currency) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("item_sale_price_input")
                        )
                    }
                }
            }

            // Quantity in Stock (Read-only sum of all brand quantities when brands exist) & Min Stock Level
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalBrandQuantity.toString(),
                        onValueChange = {
                            if (!hasBrands) fallbackQuantity = it.filter { c -> c.isDigit() }
                        },
                        readOnly = hasBrands,
                        label = {
                            Text(if (hasBrands) "Quantity in Stock (Sum) *" else "Quantity in Stock *")
                        },
                        supportingText = {
                            if (hasBrands) Text("Calculated from brand rows", fontSize = 10.sp, color = PrimaryBlue)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_quantity_input"),
                        colors = if (hasBrands) {
                            OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        } else OutlinedTextFieldDefaults.colors()
                    )

                    OutlinedTextField(
                        value = minStock,
                        onValueChange = { minStock = it.filter { c -> c.isDigit() } },
                        label = { Text("Min Stock Level *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_min_stock_input")
                    )
                }
            }

            // Warehouse & Supplier
            item {
                OutlinedTextField(
                    value = warehouse,
                    onValueChange = { warehouse = it },
                    label = { Text("Warehouse / Location") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_warehouse_input")
                )
            }

            item {
                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text("Supplier Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_supplier_input")
                )
            }

            // Description
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Specs") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_description_input")
                )
            }

            // Expiry Date (Optional)
            item {
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Expiry Date (Optional, e.g. 2028-12-31)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Submit Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (name.isBlank() || sku.isBlank() || barcode.isBlank()) {
                            errorMessage = "Name, SKU, and Barcode are required!"
                            return@Button
                        }

                        val min = minStock.toIntOrNull() ?: 5
                        val targetItemId = existingItem?.id ?: UUID.randomUUID().toString()

                        // Prepare brand rows
                        val itemBrands = mutableListOf<ItemBrand>()
                        if (hasBrands) {
                            for (row in brandRows) {
                                if (row.brandId.isBlank()) {
                                    errorMessage = "Please select a brand for all brand rows!"
                                    return@Button
                                }
                                val costMinor = CurrencyUtils.parseToMinorUnits(row.costPriceText)
                                val saleMinor = CurrencyUtils.parseToMinorUnits(row.salePriceText)
                                val qty = row.quantityText.toIntOrNull() ?: 0
                                val rowBarcode = row.barcode.ifEmpty { "BRD-${(1000000000L..9999999999L).random()}" }

                                itemBrands.add(
                                    ItemBrand(
                                        id = row.rowId,
                                        itemId = targetItemId,
                                        brandId = row.brandId,
                                        brandName = row.brandName,
                                        barcode = rowBarcode,
                                        costPriceMinor = costMinor,
                                        salePriceMinor = saleMinor,
                                        quantity = qty,
                                        minStock = row.minStockText.toIntOrNull() ?: min,
                                        status = 1
                                    )
                                )
                            }
                        }

                        val effectiveCost = if (hasBrands) {
                            itemBrands.firstOrNull()?.costPriceMajor ?: 0.0
                        } else {
                            fallbackCostPrice.toDoubleOrNull() ?: 0.0
                        }

                        val effectiveSale = if (hasBrands) {
                            itemBrands.firstOrNull()?.salePriceMajor ?: 0.0
                        } else {
                            fallbackSalePrice.toDoubleOrNull() ?: 0.0
                        }

                        val newItem = Item(
                            id = targetItemId,
                            sku = sku.trim(),
                            barcode = barcode.trim(),
                            name = name.trim(),
                            category = category.trim().ifEmpty { "General" },
                            description = description.trim(),
                            unit = unit.trim().ifEmpty { "pcs" },
                            costPrice = effectiveCost,
                            salePrice = effectiveSale,
                            quantity = totalBrandQuantity,
                            minStock = min,
                            supplier = supplier.trim(),
                            warehouse = warehouse.trim(),
                            expiryDate = expiryDate.trim(),
                            brandsJson = itemBrands.joinToString("##") { it.serialize() },
                            updatedAt = System.currentTimeMillis()
                        )

                        viewModel.saveItemWithBrands(
                            item = newItem,
                            brands = itemBrands,
                            isNew = isNew
                        ) {
                            Toast.makeText(context, "Item saved successfully with ${itemBrands.size} brands!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_item_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Inventory Item", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
