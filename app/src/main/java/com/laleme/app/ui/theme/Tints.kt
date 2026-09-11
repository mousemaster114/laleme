package com.laleme.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.laleme.app.data.HealthGrade
import com.laleme.app.data.PoopColor
import com.laleme.app.data.PoopResidue
import com.laleme.app.data.PoopShape

/**
 * 把数据层的 ARGB Long 映射成 Compose 的 Color。
 * 这样数据层 / 单元测试不需要依赖 Compose。
 */
val PoopShape.tint: Color get() = Color(argb)
val PoopColor.tint: Color get() = Color(argb)
val PoopResidue.tint: Color get() = Color(argb)
val HealthGrade.color: Color get() = Color(argb)
