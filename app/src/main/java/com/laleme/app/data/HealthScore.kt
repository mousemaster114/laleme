package com.laleme.app.data

import java.util.concurrent.TimeUnit

/**
 * 健康等级：主界面左侧的“屎的健康程度”指示条使用。
 *
 * 同样不依赖 Compose，颜色用 ARGB Long 表示。
 */
enum class HealthGrade(
    val minScore: Int,
    val emoji: String,
    val label: String,
    val argb: Long,
    val advice: String
) {
    EXCELLENT(
        85, "\uD83C\uDF1F", "优秀", 0xFF7CB342,
        "教科书级别！继续保持这样的饮食和作息。"
    ),
    GOOD(
        70, "\uD83D\uDE0A", "良好", 0xFF9CCC65,
        "整体不错，多喝水、多吃点膳食纤维会更棒。"
    ),
    FAIR(
        50, "\uD83D\uDE10", "一般", 0xFFF5A623,
        "有点小状况，注意规律饮食、别熬夜。"
    ),
    NOTICE(
        30, "\uD83D\uDE1F", "注意", 0xFFFF8A5B,
        "状态偏离健康区间，建议观察几天。"
    ),
    ALERT(
        0, "\uD83D\uDEA8", "警报", 0xFFE53935,
        "持续异常请及时咨询医生。"
    );

    companion object {
        fun of(score: Int): HealthGrade =
            entries.firstOrNull { score >= it.minScore } ?: ALERT
    }
}

/**
 * 单次拉屎的健康评分
 *
 * 权重：形状 40% + 颜色 50% + 残留 10%
 * 颜色权重最高，因为灰白 / 油黄 / 发黑在医学上是更需要关注的信号。
 *
 * 分数区间是真正的 **0 ~ 100**：
 * 三个维度各自的子分都可以取到 0，加权后最差组合正好是 0 分。
 */
object HealthScore {

    /**
     * 各维度取最差时的加权结果，作为 0 分的基准：
     * 形状最差 10 × 0.40 + 颜色最差 0 × 0.50 + 残留最差 0 × 0.10 = 4
     */
    private const val WORST_RAW = 4.0
    private const val BEST_RAW = 100.0

    fun score(entry: PoopEntry): Int =
        score(entry.shape, entry.color, entry.residue)

    /**
     * 加权后再按 [WORST_RAW] ~ [BEST_RAW] 线性映射到 0 ~ 100，
     * 这样「三维度全最差」正好是 0 分，「三维度全最好」正好是 100 分。
     */
    fun score(shape: Int, color: Int, residue: Int): Int {
        val raw = shapeScore(shape) * 0.40 +
            colorScore(color) * 0.50 +
            residueScore(residue) * 0.10
        val mapped = (raw - WORST_RAW) / (BEST_RAW - WORST_RAW) * 100.0
        return Math.round(mapped).toInt().coerceIn(0, 100)
    }

    fun grade(entry: PoopEntry): HealthGrade = HealthGrade.of(score(entry))

    /**
     * 最差 → 0，最好 → 100。
     * 以前最差只有 45 分，导致界面上的分数永远掉不到低区间，
     * 「警报」等级形同虚设；现在区间被完整拉开。
     */
    private fun shapeScore(v: Int) = when (v) {
        2 -> 100   // 黄金便
        3 -> 84    // 软香蕉
        1 -> 46    // 干硬块
        4 -> 30    // 水样
        0 -> 10    // 羊粪球：最差
        else -> 60
    }

    private fun colorScore(v: Int) = when (v) {
        1 -> 100   // 标准棕
        2 -> 85    // 浅棕黄
        0 -> 56    // 深褐
        3 -> 44    // 油黄
        4 -> 38    // 绿色
        5 -> 0     // 灰白 / 陶土色：最差
        else -> 65
    }

    private fun residueScore(v: Int) = when (v) {
        0 -> 100
        1 -> 86
        2 -> 62
        3 -> 34
        4 -> 0     // 擦不完：最差
        else -> 60
    }
}

/**
 * 一段时间的拉屎总结
 */
data class PoopSummary(
    val from: Long,
    val to: Long,
    val entries: List<PoopEntry>
) {
    val count: Int get() = entries.size

    /**
     * 区间覆盖的自然天数（按**日历天**算，不是按毫秒差除以 24 小时）。
     * 这样「今天 00:00 ~ 今天 23:59」= 1 天，跨越 7 个日期就是 7 天，
     * 不会因为毫秒差不足整天而少算一天。
     */
    val days: Long
        get() {
            if (to < from) return 0
            val startDay = floorToDay(from)
            val endDay = floorToDay(to)
            return (endDay - startDay) / DAY_MILLIS + 1
        }

    val activeDays: Int
        get() = entries.map { it.timestamp / DAY_MILLIS }.distinct().size

    val perDay: Double get() = if (days <= 0) 0.0 else count.toDouble() / days

    val perActiveDay: Double
        get() = if (activeDays <= 0) 0.0 else count.toDouble() / activeDays

    val avgScore: Int
        get() = if (entries.isEmpty()) 0
        else entries.sumOf { HealthScore.score(it) } / entries.size

    val avgGrade: HealthGrade get() = HealthGrade.of(avgScore)

    /** 形状分布：下标 0-4 对应形状，值为次数 */
    val shapeDist: List<Int> get() = dist { it.shape }

    /** 颜色分布：下标 0-5 对应颜色，值为次数 */
    val colorDist: List<Int> get() = dist { it.color }

    /** 残留分布：下标 0-4 对应残留，值为次数 */
    val residueDist: List<Int> get() = dist { it.residue }

    private fun dist(key: (PoopEntry) -> Int): List<Int> {
        val buckets = IntArray(6)
        entries.forEach { buckets[key(it).coerceIn(0, 5)]++ }
        return buckets.toList()
    }

    val dominantShape: PoopShape?
        get() = shapeDist.withIndex().maxByOrNull { it.value }
            ?.takeIf { it.value > 0 }?.let { PoopShape.of(it.index) }

    val dominantColor: PoopColor?
        get() = colorDist.withIndex().maxByOrNull { it.value }
            ?.takeIf { it.value > 0 }?.let { PoopColor.of(it.index) }

    val dominantResidue: PoopResidue?
        get() = residueDist.withIndex().maxByOrNull { it.value }
            ?.takeIf { it.value > 0 }?.let { PoopResidue.of(it.index) }

    /** 出现过的警报色（灰白 / 深褐发黑） */
    val warningColors: List<PoopColor>
        get() = entries.map { it.colorEnum }.distinct()
            .filter { it == PoopColor.PALE || it == PoopColor.DARK_BROWN }

    val messyCount: Int get() = entries.count { it.residue >= 3 }

    val healthyCount: Int get() = entries.count { it.shape == 2 || it.shape == 3 }

    /** 按天聚合，用于趋势图：当天 00:00 的时间戳 -> 次数 */
    val byDay: Map<Long, Int>
        get() = entries.groupingBy { it.timestamp / DAY_MILLIS * DAY_MILLIS }.eachCount()

    /** 最常拉屎的时间段（0-23） */
    val peakHour: Int?
        get() = entries.groupingBy { hourOf(it.timestamp) }.eachCount()
            .maxByOrNull { it.value }?.key

    /** 生成人话总结 */
    fun insights(): List<String> {
        if (entries.isEmpty()) return listOf("这段时间还没有记录，先去右上角加一条吧～")

        val list = mutableListOf<String>()

        list += if (count == 1) {
            "这段时间只有 1 次记录，样本有点少，多记几次总结会更准。"
        } else {
            "这段时间共拉了 $count 次，平均每天 %.2f 次。".format(perDay)
        }

        val ratio = healthyCount * 100 / count
        list += when {
            ratio >= 70 -> "有 $ratio% 的记录是理想的成形便，肠道状态很稳。"
            ratio >= 40 -> "约 $ratio% 是理想成形便，还算中规中矩。"
            else -> "只有 $ratio% 是理想成形便，形状偏干或偏稀的偏多。"
        }

        when {
            perDay > 3 -> list += "日均超过 3 次，偏频繁，如果伴随腹痛或水样便要多留意。"
            perDay < 0.3 && days >= 3 -> list += "日均不到 0.3 次，可能有点便秘倾向，多喝水、多吃蔬果。"
            perDay in 0.5..2.5 -> list += "频率落在健康区间（每天 0.5~2.5 次），节奏不错。"
        }

        dominantShape?.let {
            list += "最常见的形状是「${it.label}」：${it.desc}。"
        }
        dominantColor?.let {
            list += "最常见的颜色是「${it.label}」：${it.desc}。"
        }
        if (messyCount > 0) {
            val messyRatio = messyCount * 100 / count
            list += "有 $messyRatio% 的次数残留偏多（擦不干净），通常和油腻饮食或消化过快有关。"
        }
        warningColors.forEach {
            list += "出现「${it.label}」，${it.desc}。若持续出现建议就医检查。"
        }
        peakHour?.let {
            list += "你最常在 %d 点左右如厕，可以留意这个时间段的饮食规律。".format(it)
        }
        val best = entries.maxByOrNull { HealthScore.score(it) }
        val worst = entries.minByOrNull { HealthScore.score(it) }
        if (best != null && worst != null && count >= 3 && best.id != worst.id) {
            list += "最佳的一次是 ${best.timeLabel()}（${HealthScore.score(best)} 分），" +
                "最需要注意的一次是 ${worst.timeLabel()}（${HealthScore.score(worst)} 分）。"
        }
        if (count >= 3) {
            list += avgGrade.advice
        }
        return list
    }

    /** 区间末端的短标签，用于生成总结文案 */
    companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000

        fun hourOf(ts: Long): Int {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = ts
            return cal.get(java.util.Calendar.HOUR_OF_DAY)
        }

        /** 当天 00:00 的时间戳 */
        fun floorToDay(ts: Long): Long {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = ts
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}

/** "7月3日 08:15" 这样的短标签 */
fun PoopEntry.timeLabel(): String {
    val fmt = java.text.SimpleDateFormat("M月d日 HH:mm", java.util.Locale.CHINA)
    return fmt.format(java.util.Date(timestamp))
}
