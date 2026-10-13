package com.example.ui.screens.customers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.flow.Flow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.data.model.Invoice
import com.example.data.model.UserRole
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isArabic by viewModel.isArabicLanguage.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()
    val allClients by viewModel.allClients.collectAsState()

    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Access check: Vendors are restricted from client databases
    if (currentUser.role == UserRole.VENDOR) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (isArabic) "إدارة العملاء" else "Customer Management") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = DangerRed, modifier = Modifier.size(48.dp))
                        Text(
                            text = if (isArabic) "غير مصرح بالدخول" else "Access Restricted",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DangerRed
                        )
                        Text(
                            text = if (isArabic)
                                "حسابات الموردين ليس لديهم صلاحية للوصول إلى قاعدة بيانات العملاء."
                            else
                                "Vendor accounts do not have permission to access the customer database.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        return
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var inactiveThresholdMonths by remember { mutableIntStateOf(3) } // 3, 6, 12 months

    // Dialog & Sheet States
    var showAddDialog by remember { mutableStateOf(false) }
    var clientToEdit by remember { mutableStateOf<Client?>(null) }
    var clientToDelete by remember { mutableStateOf<Client?>(null) }
    var selectedClientDetail by remember { mutableStateOf<Client?>(null) }
    var whatsappDialogClient by remember { mutableStateOf<Client?>(null) }

    // Pagination for All Customers (20 per page)
    var currentPage by remember { mutableIntStateOf(1) }
    val pageSize = 20

    val filteredAllClients by remember(allClients, searchQuery) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            val list = if (q.isEmpty()) allClients else {
                allClients.filter {
                    it.name.lowercase().contains(q) || it.phone.lowercase().contains(q)
                }
            }
            list.sortedBy { it.name.lowercase() }
        }
    }

    val paginatedClients by remember(filteredAllClients, currentPage) {
        derivedStateOf {
            filteredAllClients.take(currentPage * pageSize)
        }
    }

    // Inactive Clients Calculation
    val now = System.currentTimeMillis()
    val inactiveClientsAll by remember(allClients, inactiveThresholdMonths) {
        derivedStateOf {
            val days = when (inactiveThresholdMonths) {
                6 -> 180L
                12 -> 365L
                else -> 90L
            }
            val cutoff = now - (days * 24L * 60L * 60L * 1000L)

            allClients.filter { client ->
                // Must have at least 1 invoice and last purchase date is older than cutoff
                (client.lastPurchaseDate in 1L..cutoff) ||
                (client.lastPurchaseDate == 0L && client.invoicesCount > 0 && client.createdAt <= cutoff)
            }.sortedWith(
                // Longest inactive first (oldest lastPurchaseDate)
                compareBy { if (it.lastPurchaseDate > 0) it.lastPurchaseDate else it.createdAt }
            )
        }
    }

    // 3+ months inactive count for tab badge
    val threeMonthsCutoff = now - (90L * 24L * 60L * 60L * 1000L)
    val inactiveBadgeCount by remember(allClients) {
        derivedStateOf {
            allClients.count { client ->
                (client.lastPurchaseDate in 1L..threeMonthsCutoff) ||
                (client.lastPurchaseDate == 0L && client.invoicesCount > 0 && client.createdAt <= threeMonthsCutoff)
            }
        }
    }

    val canModify = currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.MANAGER

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isArabic) "إدارة العملاء" else "Customers Directory",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("customers_back_btn")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            floatingActionButton = {
                // Cashier, Manager, and Admin can add customers
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_customer_fab")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .testTag("customers_screen")
            ) {
                // Two Tabs: All Customers vs Inactive (3+ months)
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Text(
                                text = if (isArabic) "جميع العملاء (${allClients.size})" else "All Customers (${allClients.size})",
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_all_customers")
                    )

                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "غير نشط (+3 أشهر)" else "Inactive (3+ mo)",
                                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                                if (inactiveBadgeCount > 0) {
                                    Badge(
                                        containerColor = DangerRed,
                                        contentColor = Color.White
                                    ) {
                                        Text("$inactiveBadgeCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("tab_inactive_customers")
                    )
                }

                if (selectedTabIndex == 0) {
                    // TAB 1: ALL CUSTOMERS
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search Input
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                currentPage = 1
                            },
                            placeholder = { Text(if (isArabic) "بحث بالاسم أو الهاتف..." else "Search by customer name or phone...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("customer_search_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (paginatedClients.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                                    Text(
                                        text = if (isArabic) "لم يتم العثور على عملاء" else "No customers found",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(paginatedClients, key = { it.id }) { client ->
                                    CustomerItemCard(
                                        client = client,
                                        currency = currency,
                                        isArabic = isArabic,
                                        canModify = canModify,
                                        onClick = { selectedClientDetail = client },
                                        onEdit = { clientToEdit = client },
                                        onDelete = { clientToDelete = client },
                                        onCall = { launchPhoneDialer(context, client.phone) },
                                        onWhatsApp = { whatsappDialogClient = client }
                                    )
                                }

                                // Load More Button if more items exist
                                if (paginatedClients.size < filteredAllClients.size) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            OutlinedButton(
                                                onClick = { currentPage++ },
                                                modifier = Modifier.testTag("load_more_customers_btn")
                                            ) {
                                                Text(if (isArabic) "تحميل المزيد (20)" else "Load More (20)")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 2: INACTIVE (3+ MONTHS)
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filters Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "تصفية المدة:" else "Filter:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            listOf(3, 6, 12).forEach { months ->
                                FilterChip(
                                    selected = inactiveThresholdMonths == months,
                                    onClick = { inactiveThresholdMonths = months },
                                    label = { Text(if (isArabic) "+$months أشهر" else "+$months Months") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (months == 12) DangerRed else (if (months == 6) AmberWarning else PrimaryBlue),
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("inactive_filter_${months}m")
                                )
                            }
                        }

                        if (inactiveClientsAll.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(48.dp))
                                    Text(
                                        text = if (isArabic)
                                            "لا يوجد عملاء غير نشطين لأكثر من $inactiveThresholdMonths أشهر! ممتاز."
                                        else
                                            "No customers inactive for >$inactiveThresholdMonths months! Great job.",
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(inactiveClientsAll, key = { it.id }) { client ->
                                    val daysSince = remember(client.lastPurchaseDate, client.createdAt) {
                                        val ts = if (client.lastPurchaseDate > 0) client.lastPurchaseDate else client.createdAt
                                        TimeUnit.MILLISECONDS.toDays(now - ts).coerceAtLeast(0)
                                    }

                                    InactiveCustomerRow(
                                        client = client,
                                        daysSincePurchase = daysSince,
                                        currency = currency,
                                        isArabic = isArabic,
                                        onCall = { launchPhoneDialer(context, client.phone) },
                                        onWhatsApp = { whatsappDialogClient = client },
                                        onMarkContacted = {
                                            viewModel.markClientContacted(client.id) {
                                                Toast.makeText(context, if (isArabic) "تم التحديد كتم التواصل بنجاح" else "Marked as contacted!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onClick = { selectedClientDetail = client }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Customer Details Bottom Sheet (Purchase History)
        selectedClientDetail?.let { client ->
            CustomerDetailSheet(
                client = client,
                viewModel = viewModel,
                currency = currency,
                isArabic = isArabic,
                onDismiss = { selectedClientDetail = null },
                onCall = { launchPhoneDialer(context, client.phone) },
                onWhatsApp = { whatsappDialogClient = client }
            )
        }

        // Add Customer Dialog
        if (showAddDialog) {
            AddEditCustomerDialog(
                client = null,
                isArabic = isArabic,
                onDismiss = { showAddDialog = false },
                onSave = { name, phone, email, address ->
                    viewModel.addClient(name, phone, email, address) {
                        Toast.makeText(context, if (isArabic) "تمت إضافة العميل بنجاح" else "Customer added successfully", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    }
                }
            )
        }

        // Edit Customer Dialog
        clientToEdit?.let { client ->
            AddEditCustomerDialog(
                client = client,
                isArabic = isArabic,
                onDismiss = { clientToEdit = null },
                onSave = { name, phone, email, address ->
                    viewModel.updateClient(
                        client.copy(
                            name = name,
                            phone = phone,
                            email = email,
                            address = address
                        )
                    ) {
                        Toast.makeText(context, if (isArabic) "تم تعديل بيانات العميل" else "Customer updated successfully", Toast.LENGTH_SHORT).show()
                        clientToEdit = null
                    }
                }
            )
        }

        // Delete Confirmation Dialog (Soft Delete)
        clientToDelete?.let { client ->
            AlertDialog(
                onDismissRequest = { clientToDelete = null },
                title = { Text(if (isArabic) "حذف العميل" else "Delete Customer") },
                text = {
                    Text(
                        if (isArabic)
                            "هل أنت متأكد من حذف العميل '${client.name}'؟ سيتم أرشفة السجل ولن يظهر في القائمة."
                        else
                            "Are you sure you want to soft delete '${client.name}'? Existing invoice references will remain safe."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.softDeleteClient(client.id) {
                                Toast.makeText(context, if (isArabic) "تم حذف العميل" else "Customer soft deleted", Toast.LENGTH_SHORT).show()
                                clientToDelete = null
                            }
                        }
                    ) {
                        Text(if (isArabic) "حذف" else "Delete", color = DangerRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { clientToDelete = null }) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                }
            )
        }

        // WhatsApp Custom Message Dialog
        whatsappDialogClient?.let { client ->
            WhatsAppMessageDialog(
                client = client,
                isArabic = isArabic,
                onDismiss = { whatsappDialogClient = null },
                onSend = { message ->
                    launchWhatsApp(context, client.phone, message)
                    whatsappDialogClient = null
                }
            )
        }
    }
}

@Composable
fun CustomerItemCard(
    client: Client,
    currency: String,
    isArabic: Boolean,
    canModify: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("customer_item_${client.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryBlue.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                        }
                    }

                    Column {
                        Text(
                            text = client.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = client.phone,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Call and WhatsApp buttons on every customer
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("customer_call_btn_${client.id}")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onWhatsApp,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("customer_whatsapp_btn_${client.id}")
                    ) {
                        Icon(Icons.Default.Message, contentDescription = "WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                    }

                    if (canModify) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${if (isArabic) "إجمالي المشتريات:" else "Total spent:"} $currency%.2f".format(client.totalPurchasesAmount),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue
                )

                Text(
                    text = "${if (isArabic) "الفواتير:" else "Orders:"} ${client.invoicesCount}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val dateStr = if (client.lastPurchaseDate > 0) {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(client.lastPurchaseDate))
                } else {
                    if (isArabic) "لا يوجد" else "None"
                }

                Text(
                    text = "${if (isArabic) "آخر شراء:" else "Last order:"} $dateStr",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InactiveCustomerRow(
    client: Client,
    daysSincePurchase: Long,
    currency: String,
    isArabic: Boolean,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onMarkContacted: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("inactive_customer_${client.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = client.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (daysSincePurchase >= 365) DangerRed.copy(alpha = 0.15f)
                            else if (daysSincePurchase >= 180) AmberWarning.copy(alpha = 0.15f)
                            else PrimaryBlue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isArabic) "منذ $daysSincePurchase يوم" else "$daysSincePurchase days ago",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (daysSincePurchase >= 365) DangerRed else if (daysSincePurchase >= 180) AmberWarning else PrimaryBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(text = client.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Total Spent Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currency%.2f".format(client.totalPurchasesAmount),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = PrimaryBlue
                    )
                    Text(
                        text = if (isArabic) "إجمالي المشتريات" else "Total spent",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val lastOrderDate = if (client.lastPurchaseDate > 0) {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(client.lastPurchaseDate))
                } else {
                    if (isArabic) "لا توجد فواتير" else "No orders"
                }

                Text(
                    text = "${if (isArabic) "آخر فاتورة:" else "Last order:"} $lastOrderDate",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Actions: Call, WhatsApp, Mark as Contacted
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.size(32.dp).testTag("inactive_call_btn_${client.id}")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onWhatsApp,
                        modifier = Modifier.size(32.dp).testTag("inactive_whatsapp_btn_${client.id}")
                    ) {
                        Icon(Icons.Default.Message, contentDescription = "WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                    }

                    OutlinedButton(
                        onClick = onMarkContacted,
                        modifier = Modifier.height(32.dp).testTag("mark_contacted_btn_${client.id}"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabic) "تم التواصل" else "Contacted", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailSheet(
    client: Client,
    viewModel: InventoryViewModel,
    currency: String,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val invoicesFlow: Flow<List<Invoice>> = remember(client.name, client.phone) {
        viewModel.getClientInvoices(client.name, client.phone)
    }
    val invoices: List<Invoice> by invoicesFlow.collectAsState(initial = emptyList())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Header info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = client.name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text(text = client.phone, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (client.email.isNotEmpty()) {
                        Text(text = client.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledIconButton(onClick = onCall, containerColor = EmeraldGreen.copy(alpha = 0.15f)) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = EmeraldGreen)
                    }
                    FilledIconButton(onClick = onWhatsApp, containerColor = Color(0xFF25D366).copy(alpha = 0.15f)) {
                        Icon(Icons.Default.Message, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Summary Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(if (isArabic) "إجمالي المشتريات" else "Total Spent", fontSize = 11.sp, color = PrimaryBlue)
                        Text("$currency%.2f".format(client.totalPurchasesAmount), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(if (isArabic) "الفواتير" else "Invoices", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${client.invoicesCount}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(if (isArabic) "آخر عملية" else "Last Order", fontSize = 11.sp, color = EmeraldGreen)
                        val dt = if (client.lastPurchaseDate > 0) SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(client.lastPurchaseDate)) else "-"
                        Text(dt, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isArabic) "سجل فواتير المبيعات" else "Purchase History (Sales Invoices)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (invoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isArabic) "لا توجد فواتير مبيعات مسجلة لهذا العميل حتى الآن." else "No sales invoices recorded for this customer yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(invoices) { inv ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    val dateFmt = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(inv.createdAt))
                                    Text(text = dateFmt, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$currency%.2f".format(inv.totalAmount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PrimaryBlue
                                    )
                                    Text(
                                        text = inv.status.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (inv.status.name == "PAID") EmeraldGreen else AmberWarning
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun FilledIconButton(onClick: () -> Unit, containerColor: Color, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun AddEditCustomerDialog(
    client: Client?,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, email: String, address: String) -> Unit
) {
    var name by remember { mutableStateOf(client?.name ?: "") }
    var phone by remember { mutableStateOf(client?.phone ?: "") }
    var email by remember { mutableStateOf(client?.email ?: "") }
    var address by remember { mutableStateOf(client?.address ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (client == null) (if (isArabic) "إضافة عميل جديد" else "Add New Customer")
                else (if (isArabic) "تعديل بيانات العميل" else "Edit Customer")
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isArabic) "اسم العميل *" else "Customer Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_input_name")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (isArabic) "رقم الهاتف *" else "Phone Number *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("customer_input_phone")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(if (isArabic) "البريد الإلكتروني (اختياري)" else "Email (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (isArabic) "العنوان (اختياري)" else "Address (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorText?.let {
                    Text(text = it, color = DangerRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.trim().length < 2) {
                        errorText = if (isArabic) "يرجى إدخال اسم صحيح (حرفين على الأقل)" else "Please enter a valid name (min 2 chars)"
                        return@TextButton
                    }
                    if (phone.trim().length < 6) {
                        errorText = if (isArabic) "يرجى إدخال رقم هاتف صحيح" else "Please enter a valid phone number"
                        return@TextButton
                    }
                    onSave(name.trim(), phone.trim(), email.trim(), address.trim())
                },
                modifier = Modifier.testTag("save_customer_dialog_btn")
            ) {
                Text(if (isArabic) "حفظ" else "Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}

@Composable
fun WhatsAppMessageDialog(
    client: Client,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSend: (message: String) -> Unit
) {
    val defaultMsg = if (isArabic)
        "مرحباً ${client.name}، نأمل أن تكون بخير! يسرنا التواصل معك من StockMaster لإعلامك بأحدث المنتجات والعروض الخاصة."
    else
        "Hello ${client.name}, we hope you are well! We noticed it's been a while since your last order. Check out our latest arrivals and exclusive offers at StockMaster."

    var messageText by remember { mutableStateOf(defaultMsg) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Message, contentDescription = null, tint = Color(0xFF25D366))
                Text(if (isArabic) "إرسال رسالة واتساب" else "Send WhatsApp Message")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${if (isArabic) "العميل:" else "Customer:"} ${client.name} (${client.phone})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text(if (isArabic) "نص الرسالة (قابل للتعديل)" else "Message Template (Editable)") },
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("whatsapp_message_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSend(messageText.trim()) },
                modifier = Modifier.testTag("send_whatsapp_dialog_btn")
            ) {
                Text(if (isArabic) "إرسال عبر واتساب" else "Open in WhatsApp", fontWeight = FontWeight.Bold, color = Color(0xFF25D366))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        }
    )
}

private fun launchPhoneDialer(context: Context, phone: String) {
    try {
        val cleanPhone = phone.replace(" ", "").replace("-", "")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun launchWhatsApp(context: Context, phone: String, message: String) {
    try {
        var cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
        if (cleanPhone.startsWith("0")) {
            cleanPhone = "20" + cleanPhone.substring(1) // Default to Egypt code if leading 0
        }
        val encodedMessage = Uri.encode(message)
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
