package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.BarcodeGenerator

@Composable
fun Code128BarcodeView(
    data: String,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val bits = remember(data) { BarcodeGenerator.encodeCode128(data) }

    Column(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(12.dp)
            .testTag("code128_barcode_container"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .testTag("code128_canvas")
        ) {
            val totalBars = bits.length
            val barWidth = size.width / totalBars.toFloat()

            for (i in bits.indices) {
                if (bits[i] == '1') {
                    drawRect(
                        color = barColor,
                        topLeft = Offset(i * barWidth, 0f),
                        size = Size(barWidth.coerceAtLeast(1f), size.height)
                    )
                }
            }
        }

        Text(
            text = data,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 140,
    darkColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val matrix = remember(data) { BarcodeGenerator.generateQrMatrix(data) }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(10.dp)
            .testTag("qr_code_container"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(sizeDp.dp)
                .testTag("qr_canvas")
        ) {
            val rows = matrix.size
            val cellSize = size.width / rows.toFloat()

            for (r in matrix.indices) {
                for (c in matrix[r].indices) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = darkColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }
    }
}
