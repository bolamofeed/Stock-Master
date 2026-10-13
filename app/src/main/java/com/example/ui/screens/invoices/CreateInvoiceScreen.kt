package com.example.ui.screens.invoices

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import com.example.data.model.Client
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyUtils
import com.example.data.model.InvoiceLineItem
import com.example.data.model.InvoiceType
import com.example.data.model.Item
import com.example.data.model.ItemBrand
import com.example.ui.components.Code128BarcodeView
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceScreen(
    initialItemId: String? = null,
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val catalogItems by viewModel.rawItems.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    var invoiceType by remember { mutableStateOf(InvoiceType.SALES) }
    var entityName by remember { mutableStateOf("") }
    var entityContact by remember { mutableStateOf("") }
    var taxRate by remember { mutableStateOf("15.0") }
    var discountRate by remember { mutableStateOf("0.0") }
    var notes by remember { mutableStateOf("") }
    var markPaidAndUpdateStock by remember { mutableStateOf(true) }

    val lineItems = remember { mutableStateListOf<InvoiceLineItem>() }

    // Clients directory from ViewModel
    val existingClients by viewModel.allClients.collectAsState()
    var showCustomerSelectDialog by remember { mutableStateOf(false) }
    var saveCustomer by remember { mutableStateOf(true) }

    // State for the two item addition options
    var showSearchDialog by remember { mutableStateOf(false) }
    var showBarcodeDialog by remember { mutableStateOf(false) }

    // Preload initial item if passed from barcode scan
    remember(initialItemId, catalogItems) {
        if (initialItemId != null && lineItems.isEmpty()) {
            val item = catalogItems.find { it.id == initialItemId }
            if (item != null) {
                lineItems.add(
                    InvoiceLineItem(
                        itemId = item.id,
                        itemName = item.name,
                        sku = item.sku,
                        unitPrice = if (invoiceType == InvoiceType.SALES) item.salePrice else item.costPrice,
                        quantity = 1
                    )
                )
            }
        }
    }

    var pendingItemForBrandSelection by remember { mutableStateOf<Item?>(null) }

    // Helper to add item by Item object and optional ItemBrand variation
    fun addItemToInvoice(item: Item, brand: ItemBrand? = null, quantityToAdd: Int = 1) {
        val brandId = brand?.brandId ?: ""
        val brandName = brand?.brandName ?: ""
        val unitPrice = if (brand != null) {
            if (invoiceType == InvoiceType.SALES) brand.salePriceMajor else brand.costPriceMajor
        } else {
            if (invoiceType == InvoiceType.SALES) item.salePrice else item.costPrice
        }
        val unitPriceMinor = if (brand != null) {
            if (invoiceType == InvoiceType.SALES) brand.salePriceMinor else brand.costPriceMinor
        } else {
            CurrencyUtils.toMinorUnits(unitPrice)
        }

        val existingIndex = lineItems.indexOfFirst { it.itemId == item.id && it.brandId == brandId }
        if (existingIndex >= 0) {
            val existing = lineItems[existingIndex]
            val newQty = existing.quantity + quantityToAdd
            lineItems[existingIndex] = existing.copy(
                quantity = newQty,
                total = newQty * unitPrice
            )
        } else {
            lineItems.add(
                InvoiceLineItem(
                    itemId = item.id,
                    itemName = item.name,
                    sku = item.sku,
                    unitPrice = unitPrice,
                    quantity = quantityToAdd,
                    returnedQuantity = 0,
                    total = quantityToAdd * unitPrice,
                    brandId = brandId,
                    brandName = brandName,
                    unitPriceMinor = unitPriceMinor
                )
            )
        }
    }

    val subtotal = lineItems.sumOf { it.total }
    val discountPercent = discountRate.toDoubleOrNull() ?: 0.0
    val discountAmount = subtotal * (discountPercent / 100.0)
    val discountedSubtotal = subtotal - discountAmount
    val taxPercent = taxRate.toDoubleOrNull() ?: 15.0
    val taxAmount = discountedSubtotal * (taxPercent / 100.0)
    val totalAmount = discountedSubtotal + taxAmount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Invoice", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("create_invoice_back_btn")) {
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
                .testTag("create_invoice_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Type Toggle: Sales vs Purchase
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = invoiceType == InvoiceType.SALES,
                        onClick = { invoiceType = InvoiceType.SALES },
                        label = { Text("Sales Invoice (Client)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).testTag("type_sales_chip")
                    )

                    FilterChip(
                        selected = invoiceType == InvoiceType.PURCHASE,
                        onClick = { invoiceType = InvoiceType.PURCHASE },
                        label = { Text("Purchase Order (Vendor)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8B5CF6),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).testTag("type_purchase_chip")
                    )
                }
            }

            // Customer or Vendor Details with Past Customers Search & Auto-Add Option
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (invoiceType == InvoiceType.SALES) PrimaryBlue.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (invoiceType == InvoiceType.SALES) Icons.Default.Person else Icons.Default.History,
                                    contentDescription = null,
                                    tint = if (invoiceType == InvoiceType.SALES) PrimaryBlue else Color(0xFF8B5CF6),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (invoiceType == InvoiceType.SALES) "Customer Information" else "Vendor Information",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            // If Sales Invoice, provide button to select past customer
                            if (invoiceType == InvoiceType.SALES) {
                                OutlinedButton(
                                    onClick = { showCustomerSelectDialog = true },
                                    modifier = Modifier.testTag("select_past_customer_btn"),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Select Past Client", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Client Name Input
                        OutlinedTextField(
                            value = entityName,
                            onValueChange = { entityName = it },
                            label = { Text(if (invoiceType == InvoiceType.SALES) "Customer / Business Name *" else "Supplier / Vendor Name *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("invoice_entity_name_input")
                        )

                        // Client Phone / Contact Input
                        OutlinedTextField(
                            value = entityContact,
                            onValueChange = { entityContact = it },
                            label = { Text(if (invoiceType == InvoiceType.SALES) "Phone Number (e.g. +1 555-0192) *" else "Phone / Email (Optional)") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("invoice_entity_contact_input")
                        )

                        // Toggle 'Save customer' in Sales Invoice
                        if (invoiceType == InvoiceType.SALES) {
                            val isExistingClient = existingClients.any {
                                it.name.equals(entityName.trim(), ignoreCase = true) ||
                                        (entityContact.isNotBlank() && it.phone.equals(entityContact.trim(), ignoreCase = true))
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (saveCustomer) PrimaryBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (saveCustomer) PrimaryBlue.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (saveCustomer) Icons.Default.PersonAdd else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (saveCustomer) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Save customer",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (saveCustomer) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isExistingClient) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = EmeraldGreen.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "Existing Record",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = EmeraldGreen,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = if (saveCustomer) {
                                                    if (isExistingClient)
                                                        "Updates customer purchase history and date via database transaction."
                                                    else
                                                        "Creates new customer record in database via transaction to maintain data integrity."
                                                } else {
                                                    "Inactive: invoice will be processed without saving or updating customer record."
                                                },
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = saveCustomer,
                                        onCheckedChange = { saveCustomer = it },
                                        modifier = Modifier.testTag("save_customer_toggle")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Line Items Section Header & The Two Add Options
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Line Items (${lineItems.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (lineItems.isNotEmpty()) {
                            Text(
                                text = "Subtotal: $currency%.2f".format(subtotal),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue
                            )
                        }
                    }

                    // TWO EXPLICIT OPTIONS: 1. Search Catalog | 2. Scan Barcode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option 1: Search Catalog
                        OutlinedButton(
                            onClick = { showSearchDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_add_item_search")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("1. Search Item", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Option 2: Scan Barcode
                        Button(
                            onClick = { showBarcodeDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_add_item_barcode")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("2. Scan Barcode", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Selected Line Items List
            if (lineItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                            Text(
                                text = "No items in this invoice yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Use '1. Search Item' to look up products or '2. Scan Barcode' to instantly scan product barcodes.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(lineItems) { lineItem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(lineItem.itemName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (lineItem.brandName.isNotBlank()) {
                                        Surface(
                                            color = PrimaryBlue.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = lineItem.brandName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryBlue,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "SKU: ${lineItem.sku} • $currency%.2f each".format(lineItem.unitPrice),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Qty Buttons (+ / - / Delete)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val idx = lineItems.indexOf(lineItem)
                                        if (lineItem.quantity > 1) {
                                            lineItems[idx] = lineItem.copy(
                                                quantity = lineItem.quantity - 1,
                                                total = (lineItem.quantity - 1) * lineItem.unitPrice
                                            )
                                        } else {
                                            lineItems.removeAt(idx)
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = "${lineItem.quantity}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                IconButton(
                                    onClick = {
                                        val idx = lineItems.indexOf(lineItem)
                                        lineItems[idx] = lineItem.copy(
                                            quantity = lineItem.quantity + 1,
                                            total = (lineItem.quantity + 1) * lineItem.unitPrice
                                        )
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "$currency%.2f".format(lineItem.total),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }
                }
            }

            // Financial Calculations
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = taxRate,
                        onValueChange = { taxRate = it },
                        label = { Text("Tax Rate (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("tax_rate_input")
                    )

                    OutlinedTextField(
                        value = discountRate,
                        onValueChange = { discountRate = it },
                        label = { Text("Discount (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("discount_rate_input")
                    )
                }
            }

            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal (${lineItems.sumOf { it.quantity }} items):", fontSize = 12.sp)
                            Text("$currency%.2f".format(subtotal), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (discountAmount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount (${discountPercent}%):", fontSize = 12.sp)
                                Text("-$currency%.2f".format(discountAmount), fontSize = 12.sp, color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("VAT / Tax (${taxPercent}%):", fontSize = 12.sp)
                            Text("+$currency%.2f".format(taxAmount), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL AMOUNT:", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text("$currency%.2f".format(totalAmount), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = PrimaryBlue)
                        }
                    }
                }
            }

            // Auto-update stock checkbox
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = markPaidAndUpdateStock,
                        onCheckedChange = { markPaidAndUpdateStock = it },
                        modifier = Modifier.testTag("auto_update_stock_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Mark Paid & Automatically Update Stock",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (invoiceType == InvoiceType.SALES) "Deducts units from warehouse" else "Adds units to warehouse",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Confirm Submit Button
            item {
                Button(
                    onClick = {
                        if (lineItems.isEmpty()) {
                            Toast.makeText(context, "Please add at least one line item!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val clientName = entityName.trim().ifEmpty {
                            if (invoiceType == InvoiceType.SALES) "Direct Customer" else "Main Supplier"
                        }
                        viewModel.createInvoice(
                            type = invoiceType,
                            entityName = clientName,
                            entityContact = entityContact.trim(),
                            lineItems = lineItems.toList(),
                            taxPercent = taxPercent,
                            discountPercent = discountPercent,
                            notes = notes.trim(),
                            markAsPaid = markPaidAndUpdateStock,
                            saveCustomer = saveCustomer
                        ) { result ->
                            result.onSuccess {
                                Toast.makeText(context, "Invoice created & stock synced!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to create invoice", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_invoice_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Invoice & Save", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // DIALOG OPTION 1: Search Catalog
        if (showSearchDialog) {
            ProductSearchDialog(
                catalogItems = catalogItems,
                invoiceType = invoiceType,
                currencySymbol = currency,
                lineItems = lineItems,
                onAddItem = { item ->
                    val brands = if (item.brandsJson.isNotBlank()) {
                        item.brandsJson.split("##").mapNotNull { ItemBrand.deserialize(it) }
                    } else emptyList()

                    if (brands.size > 1) {
                        pendingItemForBrandSelection = item
                        showSearchDialog = false
                    } else if (brands.size == 1) {
                        addItemToInvoice(item, brands.first())
                    } else {
                        addItemToInvoice(item, null)
                    }
                },
                onDismiss = { showSearchDialog = false }
            )
        }

        // DIALOG OPTION 2: Barcode Scanner & Barcode Lookup
        if (showBarcodeDialog) {
            BarcodeScanDialog(
                catalogItems = catalogItems,
                invoiceType = invoiceType,
                currencySymbol = currency,
                onBarcodeScanned = { scannedItem, matchedBrand ->
                    if (matchedBrand != null) {
                        addItemToInvoice(scannedItem, matchedBrand)
                        Toast.makeText(context, "Added ${scannedItem.name} (${matchedBrand.brandName}) to invoice!", Toast.LENGTH_SHORT).show()
                    } else {
                        val brands = if (scannedItem.brandsJson.isNotBlank()) {
                            scannedItem.brandsJson.split("##").mapNotNull { ItemBrand.deserialize(it) }
                        } else emptyList()

                        if (brands.size > 1) {
                            pendingItemForBrandSelection = scannedItem
                            showBarcodeDialog = false
                        } else if (brands.size == 1) {
                            addItemToInvoice(scannedItem, brands.first())
                            Toast.makeText(context, "Added ${scannedItem.name} to invoice!", Toast.LENGTH_SHORT).show()
                        } else {
                            addItemToInvoice(scannedItem, null)
                            Toast.makeText(context, "Added ${scannedItem.name} to invoice!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = { showBarcodeDialog = false }
            )
        }

        // DIALOG OPTION 2.5: Brand Selection Modal for multi-brand items
        if (pendingItemForBrandSelection != null) {
            val item = pendingItemForBrandSelection!!
            val itemBrands = if (item.brandsJson.isNotBlank()) {
                item.brandsJson.split("##").mapNotNull { ItemBrand.deserialize(it) }
            } else emptyList()

            BrandSelectionDialog(
                item = item,
                brands = itemBrands,
                invoiceType = invoiceType,
                currencySymbol = currency,
                onSelectBrand = { brand ->
                    addItemToInvoice(item, brand)
                    pendingItemForBrandSelection = null
                    Toast.makeText(context, "Added ${item.name} (${brand.brandName})!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { pendingItemForBrandSelection = null }
            )
        }

        // DIALOG OPTION 3: Select Past Customer
        if (showCustomerSelectDialog) {
            CustomerSelectDialog(
                clients = existingClients,
                currencySymbol = currency,
                onSelectClient = { client ->
                    entityName = client.name
                    entityContact = client.phone
                    showCustomerSelectDialog = false
                    Toast.makeText(context, "Selected client: ${client.name}", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showCustomerSelectDialog = false }
            )
        }
    }
}

/**
 * Past Customers Selection Dialog with Live Search & Statistics
 */
@Composable
fun CustomerSelectDialog(
    clients: List<Client>,
    currencySymbol: String,
    onSelectClient: (Client) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredClients = remember(searchQuery, clients) {
        if (searchQuery.isBlank()) clients
        else clients.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    val dateFormat = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue)
                    Text("Select Past Customer", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Surface(
                    color = PrimaryBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${filteredClients.size} clients",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().testTag("customer_search_input"),
                    placeholder = { Text("Search by name or phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (filteredClients.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No matching past customers found.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Type new name & phone in the invoice to auto-add.", fontSize = 11.sp, color = PrimaryBlue)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredClients, key = { it.id }) { client ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectClient(client) }
                                    .testTag("client_card_${client.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(client.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp), tint = PrimaryBlue)
                                            Text(client.phone.ifEmpty { "No phone recorded" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        if (client.lastPurchaseDate > 0L) {
                                            Text(
                                                text = "Last purchase: ${dateFormat.format(java.util.Date(client.lastPurchaseDate))}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            color = EmeraldGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Select",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = EmeraldGreen,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$currencySymbol%.2f".format(client.totalPurchasesAmount),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = PrimaryBlue
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
 * Option 1: Search Catalog Dialog
 */
@Composable
fun ProductSearchDialog(
    catalogItems: List<Item>,
    invoiceType: InvoiceType,
    currencySymbol: String,
    lineItems: List<InvoiceLineItem>,
    onAddItem: (Item) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(catalogItems) {
        listOf("All") + catalogItems.map { it.category }.distinct()
    }

    val filtered = remember(catalogItems, searchQuery, selectedCategory) {
        catalogItems.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.sku.contains(searchQuery, ignoreCase = true) ||
                item.barcode.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true)

            val matchesCat = selectedCategory == "All" || item.category == selectedCategory
            matchesQuery && matchesCat
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue)
                Text("Search Product Catalog", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search name, SKU, or category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_product_search_input")
                )

                // Category chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Text(
                    text = "${filtered.size} products found",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Results list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No matching products found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { item ->
                            val currentInInvoice = lineItems.find { it.itemId == item.id }?.quantity ?: 0
                            val price = if (invoiceType == InvoiceType.SALES) item.salePrice else item.costPrice

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
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
                                        Text(
                                            "SKU: ${item.sku} • In Stock: ${item.quantity} ${item.unit}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "$currencySymbol%.2f".format(price),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = PrimaryBlue
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (currentInInvoice > 0) {
                                            Surface(
                                                color = PrimaryBlue.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "${currentInInvoice} in inv",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryBlue,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { onAddItem(item) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp).testTag("select_search_item_${item.id}")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Add", fontSize = 11.sp)
                                        }
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
                Text("Done")
            }
        }
    )
}

/**
 * Option 2: Barcode Scanner Dialog
 */
@Composable
fun BarcodeScanDialog(
    catalogItems: List<Item>,
    invoiceType: InvoiceType,
    currencySymbol: String,
    onBarcodeScanned: (Item, ItemBrand?) -> Unit,
    onDismiss: () -> Unit
) {
    var manualBarcode by remember { mutableStateOf("") }
    var scanResultFeedback by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    fun processBarcode(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return

        // 1. Check brand-level barcodes across all catalog items
        var matchedItem: Item? = null
        var matchedBrand: ItemBrand? = null

        for (item in catalogItems) {
            if (item.brandsJson.isNotBlank()) {
                val brands = item.brandsJson.split("##").mapNotNull { ItemBrand.deserialize(it) }
                val brand = brands.find { it.barcode.equals(trimmed, ignoreCase = true) }
                if (brand != null) {
                    matchedItem = item
                    matchedBrand = brand
                    break
                }
            }
        }

        // 2. Fallback to master item barcode or SKU
        if (matchedItem == null) {
            matchedItem = catalogItems.find {
                it.barcode.equals(trimmed, ignoreCase = true) ||
                it.sku.equals(trimmed, ignoreCase = true)
            }
        }

        if (matchedItem != null) {
            onBarcodeScanned(matchedItem, matchedBrand)
            scanResultFeedback = if (matchedBrand != null) {
                "Added: ${matchedItem.name} [${matchedBrand.brandName}]"
            } else {
                "Added: ${matchedItem.name} (${matchedItem.sku})"
            }
            isError = false
            manualBarcode = ""
        } else {
            scanResultFeedback = "Barcode '$trimmed' not found in catalog"
            isError = true
        }
    }

    // Animated scanner line
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scannerLine"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = EmeraldGreen)
                Text("Scan Product Barcode", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Interactive Viewfinder Simulator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .border(2.dp, PrimaryBlue, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Viewfinder box corners
                    Box(
                        modifier = Modifier
                            .size(160.dp, 80.dp)
                            .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                    )

                    // Laser line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(2.dp)
                            .background(Color(0xFF38BDF8))
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "READY TO SCAN",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Tap a preset barcode below or enter code manually",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp
                        )
                    }
                }

                // Feedback Banner
                scanResultFeedback?.let { msg ->
                    Surface(
                        color = (if (isError) DangerRed else EmeraldGreen).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (isError) Icons.Default.Clear else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isError) DangerRed else EmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = msg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isError) DangerRed else EmeraldGreen
                            )
                        }
                    }
                }

                // Manual Barcode Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = manualBarcode,
                        onValueChange = { manualBarcode = it },
                        placeholder = { Text("Enter / paste barcode or SKU...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { processBarcode(manualBarcode) }),
                        modifier = Modifier.weight(1f).testTag("dialog_barcode_input")
                    )

                    Button(
                        onClick = { processBarcode(manualBarcode) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(52.dp).testTag("dialog_barcode_submit_btn")
                    ) {
                        Text("Add")
                    }
                }

                // Quick Barcode Presets list for instant emulator testing
                Text(
                    text = "Quick Barcode Presets (Tap to Scan):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(catalogItems.take(8)) { item ->
                        val price = if (invoiceType == InvoiceType.SALES) item.salePrice else item.costPrice
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { processBarcode(item.barcode) }
                                .testTag("scan_preset_${item.barcode}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.QrCode, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Barcode: ${item.barcode} • SKU: ${item.sku}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Surface(
                                    color = EmeraldGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "+ Scan",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
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
 * Brand Selection Dialog when an item has multiple brand variations.
 */
@Composable
fun BrandSelectionDialog(
    item: Item,
    brands: List<ItemBrand>,
    invoiceType: InvoiceType,
    currencySymbol: String,
    onSelectBrand: (ItemBrand) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Brand Variation", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(item.name, fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Pick the specific brand for this invoice line:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(brands, key = { it.id }) { brand ->
                        val price = if (invoiceType == InvoiceType.SALES) brand.salePriceMajor else brand.costPriceMajor
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectBrand(brand) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(brand.brandName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Barcode: ${brand.barcode} • In Stock: ${brand.quantity}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "$currencySymbol%.2f".format(price),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = PrimaryBlue
                                    )
                                    Surface(
                                        color = PrimaryBlue.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "+ Select",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                Text("Cancel")
            }
        }
    )
}

