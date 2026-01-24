package com.sudar.tnpscapp.nativeapp.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

/**
 * Simple LaTeX math renderer using text conversion
 * No WebView - just converts LaTeX to readable Unicode text
 * 
 * Examples:
 * - \(2\dfrac{21}{60}\) → 2 21/60
 * - -\(5\dfrac{41}{60}\) → -5 41/60
 * - m^2 → m²
 * - \times → ×
 * - \div → ÷
 */
@Composable
fun LatexMathView(
    latex: String,
    modifier: Modifier = Modifier,
    textColor: Color = AppColors.TextPrimary,
    fontSize: Int = 15,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Text(
        text = convertLatexToPlainText(latex),
        modifier = modifier,
        color = textColor,
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
    )
}
