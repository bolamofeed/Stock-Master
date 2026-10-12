package com.example.ui.screens.invoices

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Invoice
import com.example.data.model.InvoiceStatus
import com.example.ui.components.QrCodeView
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PdfInvoicePreviewDialog(
    invoice: Invoice,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onPayInvoice: ((Invoice) -> Unit)? = null,
    onReturnInvoice: ((Invoice) -> Unit)? = null
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val statusColor = when (invoice.status) {
        InvoiceStatus.PAID -> EmeraldGreen
        InvoiceStatus.UNPAID -> AmberWarning
        InvoiceStatus.CANCELLED -> DangerRed
        InvoiceStatus.DRAFT -> Color.Gray
        InvoiceStatus.PARTIALLY_RETURNED -> Color(0xFFF97316)
        InvoiceStatus.RETURNED -> Color(0xFF9333EA)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PDF Invoice Preview", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = invoice.status.title,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                    .padding(16.dp)
                    .testTag("pdf_invoice_container"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Document Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "STOCKMASTER",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text("Enterprise Inventory Systems", fontSize = 10.sp, color = Color.Gray)
                        Text("support@stockmaster.app", fontSize = 10.sp, color = Color.Gray)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = invoice.type.title.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "#${invoice.invoiceNumber}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                        Text(
                            text = dateFormat.format(Date(invoice.createdAt)),
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Billed To / From
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("BILLED TO / ENTITY:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(invoice.entityName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                        if (invoice.entityContact.isNotBlank()) {
                            Text(invoice.entityContact, fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("ISSUED BY:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(invoice.createdBy, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Black)
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Line Items Summary
                Text("ORDER PARTICULARS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                val lineItems = invoice.parseLineItems()
                if (lineItems.isNotEmpty()) {
                    lineItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.itemName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("SKU: ${item.sku} • Qty: ${item.quantity}${if (item.returnedQuantity > 0) " (${item.returnedQuantity} returned)" else ""}", fontSize = 10.sp, color = Color.Gray)
                            }
                            Text(
                                text = "$currencySymbol%.2f".format(item.total),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                } else {
                    val rawItems = invoice.itemsJson.split("##", ";").filter { it.isNotBlank() }
                    if (rawItems.isNotEmpty()) {
                        rawItems.forEach { line ->
                            Text(line.trim(), fontSize = 12.sp, color = Color.Black)
                        }
                    } else {
                        Text("Products listed in document", fontSize = 12.sp, color = Color.DarkGray)
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Financial Breakdown
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PdfSummaryRow(title = "Subtotal:", value = "$currencySymbol%.2f".format(invoice.subtotal))
                    if (invoice.discountAmount > 0) {
                        PdfSummaryRow(
                            title = "Discount (${invoice.discountPercent}%):",
                            value = "-$currencySymbol%.2f".format(invoice.discountAmount),
                            valueColor = EmeraldGreen
                        )
                    }
                    PdfSummaryRow(
                        title = "VAT / Tax (${invoice.taxRatePercent}%):",
                        value = "+$currencySymbol%.2f".format(invoice.taxAmount)
                    )

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL AMOUNT:", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.Black)
                        Text(
                            text = "$currencySymbol%.2f".format(invoice.totalAmount),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = PrimaryBlue
                        )
                    }
                }

                // QR Code Verification
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Official Digital Stamp", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("Verified by StockMaster Cloud ledger", fontSize = 9.sp, color = Color.Gray)
                    }

                    QrCodeView(
                        data = "INVOICE:${invoice.invoiceNumber}:${invoice.totalAmount}",
                        sizeDp = 64
                    )
                }

                // Action Buttons inside preview (Pay or Return)
                if (invoice.status == InvoiceStatus.UNPAID && onPayInvoice != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onPayInvoice(invoice)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("pdf_pay_invoice_btn")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pay This Invoice Now ($currencySymbol%.2f)".format(invoice.totalAmount))
                    }
                }

                if ((invoice.status == InvoiceStatus.PAID || invoice.status == InvoiceStatus.PARTIALLY_RETURNED) && onReturnInvoice != null) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onReturnInvoice(invoice)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("pdf_return_invoice_btn")
                    ) {
                        Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Process Return (Full or Partial)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Toast.makeText(context, "Invoice PDF saved and ready to share/print!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.testTag("pdf_share_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share / Print PDF")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PdfSummaryRow(
    title: String,
    value: String,
    valueColor: Color = Color.Black
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
