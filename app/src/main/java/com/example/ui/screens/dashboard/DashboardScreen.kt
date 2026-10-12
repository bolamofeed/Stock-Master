package com.example.ui.screens.dashboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Invoice
import com.example.data.model.InvoiceStatus
import com.example.data.model.InvoiceType
import com.example.data.model.MovementType
import com.example.data.model.StockMovement
import com.example.data.model.UserRole
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
fun DashboardScreen(
    viewModel: InventoryViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToAddItem: () -> Unit,
    onNavigateToCreateInvoice: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToItem: (String) -> Unit,
    onNavigateToInvoices: () -> Unit = {}
) {
    val user by viewModel.currentUser.collectAsState()
    val items by viewModel.rawItems.collectAsState()
    val lowStockItems by viewModel.lowStockItems.collectAsState()
    val invoices by viewModel.allInvoices.collectAsState()
    val movements by viewModel.allMovements.collectAsState()
    val unreadNotifs by viewModel.unreadNotificationsCount.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    val totalItemsCount = items.size
    val totalStockUnits = items.sumOf { it.quantity }
    val totalInventoryValue = items.sumOf { it.totalValuation }
    val lowStockCount = items.count { it.isLowStock || it.isOutOfStock }
    val totalSalesRevenue = invoices.filter { it.type.name == "SALES" }.sumOf { it.totalAmount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        item {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "StockMaster",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${user.name} • ${user.role.title}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.testTag("notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifs > 0) {
                                    Badge(containerColor = DangerRed) {
                                        Text("$unreadNotifs", color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }

        // Welcome Hero Banner
        item {
            HeroWelcomeCard(
                userRole = user.role,
                totalUnits = totalStockUnits,
                lowStockCount = lowStockCount,
                onScanClicked = onNavigateToScan
            )
        }

        // KPI Metric Cards Grid
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Catalog Items",
                        value = "$totalItemsCount items",
                        subtitle = "$totalStockUnits units total",
                        icon = Icons.Default.Inventory2,
                        accentColor = PrimaryBlue,
                        tag = "kpi_total_items"
                    )

                    MetricKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Stock Value",
                        value = if (user.role.canSeePurchasePrice) "$currency%.0f".format(totalInventoryValue) else "Restricted",
                        subtitle = "Cost valuation",
                        icon = Icons.Default.PointOfSale,
                        accentColor = EmeraldGreen,
                        tag = "kpi_stock_value"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Alerts",
                        value = "$lowStockCount Low / Out",
                        subtitle = "Needs reordering",
                        icon = Icons.Default.Warning,
                        accentColor = if (lowStockCount > 0) AmberWarning else EmeraldGreen,
                        tag = "kpi_low_stock"
                    )

                    MetricKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Sales Volume",
                        value = "$currency%.0f".format(totalSalesRevenue),
                        subtitle = "${invoices.size} Invoices",
                        icon = Icons.Default.Sync,
                        accentColor = Color(0xFF8B5CF6),
                        tag = "kpi_sales_volume"
                    )
                }
            }
        }

        // Quick Actions Row
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Quick Operations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToScan,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_scan"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    if (user.role.canEditInventory) {
                        OutlinedButton(
                            onClick = onNavigateToAddItem,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_add_item"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Item", fontSize = 13.sp)
                        }
                    }

                    if (user.role.canCreateInvoices) {
                        Button(
                            onClick = onNavigateToCreateInvoice,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_invoice"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Text("Invoice", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Sales & Purchases Performance Tracker
        item {
            SalesAndPurchasesTrackerSection(
                invoices = invoices,
                currency = currency,
                onNavigateToInvoices = onNavigateToInvoices,
                onCreateInvoice = onNavigateToCreateInvoice
            )
        }

        // Category Breakdown Chart
        item {
            CategoryDistributionSection(items = items)
        }

        // Recent Stock Movements
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Recent Stock Movements",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (movements.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "No stock movements recorded yet.",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(items = movements.take(5), key = { it.id }) { movement ->
                StockMovementRow(
                    movement = movement,
                    onClick = { onNavigateToItem(movement.itemId) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun HeroWelcomeCard(
    userRole: UserRole,
    totalUnits: Int,
    lowStockCount: Int,
    onScanClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("hero_welcome_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF0284C7))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WAREHOUSE OVERVIEW",
                        color = Color(0xFF93C5FD),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Surface(
                        color = Color(0x33FFFFFF),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = userRole.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$totalUnits Units Stored Across Warehouses",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (lowStockCount > 0)
                        "$lowStockCount items require immediate replenishment"
                    else "All inventory levels healthy and in-stock",
                    color = if (lowStockCount > 0) Color(0xFFFDE68A) else Color(0xFFA7F3D0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MetricKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    tag: String
) {
    Card(
        modifier = modifier.testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategoryDistributionSection(items: List<com.example.data.model.Item>) {
    val categoryCounts = remember(items) {
        items.groupBy { it.category }.mapValues { it.value.size }
    }

    if (categoryCounts.isEmpty()) return

    val colors = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("category_distribution_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Inventory by Category",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-colored bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
            ) {
                var currentX = 0f
                val total = items.size.toFloat().coerceAtLeast(1f)

                categoryCounts.entries.forEachIndexed { index, entry ->
                    val proportion = entry.value.toFloat() / total
                    val barWidth = size.width * proportion
                    val color = colors[index % colors.size]

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(currentX, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = CornerRadius(0f, 0f)
                    )
                    currentX += barWidth
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categoryCounts.entries.take(4).forEachIndexed { index, entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors[index % colors.size])
                        )
                        Text(
                            text = "${entry.key} (${entry.value})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StockMovementRow(movement: StockMovement, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val isPositive = movement.quantityChange > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
            .testTag("movement_row_${movement.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isPositive) EmeraldGreen.copy(alpha = 0.15f)
                            else DangerRed.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isPositive) EmeraldGreen else DangerRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = movement.itemName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${movement.type.title} • ${movement.note}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isPositive) "+" else ""}${movement.quantityChange} units",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = if (isPositive) EmeraldGreen else DangerRed
                )
                Text(
                    text = dateFormat.format(Date(movement.timestamp)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SalesAndPurchasesTrackerSection(
    invoices: List<Invoice>,
    currency: String,
    onNavigateToInvoices: () -> Unit = {},
    onCreateInvoice: () -> Unit = {}
) {
    var trackerTab by remember { mutableIntStateOf(0) } // 0 = Overview, 1 = Sales, 2 = Purchases

    val salesInvoices = remember(invoices) { invoices.filter { it.type == InvoiceType.SALES } }
    val purchaseInvoices = remember(invoices) { invoices.filter { it.type == InvoiceType.PURCHASE } }

    val totalSales = remember(salesInvoices) { salesInvoices.sumOf { it.totalAmount } }
    val paidSales = remember(salesInvoices) {
        salesInvoices.filter { it.status == InvoiceStatus.PAID }.sumOf { it.totalAmount }
    }
    val unpaidSales = remember(salesInvoices) {
        salesInvoices.filter { it.status == InvoiceStatus.UNPAID }.sumOf { it.totalAmount }
    }

    val totalPurchases = remember(purchaseInvoices) { purchaseInvoices.sumOf { it.totalAmount } }
    val paidPurchases = remember(purchaseInvoices) {
        purchaseInvoices.filter { it.status == InvoiceStatus.PAID }.sumOf { it.totalAmount }
    }
    val unpaidPurchases = remember(purchaseInvoices) {
        purchaseInvoices.filter { it.status == InvoiceStatus.UNPAID }.sumOf { it.totalAmount }
    }

    val netProfit = totalSales - totalPurchases
    val marginPercent = if (totalSales > 0) (netProfit / totalSales * 100.0) else 0.0

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("sales_purchases_tracker_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sales & Purchases Tracker",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${salesInvoices.size} Sales Orders • ${purchaseInvoices.size} Purchase Invoices",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Net Profit Badge
                Surface(
                    color = (if (netProfit >= 0) EmeraldGreen else DangerRed).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (netProfit >= 0) "+$currency%.0f (%.1f%% Margin)".format(netProfit, marginPercent)
                               else "-$currency%.0f Net Loss".format(-netProfit),
                        color = if (netProfit >= 0) EmeraldGreen else DangerRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Tabs: Overview, Sales, Purchases
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = trackerTab == 0,
                    onClick = { trackerTab = 0 },
                    label = { Text("Overview & Ratio", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = trackerTab == 1,
                    onClick = { trackerTab = 1 },
                    label = { Text("Sales (${salesInvoices.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldGreen,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = trackerTab == 2,
                    onClick = { trackerTab = 2 },
                    label = { Text("Purchases (${purchaseInvoices.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF8B5CF6),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            when (trackerTab) {
                0 -> {
                    // Overview Tab
                    // Dual Sales & Purchases Comparison Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Sales Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldGreen))
                                    Text("Total Sales", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currency%.2f".format(totalSales),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Paid: $currency%.0f • Due: $currency%.0f".format(paidSales, unpaidSales),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Purchases Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF8B5CF6).copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF8B5CF6)))
                                    Text("Purchases", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currency%.2f".format(totalPurchases),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Paid: $currency%.0f • Due: $currency%.0f".format(paidPurchases, unpaidPurchases),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Visual Comparison Progress Bars
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val maxVal = (totalSales + totalPurchases).coerceAtLeast(1.0)
                        val salesRatio = (totalSales / maxVal).toFloat().coerceIn(0.05f, 0.95f)
                        val purchasesRatio = (totalPurchases / maxVal).toFloat().coerceIn(0.05f, 0.95f)

                        Text("Revenue vs Expenditure Ratio", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            val salesWidth = size.width * salesRatio
                            val purchasesWidth = size.width * (1f - salesRatio)

                            drawRoundRect(
                                color = EmeraldGreen,
                                topLeft = Offset(0f, 0f),
                                size = Size(salesWidth, size.height),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawRoundRect(
                                color = Color(0xFF8B5CF6),
                                topLeft = Offset(salesWidth, 0f),
                                size = Size(purchasesWidth, size.height),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldGreen))
                                Text("Sales: %.1f%%".format(salesRatio * 100), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF8B5CF6)))
                                Text("Purchases: %.1f%%".format(purchasesRatio * 100), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Unpaid Invoices Alert Banner if unpaid invoices exist
                    val allUnpaid = invoices.filter { it.status == InvoiceStatus.UNPAID }
                    if (allUnpaid.isNotEmpty()) {
                        Surface(
                            color = AmberWarning.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onNavigateToInvoices)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "${allUnpaid.size} Unpaid Invoices ($currency%.2f)".format(allUnpaid.sumOf { it.totalAmount }),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text("Pay / View >", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                        }
                    }
                }

                1 -> {
                    // Recent Sales Tab
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent Sales Orders", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            OutlinedButton(
                                onClick = onCreateInvoice,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("+ New Sale", fontSize = 11.sp)
                            }
                        }

                        if (salesInvoices.isEmpty()) {
                            Text("No sales invoices created yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            salesInvoices.take(4).forEach { inv ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onNavigateToInvoices)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("#${inv.invoiceNumber} • ${inv.entityName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(dateFormat.format(Date(inv.createdAt)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("$currency%.2f".format(inv.totalAmount), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = EmeraldGreen)
                                            Text(inv.status.title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (inv.status == InvoiceStatus.PAID) EmeraldGreen else AmberWarning)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Recent Purchases Tab
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent Purchase Orders", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            OutlinedButton(
                                onClick = onCreateInvoice,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("+ New Purchase", fontSize = 11.sp)
                            }
                        }

                        if (purchaseInvoices.isEmpty()) {
                            Text("No purchase orders created yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            purchaseInvoices.take(4).forEach { inv ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onNavigateToInvoices)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("#${inv.invoiceNumber} • ${inv.entityName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(dateFormat.format(Date(inv.createdAt)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("$currency%.2f".format(inv.totalAmount), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF8B5CF6))
                                            Text(inv.status.title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (inv.status == InvoiceStatus.PAID) EmeraldGreen else AmberWarning)
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


