package com.sudar.tnpscapp.nativeapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sudar.tnpscapp.nativeapp.ui.theme.AppColors

/**
 * Renders HTML content with support for images and text
 * Handles <img> tags and cleans HTML entities
 */
@Composable
fun RichHtmlContent(
    htmlString: String,
    modifier: Modifier = Modifier,
    textColor: Color = AppColors.TextPrimary,
    fontSize: Int = 16,
    fontWeight: FontWeight = FontWeight.Normal,
    lineHeight: Int = 24,
) {
    val context = LocalContext.current
    
    // Parse HTML to extract images and text
    val contentParts = remember(htmlString) { parseHtmlContent(htmlString) }
    
    Column(modifier = modifier) {
        contentParts.forEach { part ->
            when (part) {
                is HtmlPart.Image -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(part.url)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
                is HtmlPart.Text -> {
                    if (part.content.isNotBlank()) {
                        // Check if content has LaTeX
                        if (containsLatex(part.content)) {
                            Text(
                                text = convertLatexToPlainText(part.content),
                                color = textColor,
                                fontSize = fontSize.sp,
                                fontWeight = fontWeight,
                                lineHeight = lineHeight.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = part.content,
                                color = textColor,
                                fontSize = fontSize.sp,
                                fontWeight = fontWeight,
                                lineHeight = lineHeight.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class HtmlPart {
    data class Image(val url: String) : HtmlPart()
    data class Text(val content: String) : HtmlPart()
}

/**
 * Parse HTML content to extract images and text parts
 */
fun parseHtmlContent(html: String): List<HtmlPart> {
    val parts = mutableListOf<HtmlPart>()
    var remaining = html
    
    // Regex to find <img> tags
    val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
    
    while (remaining.isNotEmpty()) {
        val match = imgRegex.find(remaining)
        
        if (match != null) {
            // Add text before the image
            val textBefore = remaining.substring(0, match.range.first)
            val cleanedText = cleanHtmlText(textBefore)
            if (cleanedText.isNotBlank()) {
                parts.add(HtmlPart.Text(cleanedText))
            }
            
            // Add the image
            val imageUrl = match.groupValues[1]
            if (imageUrl.isNotBlank()) {
                parts.add(HtmlPart.Image(imageUrl))
            }
            
            // Continue with remaining content
            remaining = remaining.substring(match.range.last + 1)
        } else {
            // No more images, add remaining text
            val cleanedText = cleanHtmlText(remaining)
            if (cleanedText.isNotBlank()) {
                parts.add(HtmlPart.Text(cleanedText))
            }
            break
        }
    }
    
    return parts
}

/**
 * Check if text contains LaTeX patterns
 */
fun containsLatex(text: String): Boolean {
    return text.contains("\\(") || 
           text.contains("\\)") || 
           text.contains("\\dfrac") || 
           text.contains("\\frac") ||
           text.contains("\\sqrt") || 
           text.contains("\\times") || 
           text.contains("\\div") ||
           text.contains("\\pm") ||
           text.contains("\\degree") ||
           text.contains("^{") ||
           text.contains("_{")
}

/**
 * Check if text contains HTML image tags
 */
fun containsHtmlImage(text: String): Boolean {
    return text.contains("<img", ignoreCase = true)
}

/**
 * Clean HTML text - remove tags and decode entities
 * Fixed to properly handle <br>, <br/>, <br /> tags
 */
fun cleanHtmlText(html: String): String {
    return html
        // Remove <br> tags in all variations FIRST
        .replace(Regex("""<br\s*/?\s*>""", RegexOption.IGNORE_CASE), " ")
        // Remove all other HTML tags
        .replace(Regex("""<[^>]*>"""), "")
        // Decode HTML entities
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        // Clean up multiple spaces
        .replace(Regex("""\s+"""), " ")
        .trim()
}

/**
 * Convert LaTeX to plain readable text
 * Handles fractions, superscripts, subscripts, and math symbols
 */
fun convertLatexToPlainText(latex: String): String {
    var result = latex
    
    // Remove \( and \) delimiters
    result = result.replace("\\(", "").replace("\\)", "")
    
    // Handle \dfrac{num}{den} and \frac{num}{den} -> num/den
    result = Regex("""\\d?frac\{([^}]+)\}\{([^}]+)\}""").replace(result) { match ->
        "${match.groupValues[1]}/${match.groupValues[2]}"
    }
    
    // Handle \sqrt{x} -> √x
    result = Regex("""\\sqrt\{([^}]+)\}""").replace(result) { match ->
        "√${match.groupValues[1]}"
    }
    
    // Handle superscripts: x^{2} -> x²
    result = Regex("""(\w)\^\{([^}]+)\}""").replace(result) { match ->
        "${match.groupValues[1]}${toSuperscript(match.groupValues[2])}"
    }
    
    // Handle simple superscripts: x^2 -> x²
    result = Regex("""(\w)\^(\d+)""").replace(result) { match ->
        "${match.groupValues[1]}${toSuperscript(match.groupValues[2])}"
    }
    
    // Handle subscripts: x_{2} -> x₂
    result = Regex("""(\w)_\{([^}]+)\}""").replace(result) { match ->
        "${match.groupValues[1]}${toSubscript(match.groupValues[2])}"
    }
    
    // Handle simple subscripts: x_2 -> x₂
    result = Regex("""(\w)_(\d+)""").replace(result) { match ->
        "${match.groupValues[1]}${toSubscript(match.groupValues[2])}"
    }
    
    // Math symbols
    result = result
        .replace("\\times", "×")
        .replace("\\div", "÷")
        .replace("\\pm", "±")
        .replace("\\degree", "°")
        .replace("\\%", "%")
        .replace("\\$", "$")
        .replace("\\left(", "(")
        .replace("\\right)", ")")
        .replace("\\left[", "[")
        .replace("\\right]", "]")
        .replace("\\leq", "≤")
        .replace("\\geq", "≥")
        .replace("\\neq", "≠")
        .replace("\\approx", "≈")
        .replace("\\infty", "∞")
        .replace("\\pi", "π")
        .replace("\\alpha", "α")
        .replace("\\beta", "β")
        .replace("\\gamma", "γ")
        .replace("\\theta", "θ")
    
    // Handle \text{...} -> ...
    result = Regex("""\\text\{([^}]+)\}""").replace(result) { match ->
        match.groupValues[1]
    }
    
    // Remove remaining backslashes before words (like \quad, \space)
    result = Regex("""\\[a-zA-Z]+""").replace(result, " ")
    
    // Remove remaining braces
    result = result.replace("{", "").replace("}", "")
    
    // Clean up multiple spaces
    result = result.replace(Regex("""\s+"""), " ").trim()
    
    return result
}

private fun toSuperscript(input: String): String {
    return input.map { char ->
        when (char) {
            '0' -> '⁰'
            '1' -> '¹'
            '2' -> '²'
            '3' -> '³'
            '4' -> '⁴'
            '5' -> '⁵'
            '6' -> '⁶'
            '7' -> '⁷'
            '8' -> '⁸'
            '9' -> '⁹'
            '+' -> '⁺'
            '-' -> '⁻'
            'n' -> 'ⁿ'
            else -> char
        }
    }.joinToString("")
}

private fun toSubscript(input: String): String {
    return input.map { char ->
        when (char) {
            '0' -> '₀'
            '1' -> '₁'
            '2' -> '₂'
            '3' -> '₃'
            '4' -> '₄'
            '5' -> '₅'
            '6' -> '₆'
            '7' -> '₇'
            '8' -> '₈'
            '9' -> '₉'
            '+' -> '₊'
            '-' -> '₋'
            else -> char
        }
    }.joinToString("")
}
