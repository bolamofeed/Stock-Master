package com.example.ui.screens.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Item
import com.example.data.model.MovementType
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.PrimaryBlue

@Composable
fun StockAdjustDialog(
    item: Item,
    onDismiss: () -> Unit,
    onConfirm: (MovementType, Int, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(MovementType.STOCK_IN) }
    var quantityText by remember { mutableStateOf("1") }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val qty = quantityText.toIntOrNull() ?: 0
    val resultingQty = when (selectedType) {
        MovementType.STOCK_IN, MovementType.RETURN -> item.quantity + qty
        MovementType.STOCK_OUT -> item.quantity - qty
        MovementType.ADJUSTMENT -> qty
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Adjust Inventory",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${item.name} (${item.sku})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Current Stock Indicator
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Stock:", fontSize = 12.sp)
                        Text("${item.quantity} ${item.unit}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Type Chips
                Text("Movement Type:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedType == MovementType.STOCK_IN,
                        onClick = { selectedType = MovementType.STOCK_IN },
                        label = { Text("Stock In", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("adjust_type_stock_in")
                    )

                    FilterChip(
                        selected = selectedType == MovementType.STOCK_OUT,
                        onClick = { selectedType = MovementType.STOCK_OUT },
                        label = { Text("Stock Out", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DangerRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("adjust_type_stock_out")
                    )

                    FilterChip(
                        selected = selectedType == MovementType.ADJUSTMENT,
                        onClick = { selectedType = MovementType.ADJUSTMENT },
                        label = { Text("Set Count", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("adjust_type_adjustment")
                    )
                }

                // Quantity Input
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it.filter { char -> char.isDigit() }
                        errorMessage = null
                    },
                    label = { Text(if (selectedType == MovementType.ADJUSTMENT) "Exact count" else "Quantity to move") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("adjust_quantity_input")
                )

                // Note / Reason Input
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Reason / Reference note") },
                    placeholder = { Text("e.g. Supplier delivery, damage, recount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("adjust_note_input")
                )

                // Projected Result
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Resulting Stock:", fontSize = 12.sp)
                    Text(
                        text = "$resultingQty ${item.unit}",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (resultingQty < 0) DangerRed else EmeraldGreen,
                        fontSize = 14.sp
                    )
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = DangerRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (qty <= 0 && selectedType != MovementType.ADJUSTMENT) {
                        errorMessage = "Quantity must be greater than 0"
                        return@Button
                    }
                    if (resultingQty < 0) {
                        errorMessage = "Cannot reduce stock below 0!"
                        return@Button
                    }
                    onConfirm(selectedType, qty, noteText)
                },
                modifier = Modifier.testTag("confirm_adjust_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == MovementType.STOCK_OUT) DangerRed else EmeraldGreen
                )
            ) {
                Text("Confirm Update")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
