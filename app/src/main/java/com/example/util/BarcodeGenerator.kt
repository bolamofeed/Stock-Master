package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

object BarcodeGenerator {

    // Code 128 Table B encoding patterns (107 patterns, each 11 bits except stop code which is 13 bits)
    // 0 = space, 1 = bar
    private val CODE128_PATTERNS = arrayOf(
        "11011001100", "11001101100", "11001100110", "10010011000", "10010001100",
        "10001001100", "10011001000", "10011000100", "10001100100", "11001001000",
        "11001000100", "11000100100", "10110011100", "10011011100", "10011001110",
        "10111001100", "10011101100", "10011100110", "11001110010", "11001011100",
        "11001001110", "11011100100", "11001110100", "11101101110", "11101001100",
        "11100101100", "11100100110", "11101100100", "11100110100", "11100110010",
        "11011011000", "11011000110", "11000110110", "10100011000", "10001011000",
        "10001000110", "10110001000", "10001101000", "10001100010", "11010001000",
        "11000101000", "11000100010", "10110111000", "10110001110", "10001101110",
        "10111011000", "10111000110", "10001110110", "11101110110", "11010001110",
        "11000101110", "11011101000", "11011100010", "11011101110", "11101011000",
        "11101000110", "11100010110", "11101101000", "11101100010", "11100011010",
        "11101111010", "11001000010", "11110001010", "10100110000", "10100001100",
        "10010110000", "10010000110", "10000101100", "10000100110", "10110010000",
        "10110000100", "10011010000", "10011000010", "10000110100", "10000110010",
        "11000010010", "11001010000", "11110111010", "11000010100", "10001111010",
        "10100111100", "10010111100", "10010011110", "10111100100", "10011110100",
        "10011110010", "11110100100", "11110010100", "11110010010", "11011011110",
        "11011110110", "11110110110", "10101111000", "10100011110", "10001011110",
        "10111101000", "10111100010", "11110101000", "11110100010", "10111011110",
        "10111101110", "11101011110", "11110101110", "11010000100", "11010010000",
        "11010011100", "11000111010" // Stop code: "1100011101011"
    )

    private const val START_CODE_B = 104
    private const val STOP_CODE_PATTERN = "1100011101011"

    /**
     * Encodes a string into a binary string ("1" for black bar, "0" for space) using Code 128B.
     */
    fun encodeCode128(text: String): String {
        val sanitized = text.filter { it.code in 32..126 }.ifEmpty { "SM-1001" }
        val values = mutableListOf<Int>()
        values.add(START_CODE_B)

        for (ch in sanitized) {
            values.add(ch.code - 32)
        }

        // Calculate Checksum: (Start_code + SUM(value * index)) % 103
        var checksum = values[0]
        for (i in 1 until values.size) {
            checksum += values[i] * i
        }
        checksum %= 103
        values.add(checksum)

        val sb = StringBuilder()
        // Quiet zone (10 modules)
        sb.append("0000000000")

        for (valCode in values) {
            if (valCode in CODE128_PATTERNS.indices) {
                sb.append(CODE128_PATTERNS[valCode])
            }
        }
        sb.append(STOP_CODE_PATTERN)
        // Quiet zone
        sb.append("0000000000")

        return sb.toString()
    }

    /**
     * Generates a 2D QR-code like matrix (25x25) deterministically based on data hash
     * with standard 3 position detection patterns at top-left, top-right, bottom-left.
     */
    fun generateQrMatrix(data: String): Array<BooleanArray> {
        val size = 25
        val matrix = Array(size) { BooleanArray(size) { false } }

        fun placeFinder(top: Int, left: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    matrix[top + r][left + c] = isBorder || isCenter
                }
            }
        }

        // Place 3 finder patterns
        placeFinder(0, 0)
        placeFinder(0, size - 7)
        placeFinder(size - 7, 0)

        // Timing patterns
        for (i in 7 until size - 7) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        // Alignment pattern at bottom right (row 16..20, col 16..20)
        for (r in 16..20) {
            for (c in 16..20) {
                val isBorder = r == 16 || r == 20 || c == 16 || c == 20
                val isCenter = (r == 18 && c == 18)
                matrix[r][c] = isBorder || isCenter
            }
        }

        // Fill data cells using hash
        val bytes = data.toByteArray()
        var bitIndex = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                // Skip finders & separators
                if ((r < 8 && c < 8) || (r < 8 && c >= size - 8) || (r >= size - 8 && c < 8)) continue
                if (r == 6 || c == 6) continue
                if (r in 16..20 && c in 16..20) continue

                val byteVal = if (bytes.isNotEmpty()) bytes[bitIndex % bytes.size].toInt() else 42
                val shift = (bitIndex * 3 + r * 7 + c * 11) % 8
                val bit = ((byteVal shr shift) and 1) == 1
                val mask = (r + c) % 2 == 0
                matrix[r][c] = bit xor mask
                bitIndex++
            }
        }

        return matrix
    }

    /**
     * Generates a printable product label Bitmap containing:
     * - Item Name
     * - SKU and Price
     * - 1D Code128 Barcode
     * - 2D QR Code
     * - Warehouse location
     */
    fun createPrintableLabelBitmap(
        name: String,
        sku: String,
        barcode: String,
        price: Double,
        warehouse: String,
        currencySymbol: String = "$"
    ): Bitmap {
        val width = 600
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint().apply {
            color = Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(8f, 8f, (width - 8).toFloat(), (height - 8).toFloat(), borderPaint)

        val textPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        // Header: Brand & Title
        textPaint.textSize = 28f
        textPaint.isFakeBoldText = true
        canvas.drawText("STOCKMASTER LABEL", 24f, 44f, textPaint)

        // Item Name
        textPaint.textSize = 32f
        val displayName = if (name.length > 24) name.take(22) + ".." else name
        canvas.drawText(displayName, 24f, 88f, textPaint)

        // SKU & Price
        textPaint.textSize = 24f
        textPaint.isFakeBoldText = false
        canvas.drawText("SKU: $sku", 24f, 124f, textPaint)

        textPaint.isFakeBoldText = true
        textPaint.color = Color.rgb(16, 120, 70) // Green
        canvas.drawText("PRICE: $currencySymbol%.2f".format(price), 24f, 160f, textPaint)

        textPaint.color = Color.DKGRAY
        textPaint.textSize = 20f
        textPaint.isFakeBoldText = false
        canvas.drawText("Loc: $warehouse", 24f, 192f, textPaint)

        // Render QR Code on top right
        val qrMatrix = generateQrMatrix(barcode)
        val qrSize = 140
        val qrLeft = width - qrSize - 32
        val qrTop = 32
        val cellSize = qrSize.toFloat() / qrMatrix.size
        val blackPaint = Paint().apply { color = Color.BLACK }

        for (r in qrMatrix.indices) {
            for (c in qrMatrix[r].indices) {
                if (qrMatrix[r][c]) {
                    canvas.drawRect(
                        qrLeft + c * cellSize,
                        qrTop + r * cellSize,
                        qrLeft + (c + 1) * cellSize,
                        qrTop + (r + 1) * cellSize,
                        blackPaint
                    )
                }
            }
        }

        // Render Code 128 Barcode at bottom
        val bits = encodeCode128(barcode)
        val barLeft = 32f
        val barTop = 220f
        val barBottom = 330f
        val barWidth = (width - 64).toFloat() / bits.length

        for (i in bits.indices) {
            if (bits[i] == '1') {
                canvas.drawRect(
                    barLeft + i * barWidth,
                    barTop,
                    barLeft + (i + 1) * barWidth,
                    barBottom,
                    blackPaint
                )
            }
        }

        // Barcode text below bars
        textPaint.color = Color.BLACK
        textPaint.textSize = 22f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("* $barcode *", width / 2f, 370f, textPaint)

        return bitmap
    }

    /**
     * Generates a multi-label printable sheet Bitmap for N copies of a barcode label
     * Arranged in a grid (e.g. 2 columns x rows) with cut borders and label headers.
     */
    fun createMultiLabelSheetBitmap(
        name: String,
        sku: String,
        barcode: String,
        price: Double,
        warehouse: String,
        currencySymbol: String = "$",
        quantity: Int = 1
    ): Bitmap {
        val qty = quantity.coerceIn(1, 100)
        val singleLabel = createPrintableLabelBitmap(name, sku, barcode, price, warehouse, currencySymbol)

        val columns = if (qty == 1) 1 else 2
        val rows = (qty + columns - 1) / columns
        val padding = 20
        val labelW = singleLabel.width
        val labelH = singleLabel.height

        val sheetWidth = columns * labelW + (columns + 1) * padding
        val sheetHeight = rows * labelH + (rows + 1) * padding + 80 // extra for sheet header

        val sheetBitmap = Bitmap.createBitmap(sheetWidth, sheetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)
        canvas.drawColor(Color.WHITE)

        val headerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 28f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("STOCKMASTER BATCH PRINT SHEET • $qty Labels ($name - $sku)", padding.toFloat(), 50f, headerPaint)

        var labelCount = 0
        for (r in 0 until rows) {
            for (c in 0 until columns) {
                if (labelCount >= qty) break
                val x = padding + c * (labelW + padding)
                val y = 80 + padding + r * (labelH + padding)
                canvas.drawBitmap(singleLabel, x.toFloat(), y.toFloat(), null)
                labelCount++
            }
        }

        return sheetBitmap
    }
}
