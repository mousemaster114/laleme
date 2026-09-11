package com.laleme.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- 黄色主题调色板 ----------

val Honey = Color(0xFFF2B01E)       // 主黄
val HoneyDeep = Color(0xFFD98E04)   // 深黄
val Amber = Color(0xFFFFC93C)       // 亮琥珀
val Custard = Color(0xFFFFE082)     // 奶油黄
val Cream = Color(0xFFFFF8E7)       // 背景奶油
val CreamDeep = Color(0xFFFFEFC7)   // 背景加深
val Cocoa = Color(0xFF4A3720)       // 主文字 深可可
val CocoaSoft = Color(0xFF7A6242)   // 次文字
val CocoaFaint = Color(0xFFA08A68)  // 弱文字
val GlassWhite = Color(0x99FFFFFF)  // 玻璃填充
val GlassStroke = Color(0x66FFFFFF) // 玻璃描边

private val LightColors = lightColorScheme(
    primary = HoneyDeep,
    onPrimary = Color.White,
    primaryContainer = Custard,
    onPrimaryContainer = Cocoa,
    secondary = Honey,
    onSecondary = Cocoa,
    secondaryContainer = Color(0xFFFFF0C2),
    onSecondaryContainer = Cocoa,
    tertiary = Color(0xFFB07B3E),
    onTertiary = Color.White,
    background = Cream,
    onBackground = Cocoa,
    surface = Color(0xFFFFFDF6),
    onSurface = Cocoa,
    surfaceVariant = Color(0xFFFFF3D6),
    onSurfaceVariant = CocoaSoft,
    outline = Color(0x33A08A68),
    error = Color(0xFFE5533D),
    onError = Color.White
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        letterSpacing = 0.4.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.3.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun LaLeMeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
