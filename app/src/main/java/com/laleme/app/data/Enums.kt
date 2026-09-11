package com.laleme.app.data

/**
 * 屎的形状（参考 Bristol 大便分类法的简化版，0-4 连续刻度）
 *
 * 数据层刻意不依赖 Compose：颜色以 ARGB 的 Long 表示，
 * UI 层通过 [com.laleme.app.ui.theme.tint] 扩展属性取到 Color。
 */
enum class PoopShape(
    val value: Int,
    val emoji: String,
    val label: String,
    val desc: String,
    /** 该形状对应的 ARGB 颜色 */
    val argb: Long
) {
    VERY_HARD(
        0, "\uD83E\uDEA8", "羊粪球", "一颗颗硬球，很难排出", 0xFF8D6E63
    ),
    HARD(
        1, "\uD83C\uDF30", "干硬块", "结块状、表面凹凸，偏干", 0xFFA1887F
    ),
    IDEAL(
        2, "\uD83D\uDCA9", "黄金便", "香肠状表面有裂纹，最理想", 0xFFB07B3E
    ),
    SOFT(
        3, "\uD83C\uDF66", "软香蕉", "柔软成形、边缘光滑", 0xFFC79A4B
    ),
    LIQUID(
        4, "\uD83D\uDCA7", "水样便", "糊状或完全水样，无法成形", 0xFFD9B45A
    );

    companion object {
        fun of(value: Int): PoopShape = entries.firstOrNull { it.value == value } ?: IDEAL
    }
}

/**
 * 屎的颜色（0-5）
 */
enum class PoopColor(
    val value: Int,
    val emoji: String,
    val label: String,
    val desc: String,
    val argb: Long
) {
    DARK_BROWN(
        0, "\uD83D\uDFEB", "深褐", "颜色很深，接近黑褐", 0xFF3E2723
    ),
    BROWN(
        1, "\uD83D\uDFE4", "标准棕", "教科书级健康色", 0xFF6D4C41
    ),
    LIGHT_BROWN(
        2, "\uD83D\uDFE8", "浅棕黄", "偏浅的土黄色", 0xFFA9752F
    ),
    YELLOW(
        3, "\uD83D\uDFE1", "油黄", "偏黄、可能偏油", 0xFFE0A82E
    ),
    GREEN(
        4, "\uD83D\uDFE9", "绿色", "可能与蔬菜或肠胃蠕动过快有关", 0xFF6B8E23
    ),
    PALE(
        5, "\u26AA", "灰白", "灰白或陶土色，需留意", 0xFFD7D3C4
    );

    companion object {
        fun of(value: Int): PoopColor = entries.firstOrNull { it.value == value } ?: BROWN
    }
}

/**
 * 擦屁股时纸上的残留情况（0-4）
 */
enum class PoopResidue(
    val value: Int,
    val emoji: String,
    val label: String,
    val desc: String,
    val argb: Long
) {
    CLEAN(
        0, "\u2728", "一擦就净", "一张纸搞定，干净利落", 0xFF9CCC65
    ),
    LIGHT(
        1, "\uD83E\uDDFB", "轻微", "擦一两下就干净了", 0xFFAED581
    ),
    MEDIUM(
        2, "\uD83E\uDDFB", "中等", "需要三四张纸", 0xFFFFD54F
    ),
    HEAVY(
        3, "\uD83D\uDCA6", "较多", "擦很多次仍有残留", 0xFFFFB74D
    ),
    ENDLESS(
        4, "\uD83D\uDE30", "擦不完", "怎么擦都不干净，粘腻", 0xFFFF8A65
    );

    companion object {
        fun of(value: Int): PoopResidue = entries.firstOrNull { it.value == value } ?: LIGHT
    }
}
