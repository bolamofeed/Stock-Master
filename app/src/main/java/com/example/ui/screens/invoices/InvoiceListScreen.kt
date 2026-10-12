package com.example.ui.screens.invoices

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Invoice
import com.example.data.model.InvoiceLineItem
import com.example.data.model.InvoiceStatus
import com.example.data.model.InvoiceType
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(
    viewModel: InventoryViewModel,
    onNavigateToCreateInvoice: () -> Unit
) {
    val context = LocalContext.current
    val invoices by viewModel.allInvoices.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()
    val user by viewModel.currentUser.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<InvoiceType?>(null) }
    var selectedStatus by remember { mutableStateOf<InvoiceStatus?>(null) }

    var selectedInvoiceForPreview by remember { mutableStateOf<Invoice?>(null) }
    var selectedInvoiceForPay by remember { mutableStateOf<Invoice?>(null) }
    var selectedInvoiceForReturn by remember { mutableStateOf<Invoice?>(null) }

    val filteredInvoices = remember(invoices, searchQuery, selectedType, selectedStatus) {
        invoices.filter { inv ->
            val matchesQuery = searchQuery.isBlank() ||
                inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                inv.entityName.contains(searchQuery, ignoreCase = true)
            val matchesType = selectedType == null || inv.type == selectedType
            val matchesStatus = selectedStatus == null || inv.status == selectedStatus
            matchesQuery && matchesType && matchesStatus
        }
    }

    val totalSales = invoices.filter { it.type == InvoiceType.SALES }.sumOf { it.totalAmount }
    val totalPurchases = invoices.filter { it.type == InvoiceType.PURCHASE }.sumOf { it.totalAmount }
    val totalPaid = invoices.filter { it.status == InvoiceStatus.PAID }.sumOf { it.totalAmount }
    val unpaidInvoices = invoices.filter { it.status == InvoiceStatus.UNPAID }
    val totalUnpaid = unpaidInvoices.sumOf { it.totalAmount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Invoices & Orders", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = "${invoices.size} total • ${unpaidInvoices.size} pending payment",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (user.role.canCreateInvoices) {
                FloatingActionButton(
                    onClick = onNavigateToCreateInvoice,
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("create_invoice_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Invoice")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .testTag("invoices_list_screen")
        ) {
            // Unpaid Invoices Quick Action Alert (if any exist)
            if (unpaidInvoices.isNotEmpty() && selectedStatus != InvoiceStatus.PAID) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { selectedStatus = InvoiceStatus.UNPAID }
                        .testTag("unpaid_invoices_alert_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = "${unpaidInvoices.size} Unpaid Invoices Due",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Outstanding balance: $currency%.2f • Tap to collect".format(totalUnpaid),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { selectedStatus = InvoiceStatus.UNPAID },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("View Due", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }

            // Financial Summary Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InvoiceMetricPill(
                    modifier = Modifier.weight(1f),
                    label = "Total Sales",
                    amount = "$currency%.0f".format(totalSales),
                    color = PrimaryBlue
                )
                InvoiceMetricPill(
                    modifier = Modifier.weight(1f),
                    label = "Purchases",
                    amount = "$currency%.0f".format(totalPurchases),
                    color = Color(0xFF8B5CF6)
                )
                InvoiceMetricPill(
                    modifier = Modifier.weight(1f),
                    label = "Collected (Paid)",
                    amount = "$currency%.0f".format(totalPaid),
                    color = EmeraldGreen
                )
                InvoiceMetricPill(
                    modifier = Modifier.weight(1f),
                    label = "Due (Unpaid)",
                    amount = "$currency%.0f".format(totalUnpaid),
                    color = if (totalUnpaid > 0) AmberWarning else Color.Gray
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("invoice_search_input"),
                placeholder = { Text("Search by invoice # or client name...") },
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Type Filter Chips (All, Sales, Purchases)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = { Text("All Types", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedType == InvoiceType.SALES,
                        onClick = { selectedType = if (selectedType == InvoiceType.SALES) null else InvoiceType.SALES },
                        label = { Text("Sales (${invoices.count { it.type == InvoiceType.SALES }})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedType == InvoiceType.PURCHASE,
                        onClick = { selectedType = if (selectedType == InvoiceType.PURCHASE) null else InvoiceType.PURCHASE },
                        label = { Text("Purchases (${invoices.count { it.type == InvoiceType.PURCHASE }})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF8B5CF6),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Status Filter Chips (All, Unpaid, Paid, Partially Returned, Returned, Cancelled)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("All Statuses (${invoices.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                items(InvoiceStatus.entries) { status ->
                    val count = invoices.count { it.status == status }
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = if (selectedStatus == status) null else status },
                        label = { Text("${status.title} ($count)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (status) {
                                InvoiceStatus.UNPAID -> AmberWarning
                                InvoiceStatus.PAID -> EmeraldGreen
                                InvoiceStatus.PARTIALLY_RETURNED -> Color(0xFFF97316)
                                InvoiceStatus.RETURNED -> Color(0xFF9333EA)
                                InvoiceStatus.CANCELLED -> DangerRed
                                InvoiceStatus.DRAFT -> Color.Gray
                            },
                            selectedLabelColor = if (status == InvoiceStatus.UNPAID) Color.Black else Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Invoices List
            if (filteredInvoices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No invoices match criteria", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Tap + to create a sales invoice or purchase order.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredInvoices, key = { it.id }) { invoice ->
                        InvoiceItemCard(
                            invoice = invoice,
                            currencySymbol = currency,
                            onClick = { selectedInvoiceForPreview = invoice },
                            onPayClick = { selectedInvoiceForPay = invoice },
                            onReturnClick = { selectedInvoiceForReturn = invoice }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // PDF Preview Dialog
        selectedInvoiceForPreview?.let { inv ->
            PdfInvoicePreviewDialog(
                invoice = inv,
                currencySymbol = currency,
                onDismiss = { selectedInvoiceForPreview = null },
                onPayInvoice = { toPay -> selectedInvoiceForPay = toPay },
                onReturnInvoice = { toReturn -> selectedInvoiceForReturn = toReturn }
            )
        }

        // Pay Invoice Dialog
        selectedInvoiceForPay?.let { inv ->
            PayInvoiceDialog(
                invoice = inv,
                currencySymbol = currency,
                onDismiss = { selectedInvoiceForPay = null },
                onConfirmPay = { paymentMethod ->
                    viewModel.payInvoice(inv.id, paymentMethod) { result ->
                        result.onSuccess {
                            Toast.makeText(context, "Invoice #${inv.invoiceNumber} marked as PAID via $paymentMethod! Inventory updated.", Toast.LENGTH_LONG).show()
                            selectedInvoiceForPay = null
                        }.onFailure { err ->
                            Toast.makeText(context, "Failed to record payment: ${err.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        // Return Invoice Dialog
        selectedInvoiceForReturn?.let { inv ->
            ReturnInvoiceDialog(
                invoice = inv,
                currencySymbol = currency,
                onDismiss = { selectedInvoiceForReturn = null },
                onConfirmReturn = { itemsToReturn, reason ->
                    viewModel.returnInvoice(inv.id, itemsToReturn, reason) { result ->
                        result.onSuccess {
                            Toast.makeText(context, "Return processed successfully! Warehouse inventory restored.", Toast.LENGTH_LONG).show()
                            selectedInvoiceForReturn = null
                        }.onFailure { err ->
                            Toast.makeText(context, "Failed to process return: ${err.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun InvoiceMetricPill(
    modifier: Modifier = Modifier,
    label: String,
    amount: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(amount, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun InvoiceItemCard(
    invoice: Invoice,
    currencySymbol: String,
    onClick: () -> Unit,
    onPayClick: () -> Unit,
    onReturnClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val statusColor = when (invoice.status) {
        InvoiceStatus.PAID -> EmeraldGreen
        InvoiceStatus.UNPAID -> AmberWarning
        InvoiceStatus.CANCELLED -> DangerRed
        InvoiceStatus.DRAFT -> Color.Gray
        InvoiceStatus.PARTIALLY_RETURNED -> Color(0xFFF97316)
        InvoiceStatus.RETURNED -> Color(0xFF9333EA)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
            .testTag("invoice_card_${invoice.invoiceNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Invoice #, Type Badge, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "#${invoice.invoiceNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = (if (invoice.type == InvoiceType.SALES) PrimaryBlue else Color(0xFF8B5CF6)).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (invoice.type == InvoiceType.SALES) "SALES" else "PURCHASE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (invoice.type == InvoiceType.SALES) PrimaryBlue else Color(0xFF8B5CF6),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = invoice.status.title,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Client / Vendor Name
            Text(
                text = invoice.entityName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Items Summary
            val lineItems = invoice.parseLineItems()
            if (lineItems.isNotEmpty()) {
                Text(
                    text = lineItems.joinToString(" • ") { "${it.quantity}x ${it.itemName}" },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            } else if (invoice.itemsJson.isNotBlank()) {
                Text(
                    text = invoice.itemsJson,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: Date, Total Amount, and Quick Actions (Pay, Return, PDF)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateFormat.format(Date(invoice.createdAt)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol%.2f".format(invoice.totalAmount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = PrimaryBlue
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Pay Button if UNPAID
                    if (invoice.status == InvoiceStatus.UNPAID) {
                        Button(
                            onClick = onPayClick,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp).testTag("pay_invoice_btn_${invoice.invoiceNumber}")
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Return Button if PAID or PARTIALLY_RETURNED
                    if (invoice.status == InvoiceStatus.PAID || invoice.status == InvoiceStatus.PARTIALLY_RETURNED) {
                        OutlinedButton(
                            onClick = onReturnClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp).testTag("return_invoice_btn_${invoice.invoiceNumber}")
                        ) {
                            Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Return", fontSize = 12.sp)
                        }
                    }

                    // PDF Icon preview button
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "View PDF",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PayInvoiceDialog(
    invoice: Invoice,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirmPay: (paymentMethod: String) -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }
    var referenceNote by remember { mutableStateOf("") }
    val paymentMethods = listOf("Cash", "Credit Card", "Bank Transfer", "Mobile Payment (Stripe / POS)")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = EmeraldGreen)
                Text("Pay Invoice #${invoice.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Entity / Customer:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(invoice.entityName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Amount Due:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("$currencySymbol%.2f".format(invoice.totalAmount), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldGreen)
                        }
                    }
                }

                Text("Select Payment Method:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                Column {
                    paymentMethods.forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPaymentMethod = method }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPaymentMethod == method,
                                onClick = { selectedPaymentMethod = method }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(method, fontSize = 13.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = referenceNote,
                    onValueChange = { referenceNote = it },
                    label = { Text("Transaction Reference / Notes (Optional)") },
                    placeholder = { Text("e.g. TXN-89412 or Check #33") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fullMethod = if (referenceNote.isNotBlank()) "$selectedPaymentMethod ($referenceNote)" else selectedPaymentMethod
                    onConfirmPay(fullMethod)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.testTag("confirm_pay_btn")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReturnInvoiceDialog(
    invoice: Invoice,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirmReturn: (itemsToReturn: List<Pair<String, Int>>, reason: String) -> Unit
) {
    val lineItems = remember(invoice) { invoice.parseLineItems() }
    var isFullReturn by remember { mutableStateOf(true) }
    var selectedReason by remember { mutableStateOf("Customer Request") }
    var customReason by remember { mutableStateOf("") }

    val returnQuantities = remember(lineItems) {
        mutableStateMapOf<String, Int>().apply {
            lineItems.forEach { item ->
                put(item.itemId, item.remainingQuantity)
            }
        }
    }

    val reasonsList = listOf("Customer Request", "Defective / Damaged", "Wrong Item Delivered", "Order Canceled", "Other")

    val returnTotalAmount = remember(isFullReturn, returnQuantities.toMap()) {
        if (isFullReturn) {
            lineItems.sumOf { it.unitPrice * it.remainingQuantity }
        } else {
            lineItems.sumOf { item ->
                val qty = returnQuantities[item.itemId] ?: 0
                item.unitPrice * qty
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AssignmentReturn, contentDescription = null, tint = Color(0xFFF97316))
                Text("Return Invoice #${invoice.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Return Mode:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isFullReturn,
                        onClick = {
                            isFullReturn = true
                            lineItems.forEach { returnQuantities[it.itemId] = it.remainingQuantity }
                        },
                        label = { Text("Full Return (All Items)") }
                    )

                    FilterChip(
                        selected = !isFullReturn,
                        onClick = { isFullReturn = false },
                        label = { Text("Partial Return") }
                    )
                }

                if (!isFullReturn) {
                    Text("Select Quantities to Return:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(lineItems) { item ->
                                val returnQty = returnQuantities[item.itemId] ?: 0
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.itemName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Remaining: ${item.remainingQuantity} • $currencySymbol%.2f ea".format(item.unitPrice), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    val current = returnQuantities[item.itemId] ?: 0
                                                    if (current > 0) returnQuantities[item.itemId] = current - 1
                                                },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                            }

                                            Text(
                                                text = "$returnQty",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            )

                                            IconButton(
                                                onClick = {
                                                    val current = returnQuantities[item.itemId] ?: 0
                                                    if (current < item.remainingQuantity) returnQuantities[item.itemId] = current + 1
                                                },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text("Reason for Return:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(reasonsList) { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            label = { Text(reason, fontSize = 11.sp) }
                        )
                    }
                }

                if (selectedReason == "Other") {
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        label = { Text("Specify Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Restocked Value:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("$currencySymbol%.2f".format(returnTotalAmount), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF97316))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (selectedReason == "Other") customReason.ifBlank { "Other reason" } else selectedReason
                    val itemsToReturn = if (isFullReturn) {
                        lineItems.map { it.itemId to it.remainingQuantity }.filter { it.second > 0 }
                    } else {
                        returnQuantities.entries.map { it.key to it.value }.filter { it.second > 0 }
                    }

                    if (itemsToReturn.isEmpty()) {
                        return@Button
                    }
                    onConfirmReturn(itemsToReturn, finalReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                modifier = Modifier.testTag("confirm_return_btn")
            ) {
                Text("Confirm Return & Restock")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
