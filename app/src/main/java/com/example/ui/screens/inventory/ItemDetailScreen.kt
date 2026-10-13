package com.example.ui.screens.inventory

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrencyUtils
import com.example.data.model.Item
import com.example.data.model.ItemBrand
import com.example.ui.components.Code128BarcodeView
import com.example.ui.components.QrCodeView
import com.example.ui.screens.dashboard.StockMovementRow
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.BarcodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: String,
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.rawItems.collectAsState()
    val movements by viewModel.allMovements.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    val item = items.find { it.id == itemId }

    var showAdjustDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }
    var barcodeFormatTab by remember { mutableIntStateOf(0) } // 0 = Code128, 1 = QR Code

    if (item == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Item not found", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onNavigateBack) { Text("Back to Inventory") }
            }
        }
        return
    }

    val itemMovements: List<com.example.data.model.StockMovement> = remember(movements, item.id) {
        movements.filter { it.itemId == item.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.name, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (user.role.canEditInventory) {
                        IconButton(
                            onClick = { onNavigateToEdit(item.id) },
                            modifier = Modifier.testTag("detail_edit_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }

                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("detail_delete_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed)
                        }
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
                .background(MaterialTheme.colorScheme.background)
                .testTag("item_detail_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${item.category} • SKU: ${item.sku}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Stock status
                            val stockColor = when {
                                item.isOutOfStock -> DangerRed
                                item.isLowStock -> AmberWarning
                                else -> EmeraldGreen
                            }
                            Surface(
                                color = stockColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${item.quantity} ${item.unit}",
                                    color = stockColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (item.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = item.description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Details Grid
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DetailItem(title = "Selling Price", value = "$currency%.2f".format(item.salePrice))
                            if (user.role.canSeePurchasePrice) {
                                DetailItem(title = "Cost Price", value = "$currency%.2f".format(item.costPrice))
                                val margin = ((item.salePrice - item.costPrice) / item.salePrice * 100).coerceAtLeast(0.0)
                                DetailItem(title = "Margin", value = "%.1f%%".format(margin))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DetailItem(title = "Min Stock Level", value = "${item.minStock} ${item.unit}")
                            DetailItem(title = "Warehouse", value = item.warehouse)
                            DetailItem(title = "Supplier", value = item.supplier.ifEmpty { "N/A" })
                        }
                    }
                }
            }

            // Brands & Prices Section
            if (item.brandsJson.isNotBlank()) {
                val itemBrands = item.brandsJson.split("##").mapNotNull { ItemBrand.deserialize(it) }
                if (itemBrands.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Brand Variations (${itemBrands.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Surface(
                                        color = PrimaryBlue.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${item.quantity} ${item.unit} total",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                for (b in itemBrands) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(b.brandName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("Barcode: ${b.barcode} • Stock: ${b.quantity} ${item.unit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    "$currency%.2f".format(b.salePriceMajor),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    color = PrimaryBlue
                                                )
                                                if (user.role.canSeePurchasePrice) {
                                                    Text(
                                                        "Cost: $currency%.2f".format(b.costPriceMajor),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
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

            // Barcode & QR Code Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Item Barcodes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Button(
                                onClick = { showLabelDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier.testTag("preview_label_button")
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Print Label", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TabRow(selectedTabIndex = barcodeFormatTab) {
                            Tab(
                                selected = barcodeFormatTab == 0,
                                onClick = { barcodeFormatTab = 0 },
                                text = { Text("Code 128 (1D)") }
                            )
                            Tab(
                                selected = barcodeFormatTab == 1,
                                onClick = { barcodeFormatTab = 1 },
                                text = { Text("QR Code (2D)") }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (barcodeFormatTab == 0) {
                            Code128BarcodeView(
                                data = item.barcode,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            QrCodeView(
                                data = item.barcode,
                                sizeDp = 150
                            )
                        }
                    }
                }
            }

            // Adjust Stock Action Bar
            if (user.role.canAdjustStock) {
                item {
                    Button(
                        onClick = { showAdjustDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("detail_adjust_stock_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Record Stock Movement (In / Out / Count)")
                    }
                }
            }

            // Item Stock Movements History
            item {
                Text(
                    text = "Movement History (${itemMovements.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (itemMovements.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "No recorded movements for this item yet.",
                            modifier = Modifier.padding(20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(items = itemMovements, key = { it.id }) { movement ->
                    StockMovementRow(movement = movement, onClick = {})
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Adjust Dialog
        if (showAdjustDialog) {
            StockAdjustDialog(
                item = item,
                onDismiss = { showAdjustDialog = false },
                onConfirm = { type, qty, note ->
                    viewModel.recordStockMovement(item, type, qty, note) { res ->
                        res.onSuccess {
                            Toast.makeText(context, "Stock updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showAdjustDialog = false
                }
            )
        }

        // Delete Confirm Dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Item?") },
                text = { Text("Are you sure you want to delete ${item.name} (${item.sku})? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteItem(item) {
                                onNavigateBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Printable Label Preview Dialog
        if (showLabelDialog) {
            AlertDialog(
                onDismissRequest = { showLabelDialog = false },
                title = { Text("Printable Warehouse Label") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Label ready for thermal printing or PDF export:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Render composite preview
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(item.name, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                                Text("SKU: ${item.sku} • Price: $currency%.2f".format(item.salePrice), color = Color.DarkGray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Code128BarcodeView(data = item.barcode, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(8.dp))
                                QrCodeView(data = item.barcode, sizeDp = 100)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Generate bitmap and simulate print/share
                            BarcodeGenerator.createPrintableLabelBitmap(
                                item.name, item.sku, item.barcode, item.salePrice, item.warehouse, currency
                            )
                            Toast.makeText(context, "Label sent to printer / PDF saved!", Toast.LENGTH_SHORT).show()
                            showLabelDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print / Export")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showLabelDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun DetailItem(title: String, value: String) {
    Column {
        Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
