package com.example.ui.screens.inventory

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.model.Item
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.InventoryViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(
    itemId: String?,
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.rawItems.collectAsState()
    val existingItem = items.find { it.id == itemId }
    val isNew = existingItem == null

    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var sku by remember { mutableStateOf(existingItem?.sku ?: "") }
    var barcode by remember { mutableStateOf(existingItem?.barcode ?: "") }
    var category by remember { mutableStateOf(existingItem?.category ?: "Electronics") }
    var description by remember { mutableStateOf(existingItem?.description ?: "") }
    var unit by remember { mutableStateOf(existingItem?.unit ?: "pcs") }
    var costPrice by remember { mutableStateOf(existingItem?.costPrice?.toString() ?: "0.0") }
    var salePrice by remember { mutableStateOf(existingItem?.salePrice?.toString() ?: "0.0") }
    var quantity by remember { mutableStateOf(existingItem?.quantity?.toString() ?: "10") }
    var minStock by remember { mutableStateOf(existingItem?.minStock?.toString() ?: "5") }
    var supplier by remember { mutableStateOf(existingItem?.supplier ?: "Apex Tech Supplies") }
    var warehouse by remember { mutableStateOf(existingItem?.warehouse ?: "Main Central Hub") }
    var expiryDate by remember { mutableStateOf(existingItem?.expiryDate ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun generateRandomSkuAndBarcode() {
        val randomSuffix = (1000..9999).random()
        val catCode = category.take(3).uppercase()
        sku = "$catCode-ITM-$randomSuffix"
        barcode = "890${(1000000000L..9999999999L).random()}"
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
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("item_name_input")
                )
            }

            // SKU & Generate Button
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
                        modifier = Modifier.weight(1f).testTag("item_sku_input")
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

            // Barcode
            item {
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode / EAN-13 *") },
                    placeholder = { Text("e.g. 8901234567890") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("item_barcode_input")
                )
            }

            // Category & Unit
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("item_category_input")
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (pcs/kg/box)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("item_unit_input")
                    )
                }
            }

            // Cost Price & Sale Price
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Cost Price *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("item_cost_price_input")
                    )

                    OutlinedTextField(
                        value = salePrice,
                        onValueChange = { salePrice = it },
                        label = { Text("Selling Price *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("item_sale_price_input")
                    )
                }
            }

            // Initial Quantity & Min Stock Limit
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                        label = { Text("Quantity in Stock *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("item_quantity_input")
                    )

                    OutlinedTextField(
                        value = minStock,
                        onValueChange = { minStock = it.filter { c -> c.isDigit() } },
                        label = { Text("Min Stock Level *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("item_min_stock_input")
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
                    modifier = Modifier.fillMaxWidth().testTag("item_warehouse_input")
                )
            }

            item {
                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text("Supplier Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("item_supplier_input")
                )
            }

            // Description
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Specs") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("item_description_input")
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

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (name.isBlank() || sku.isBlank() || barcode.isBlank()) {
                            errorMessage = "Name, SKU, and Barcode are required!"
                            return@Button
                        }
                        val cost = costPrice.toDoubleOrNull() ?: 0.0
                        val sale = salePrice.toDoubleOrNull() ?: 0.0
                        val qty = quantity.toIntOrNull() ?: 0
                        val min = minStock.toIntOrNull() ?: 5

                        val newItem = Item(
                            id = existingItem?.id ?: UUID.randomUUID().toString(),
                            sku = sku.trim(),
                            barcode = barcode.trim(),
                            name = name.trim(),
                            category = category.trim().ifEmpty { "General" },
                            description = description.trim(),
                            unit = unit.trim().ifEmpty { "pcs" },
                            costPrice = cost,
                            salePrice = sale,
                            quantity = qty,
                            minStock = min,
                            supplier = supplier.trim(),
                            warehouse = warehouse.trim(),
                            expiryDate = expiryDate.trim(),
                            updatedAt = System.currentTimeMillis()
                        )

                        viewModel.saveItem(newItem, isNew = isNew) {
                            Toast.makeText(context, "Item saved successfully!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_item_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Inventory Item", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
