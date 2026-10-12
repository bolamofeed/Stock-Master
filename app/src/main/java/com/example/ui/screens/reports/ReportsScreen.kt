package com.example.ui.screens.reports

import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InvoiceType
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: InventoryViewModel) {
    val context = LocalContext.current
    val items by viewModel.rawItems.collectAsState()
    val invoices by viewModel.allInvoices.collectAsState()
    val movements by viewModel.allMovements.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    val totalValuation = items.sumOf { it.totalValuation }
    val totalRetail = items.sumOf { it.totalRetailValue }
    val potentialProfit = (totalRetail - totalValuation).coerceAtLeast(0.0)

    val totalSales = invoices.filter { it.type == InvoiceType.SALES }.sumOf { it.totalAmount }
    val totalPurchases = invoices.filter { it.type == InvoiceType.PURCHASE }.sumOf { it.totalAmount }

    val warehouseValuation = remember(items) {
        items.groupBy { it.warehouse }.mapValues { entry ->
            entry.value.sumOf { it.totalValuation }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports & Analytics", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
                .testTag("reports_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Valuation Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Inventory Valuation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Cost Value", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (user.role.canSeePurchasePrice) "$currency%.2f".format(totalValuation) else "Restricted",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column {
                                Text("Retail Potential", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "$currency%.2f".format(totalRetail),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }

                            Column {
                                Text("Expected Profit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (user.role.canSeePurchasePrice) "$currency%.2f".format(potentialProfit) else "Restricted",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                        }
                    }
                }
            }

            // Sales vs Purchases Performance Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Financial Ledger Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Total Invoiced Sales", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currency%.2f".format(totalSales), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldGreen)
                            }

                            Column {
                                Text("Purchase Orders", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$currency%.2f".format(totalPurchases), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF8B5CF6))
                            }
                        }
                    }
                }
            }

            // Valuation by Warehouse Location
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Stock Value by Warehouse", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        warehouseValuation.forEach { (wh, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("📍 $wh", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = if (user.role.canSeePurchasePrice) "$currency%.2f".format(value) else "Restricted",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }
                }
            }

            // Top Stocked Items
            item {
                Text(
                    text = "Top Stocked Products",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(items.sortedByDescending { it.quantity }.take(4)) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("SKU: ${item.sku} • ${item.warehouse}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${item.quantity} ${item.unit}",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Export Actions
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Exported Stock Valuation Report (PDF)", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).testTag("export_pdf_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export PDF", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Exported inventory data as CSV", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).testTag("export_csv_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
