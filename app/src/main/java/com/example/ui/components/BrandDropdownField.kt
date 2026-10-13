package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.model.Brand
import com.example.data.model.CurrencyUtils
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.InventoryViewModel

/**
 * Reusable searchable Brand Dropdown with in-form "Add new brand" and "Edit brand" capabilities.
 */
@Composable
fun BrandDropdownField(
    selectedBrandId: String,
    onBrandSelected: (Brand) -> Unit,
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier,
    label: String = "Brand *",
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val brands by viewModel.activeBrands.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()
    val selectedBrand = brands.find { it.id == selectedBrandId }

    var showPickerModal by remember { mutableStateOf(false) }
    var showBrandDialog by remember { mutableStateOf(false) }
    var editingBrand by remember { mutableStateOf<Brand?>(null) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Dropdown Clickable Box
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = enabled) { showPickerModal = true }
        ) {
            OutlinedTextField(
                value = selectedBrand?.name ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text(label) },
                placeholder = { Text("Select Brand") },
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("brand_dropdown_input"),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Edit Selected Brand Button (enabled if brand selected)
        if (selectedBrand != null && enabled) {
            IconButton(
                onClick = {
                    editingBrand = selectedBrand
                    showBrandDialog = true
                },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("edit_selected_brand_button")
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit brand",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Add New Brand Quick Button
        if (enabled) {
            OutlinedButton(
                onClick = {
                    editingBrand = null
                    showBrandDialog = true
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("add_new_brand_quick_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Modal Brand Search & Picker
    if (showPickerModal) {
        BrandPickerModal(
            brands = brands,
            selectedBrandId = selectedBrandId,
            currencySymbol = currency,
            onSelect = { brand ->
                onBrandSelected(brand)
                showPickerModal = false
            },
            onAddNew = {
                editingBrand = null
                showBrandDialog = true
                showPickerModal = false
            },
            onDismiss = { showPickerModal = false }
        )
    }

    // Add / Edit Brand Form Dialog
    if (showBrandDialog) {
        BrandFormDialog(
            brand = editingBrand,
            currencySymbol = currency,
            onDismiss = { showBrandDialog = false },
            onSave = { name, costMinor, saleMinor ->
                if (editingBrand != null) {
                    viewModel.updateBrand(
                        id = editingBrand!!.id,
                        name = name,
                        costPriceMinor = costMinor,
                        salePriceMinor = saleMinor
                    ) { result ->
                        result.onSuccess { updated ->
                            Toast.makeText(context, "Brand '${updated.name}' updated!", Toast.LENGTH_SHORT).show()
                            onBrandSelected(updated)
                            showBrandDialog = false
                        }.onFailure { err ->
                            Toast.makeText(context, err.message ?: "Failed to update brand", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    viewModel.saveBrand(
                        name = name,
                        costPriceMinor = costMinor,
                        salePriceMinor = saleMinor
                    ) { result ->
                        result.onSuccess { created ->
                            Toast.makeText(context, "Brand '${created.name}' created!", Toast.LENGTH_SHORT).show()
                            onBrandSelected(created)
                            showBrandDialog = false
                        }.onFailure { err ->
                            Toast.makeText(context, err.message ?: "Failed to create brand", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}

/**
 * Searchable modal to pick an existing brand or create a new one.
 */
@Composable
fun BrandPickerModal(
    brands: List<Brand>,
    selectedBrandId: String,
    currencySymbol: String,
    onSelect: (Brand) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(brands, searchQuery) {
        if (searchQuery.isBlank()) brands
        else brands.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Sell, contentDescription = null, tint = PrimaryBlue)
                    Text("Select Brand", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                OutlinedButton(
                    onClick = onAddNew,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("picker_modal_add_new_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add New", fontSize = 11.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search brand name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("brand_picker_search_input")
                )

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No brands found matching '$searchQuery'", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onAddNew) {
                                Text("Add as New Brand")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered, key = { it.id }) { brand ->
                            val isSelected = brand.id == selectedBrandId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(brand) }
                                    .testTag("brand_picker_item_${brand.id}"),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryBlue.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(brand.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "Default Sale: ${CurrencyUtils.formatMinorUnits(brand.defaultSalePriceMinor, currencySymbol)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        color = if (isSelected) PrimaryBlue else EmeraldGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isSelected) "Selected" else "Pick",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.surface else EmeraldGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Small dialog to Add or Edit Brand (name, default cost price, default selling price).
 */
@Composable
fun BrandFormDialog(
    brand: Brand?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (name: String, costMinor: Long, saleMinor: Long) -> Unit
) {
    val isEdit = brand != null
    var name by remember { mutableStateOf(brand?.name ?: "") }
    var costPriceText by remember {
        mutableStateOf(if (brand != null) "%.2f".format(brand.defaultCostPriceMinor / 100.0) else "0.00")
    }
    var salePriceText by remember {
        mutableStateOf(if (brand != null) "%.2f".format(brand.defaultSalePriceMinor / 100.0) else "0.00")
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit Brand" else "Add New Brand",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Brand Name * (2-50 chars)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("brand_form_name_input")
                )

                OutlinedTextField(
                    value = costPriceText,
                    onValueChange = { costPriceText = it },
                    label = { Text("Default Cost Price ($currencySymbol) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("brand_form_cost_input")
                )

                OutlinedTextField(
                    value = salePriceText,
                    onValueChange = { salePriceText = it },
                    label = { Text("Default Selling Price ($currencySymbol) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("brand_form_sale_input")
                )

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.length < 2 || cleanName.length > 50) {
                        error = "Brand name must be between 2 and 50 characters."
                        return@Button
                    }
                    val costMinor = CurrencyUtils.parseToMinorUnits(costPriceText)
                    val saleMinor = CurrencyUtils.parseToMinorUnits(salePriceText)

                    onSave(cleanName, costMinor, saleMinor)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.testTag("brand_form_save_btn")
            ) {
                Text(if (isEdit) "Update Brand" else "Create Brand")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
