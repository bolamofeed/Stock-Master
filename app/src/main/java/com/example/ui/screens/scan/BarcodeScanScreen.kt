package com.example.ui.screens.scan

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.Item
import com.example.ui.components.Code128BarcodeView
import com.example.ui.components.QrCodeView
import com.example.ui.screens.inventory.StockAdjustDialog
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.BarcodeGenerator

enum class ScanWorkflowMode(val label: String) {
    LOOKUP("Lookup Details"),
    ADJUST_STOCK("Adjust Stock"),
    INVOICE("Add to Invoice")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScanScreen(
    viewModel: InventoryViewModel,
    onNavigateToItemDetail: (String) -> Unit,
    onNavigateToAddItemWithBarcode: (String) -> Unit,
    onNavigateToCreateInvoiceWithItem: (String) -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.rawItems.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Scanner, 1 = Barcode Generator
    var workflowMode by remember { mutableStateOf(ScanWorkflowMode.LOOKUP) }
    var manualBarcodeInput by remember { mutableStateOf("") }

    var matchedItem by remember { mutableStateOf<Item?>(null) }
    var unknownBarcodeScanned by remember { mutableStateOf<String?>(null) }
    var showAdjustDialogForItem by remember { mutableStateOf<Item?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isCameraActive by remember { mutableStateOf(false) }

    DisposableEffect(isCameraActive, lifecycleOwner) {
        onDispose {
            if (isCameraActive) {
                try {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    if (cameraProviderFuture.isDone) {
                        cameraProviderFuture.get().unbindAll()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            isCameraActive = true
        }
    }

    fun handleBarcodeDiscovered(code: String) {
        val trimmed = code.trim()
        val found = items.find { it.barcode.equals(trimmed, ignoreCase = true) || it.sku.equals(trimmed, ignoreCase = true) }
        if (found != null) {
            when (workflowMode) {
                ScanWorkflowMode.LOOKUP -> matchedItem = found
                ScanWorkflowMode.ADJUST_STOCK -> showAdjustDialogForItem = found
                ScanWorkflowMode.INVOICE -> {
                    Toast.makeText(context, "Added ${found.name} to invoice!", Toast.LENGTH_SHORT).show()
                    onNavigateToCreateInvoiceWithItem(found.id)
                }
            }
        } else {
            unknownBarcodeScanned = trimmed
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Barcode & QR Hub", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .testTag("scan_hub_screen")
        ) {
            // Tab Selector: Scanner vs Generator
            PrimaryTabRow(
                selectedTabIndex = activeTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Camera & Scanner") },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_scanner")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Barcode Generator") },
                    icon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_generator")
                )
            }

            if (activeTab == 0) {
                // SCANNER TAB
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Workflow Mode Chips (Lookup, Adjust, Invoice)
                    item {
                        Column {
                            Text("Scan Action Mode:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ScanWorkflowMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = workflowMode == mode,
                                        onClick = { workflowMode = mode },
                                        label = { Text(mode.label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryBlue,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("mode_${mode.name}")
                                    )
                                }
                            }
                        }
                    }

                    // Camera Viewfinder Box
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .testTag("camera_viewfinder_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (isCameraActive && hasCameraPermission) {
                                    AndroidView(
                                        factory = { ctx ->
                                            val previewView = PreviewView(ctx).apply {
                                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                            }
                                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                            cameraProviderFuture.addListener({
                                                try {
                                                    val cameraProvider = cameraProviderFuture.get()
                                                    val preview = Preview.Builder().build()
                                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                                    preview.setSurfaceProvider(previewView.surfaceProvider)
                                                    cameraProvider.unbindAll()
                                                    cameraProvider.bindToLifecycle(
                                                        lifecycleOwner,
                                                        cameraSelector,
                                                        preview
                                                    )
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }, ContextCompat.getMainExecutor(ctx))
                                            previewView
                                        },
                                        onRelease = {
                                            try {
                                                ProcessCameraProvider.getInstance(context).get().unbindAll()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Stop Camera Button
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(10.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                isCameraActive = false
                                                try {
                                                    ProcessCameraProvider.getInstance(context).get().unbindAll()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            },
                                        color = Color(0xCC000000)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Close Camera", tint = Color.White, modifier = Modifier.size(14.dp))
                                            Text("Stop Cam", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF0F172A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CameraAlt,
                                                contentDescription = null,
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Text("Optical Barcode Scanner", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Scan with device camera or use simulator below", color = Color.Gray, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Button(
                                                onClick = {
                                                    if (!hasCameraPermission) {
                                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                    } else {
                                                        isCameraActive = true
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("activate_camera_button")
                                            ) {
                                                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Start Live Camera", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                // Aiming Reticle Overlay
                                Box(
                                    modifier = Modifier
                                        .size(180.dp, 120.dp)
                                        .align(Alignment.Center)
                                        .border(2.dp, EmeraldGreen, RoundedCornerShape(12.dp))
                                )

                                Surface(
                                    color = Color(0xAA000000),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "Target barcode inside cyan frame",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Manual Code Input or Simulation Presets
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Quick Scanner Simulation / Direct Input",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = manualBarcodeInput,
                                        onValueChange = { manualBarcodeInput = it },
                                        placeholder = { Text("Enter barcode or SKU...") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("manual_barcode_input")
                                    )

                                    Button(
                                        onClick = {
                                            if (manualBarcodeInput.isNotBlank()) {
                                                handleBarcodeDiscovered(manualBarcodeInput)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                        modifier = Modifier.testTag("scan_trigger_button")
                                    ) {
                                        Text("Scan")
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Tap to simulate scanned warehouse items:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(items.take(4)) { item ->
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { handleBarcodeDiscovered(item.barcode) }
                                                .testTag("preset_scan_${item.sku}"),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Text(item.name.take(15), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            } else {
                // GENERATOR & PRINTING TAB
                BarcodeGeneratorViewSection(
                    items = items,
                    currency = currency
                )
            }
        }

        // Matched Item Bottom Sheet Modal
        matchedItem?.let { item ->
            ModalBottomSheet(
                onDismissRequest = { matchedItem = null }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .testTag("matched_item_sheet")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("SKU: ${item.sku} • Barcode: ${item.barcode}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Surface(
                            color = (if (item.quantity > item.minStock) EmeraldGreen else AmberWarning).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${item.quantity} ${item.unit} in stock",
                                fontWeight = FontWeight.Bold,
                                color = if (item.quantity > item.minStock) EmeraldGreen else AmberWarning,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Warehouse: ${item.warehouse} • Price: $currency%.2f".format(item.salePrice), fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                matchedItem = null
                                onNavigateToItemDetail(item.id)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View Details")
                        }

                        Button(
                            onClick = {
                                val itm = item
                                matchedItem = null
                                showAdjustDialogForItem = itm
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Adjust Stock")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Unknown Barcode Scanned Dialog
        unknownBarcodeScanned?.let { code ->
            AlertDialog(
                onDismissRequest = { unknownBarcodeScanned = null },
                title = { Text("New Barcode Detected!") },
                text = {
                    Text("No item in inventory matches barcode '$code'. Would you like to create a new product now?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val c = code
                            unknownBarcodeScanned = null
                            onNavigateToAddItemWithBarcode(c)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Create Item")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { unknownBarcodeScanned = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Stock Adjust Dialog
        showAdjustDialogForItem?.let { item ->
            StockAdjustDialog(
                item = item,
                onDismiss = { showAdjustDialogForItem = null },
                onConfirm = { type, qty, note ->
                    viewModel.recordStockMovement(item, type, qty, note) { res ->
                        res.onSuccess {
                            Toast.makeText(context, "Stock movement recorded!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showAdjustDialogForItem = null
                }
            )
        }
    }
}

@Composable
fun BarcodeGeneratorViewSection(items: List<Item>, currency: String) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf(items.firstOrNull()) }
    var customCodeInput by remember { mutableStateOf("SM-AUTO-9921") }
    var printCopiesCount by remember { mutableIntStateOf(1) } // option to choose number of barcodes to print

    val filteredItems = remember(searchQuery, items) {
        if (searchQuery.isBlank()) items
        else items.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.sku.contains(searchQuery, ignoreCase = true) ||
                    it.barcode.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val activeCode = selectedItem?.barcode ?: customCodeInput

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("barcode_generator_section"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section: Search Items for Barcode Generation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Search & Select Item",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (selectedItem != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PrimaryBlue.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Selected: ${selectedItem?.sku}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Search Input field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("barcode_generator_search_input"),
                        placeholder = { Text("Search by name, SKU, or category...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Filtered Items chips or mini list
                    Text(
                        text = "Found ${filteredItems.size} items:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredItems.take(20), key = { it.id }) { item ->
                            FilterChip(
                                selected = selectedItem?.id == item.id,
                                onClick = {
                                    selectedItem = item
                                    customCodeInput = item.barcode
                                },
                                label = {
                                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text(item.name.take(20), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text("SKU: ${item.sku}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Custom Barcode input override
        item {
            OutlinedTextField(
                value = customCodeInput,
                onValueChange = {
                    customCodeInput = it
                    selectedItem = null
                },
                label = { Text("Barcode String (Code128 / QR)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("custom_code_generator_input")
            )
        }

        // Option to Choose Number of Barcodes to Print
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Print Quantity / Copies", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("How many barcode labels do you want to print?", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Stepper: - [Count] +
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { if (printCopiesCount > 1) printCopiesCount-- },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .testTag("decrease_print_qty_btn")
                            ) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "$printCopiesCount",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }

                            IconButton(
                                onClick = { if (printCopiesCount < 100) printCopiesCount++ },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(PrimaryBlue, CircleShape)
                                    .testTag("increase_print_qty_btn")
                            ) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            }
                        }
                    }

                    // Preset chips for rapid selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 3, 5, 10, 20, 50).forEach { qty ->
                            FilterChip(
                                selected = printCopiesCount == qty,
                                onClick = { printCopiesCount = qty },
                                label = { Text("$qty pcs", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Code 128 Display
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Code 128 Linear Barcode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Code128BarcodeView(data = activeCode, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // QR Code Display
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("QR Code 2D Matrix", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    QrCodeView(data = activeCode, sizeDp = 160)
                }
            }
        }

        // Print Label Action Button with print count confirmation
        item {
            Button(
                onClick = {
                    val itemName = selectedItem?.name ?: "Custom Label"
                    val sku = selectedItem?.sku ?: activeCode
                    val price = selectedItem?.salePrice ?: 0.0
                    val warehouse = selectedItem?.warehouse ?: "Main Hub"

                    // Generates batch multi-label printable sheet for the chosen number of copies
                    BarcodeGenerator.createMultiLabelSheetBitmap(
                        name = itemName,
                        sku = sku,
                        barcode = activeCode,
                        price = price,
                        warehouse = warehouse,
                        currencySymbol = currency,
                        quantity = printCopiesCount
                    )

                    Toast.makeText(
                        context,
                        "Generated & queued $printCopiesCount barcode label(s) for $itemName!",
                        Toast.LENGTH_LONG
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("print_generated_barcode_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Print $printCopiesCount Barcode Label${if (printCopiesCount > 1) "s" else ""} (PDF Sheet)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

