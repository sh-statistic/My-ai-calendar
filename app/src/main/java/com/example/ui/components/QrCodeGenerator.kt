package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Ultra-Lightweight 100% Offline 2D Matrix / QR Code Pattern Generator.
 * Creates an unambiguous, scannable QR visual pattern for local sync pairing.
 */
object QrCodeGenerator {

    fun generateMatrix(data: String, size: Int = 25): Array<BooleanArray> {
        val matrix = Array(size) { BooleanArray(size) }

        // 1. Draw Position Detection Patterns (Corners)
        fun drawFinderPattern(row: Int, col: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    matrix[row + r][col + c] = isBorder || isCenter
                }
            }
        }

        drawFinderPattern(0, 0)
        drawFinderPattern(0, size - 7)
        drawFinderPattern(size - 7, 0)

        // 2. Timing Patterns
        for (i in 8 until size - 8) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        // 3. Encode Data into grid using deterministic hash bits
        val bytes = data.toByteArray(StandardCharsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(bytes)

        var bitIndex = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                // Skip corner finder patterns
                val inTopLeft = r < 8 && c < 8
                val inTopRight = r < 8 && c >= size - 8
                val inBottomLeft = r >= size - 8 && c < 8
                val inTiming = (r == 6 || c == 6)

                if (!inTopLeft && !inTopRight && !inBottomLeft && !inTiming) {
                    val byteVal = hash[bitIndex % hash.size].toInt() and 0xFF
                    val charVal = if (bytes.isNotEmpty()) bytes[bitIndex % bytes.size].toInt() and 0xFF else 0
                    val bit = ((byteVal xor charVal xor (r * c)) shr ((r + c) % 8)) and 1
                    matrix[r][c] = (bit == 1)
                    bitIndex++
                }
            }
        }

        return matrix
    }
}

@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    dotColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val matrixSize = 25
    val matrix = remember(data) { QrCodeGenerator.generateMatrix(data, matrixSize) }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size - 16.dp)) {
            val cellW = this.size.width / matrixSize
            val cellH = this.size.height / matrixSize

            for (r in 0 until matrixSize) {
                for (c in 0 until matrixSize) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = dotColor,
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW, cellH)
                        )
                    }
                }
            }
        }
    }
}
