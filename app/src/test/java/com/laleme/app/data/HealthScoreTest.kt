package com.laleme.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * 核心算法（健康评分 / 时间段总结）的单元测试。
 * 这些逻辑不依赖 Android Framework，可直接在 JVM 上运行。
 */
class HealthScoreTest {

    private fun ts(
        year: Int, month: Int, day: Int, hour: Int, minute: Int
    ): Long = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun entry(
        t: Long,
        shape: Int = PoopShape.IDEAL.value,
        color: Int = PoopColor.BROWN.value,
        residue: Int = PoopResidue.LIGHT.value,
        id: Long = 0L
    ) = PoopEntry(id = id, timestamp = t, shape = shape, color = color, residue = residue)

    /* ---------------- 单次评分 ---------------- */

    @Test
    fun `理想的一坨应当得到高分`() {
        val s = HealthScore.score(
            PoopShape.IDEAL.value,
            PoopColor.BROWN.value,
            PoopResidue.CLEAN.value
        )
        assertEquals(100, s)
        assertEquals(HealthGrade.EXCELLENT, HealthGrade.of(s))
    }

    @Test
    fun `灰白色需要显著扣分并落到警报或注意`() {
        val s = HealthScore.score(
            PoopShape.IDEAL.value,
            PoopColor.PALE.value,
            PoopResidue.LIGHT.value
        )
        assertTrue("灰白分数应偏低，实际 $s", s < 70)
        assertEquals(HealthGrade.of(s), HealthGrade.of(s))
        assertTrue(HealthGrade.of(s).ordinal >= HealthGrade.FAIR.ordinal)
    }

    @Test
    fun `评分始终落在 0 到 100 之间且覆盖所有组合`() {
        for (shape in 0..4) {
            for (color in 0..5) {
                for (residue in 0..4) {
                    val s = HealthScore.score(shape, color, residue)
                    assertTrue("score=$s 越界 shape=$shape color=$color residue=$residue", s in 0..100)
                }
            }
        }
    }

    @Test
    fun `形状越接近黄金便分数越高`() {
        val hard = HealthScore.score(PoopShape.VERY_HARD.value, 1, 1)
        val dull = HealthScore.score(PoopShape.HARD.value, 1, 1)
        val ideal = HealthScore.score(PoopShape.IDEAL.value, 1, 1)
        assertTrue("干硬 < 黄金便", dull < ideal)
        assertTrue("羊粪球 < 干硬", hard < dull)
    }

    @Test
    fun `等级边界正确`() {
        assertEquals(HealthGrade.EXCELLENT, HealthGrade.of(100))
        assertEquals(HealthGrade.EXCELLENT, HealthGrade.of(85))
        assertEquals(HealthGrade.GOOD, HealthGrade.of(84))
        assertEquals(HealthGrade.FAIR, HealthGrade.of(50))
        assertEquals(HealthGrade.NOTICE, HealthGrade.of(49))
        assertEquals(HealthGrade.ALERT, HealthGrade.of(0))
    }

    /* ---------------- 总结 ---------------- */

    @Test
    fun `空数据的总结不应崩溃并给出引导`() {
        val sum = PoopSummary(ts(2025, 1, 1, 0, 0), ts(2025, 1, 7, 23, 59), emptyList())
        assertEquals(0, sum.count)
        assertEquals(0, sum.avgScore)
        assertEquals(0, sum.activeDays)
        assertTrue(sum.insights().isNotEmpty())
        assertEquals(null, sum.dominantShape)
        assertEquals(null, sum.peakHour)
    }

    @Test
    fun `统计计数与分布正确`() {
        val from = ts(2025, 3, 1, 0, 0)
        val to = ts(2025, 3, 7, 23, 59)
        val list = listOf(
            entry(ts(2025, 3, 1, 8, 10), id = 1),
            entry(ts(2025, 3, 1, 20, 5), id = 2),
            entry(ts(2025, 3, 2, 9, 0), shape = PoopShape.LIQUID.value, residue = PoopResidue.ENDLESS.value, id = 3),
            entry(
                ts(2025, 3, 3, 9, 0),
                color = PoopColor.PALE.value,
                residue = PoopResidue.CLEAN.value,
                id = 4
            )
        )
        val sum = PoopSummary(from, to, list)

        assertEquals(4, sum.count)
        assertEquals(7L, sum.days)
        assertEquals(3, sum.activeDays)                       // 3/1、3/2、3/3
        assertEquals(3, sum.shapeDist[PoopShape.IDEAL.value])
        assertEquals(1, sum.shapeDist[PoopShape.LIQUID.value])
        assertEquals(1, sum.colorDist[PoopColor.PALE.value])
        assertEquals(3, sum.colorDist[PoopColor.BROWN.value])
        assertEquals(2, sum.residueDist[PoopResidue.LIGHT.value])
        assertEquals(1, sum.residueDist[PoopResidue.CLEAN.value])
        assertEquals(1, sum.residueDist[PoopResidue.ENDLESS.value])
        assertEquals(1, sum.messyCount)                       // residue >= 3 只有一条
        assertEquals(3, sum.healthyCount)                     // 黄金便 3 条
        assertEquals(PoopShape.IDEAL, sum.dominantShape)
        assertEquals(1, sum.warningColors.size)               // 出现过灰白
        assertNotNull(sum.peakHour)
    }

    @Test
    fun `按天聚合与峰值时段正确`() {
        val list = listOf(
            entry(ts(2025, 5, 10, 7, 30), id = 1),
            entry(ts(2025, 5, 10, 7, 45), id = 2),
            entry(ts(2025, 5, 11, 22, 0), id = 3)
        )
        val sum = PoopSummary(ts(2025, 5, 10, 0, 0), ts(2025, 5, 11, 23, 59), list)
        val byDay = sum.byDay
        assertEquals(2, byDay.size)
        assertEquals(2, byDay.values.max())
        assertEquals(7, sum.peakHour)                         // 7 点出现 2 次
    }

    @Test
    fun `总结文字包含关键信息且不为空`() {
        val list = (1..5).map {
            entry(ts(2025, 6, it, 8, 0), id = it.toLong())
        }
        val sum = PoopSummary(ts(2025, 6, 1, 0, 0), ts(2025, 6, 5, 23, 59), list)
        val insights = sum.insights()
        assertTrue(insights.isNotEmpty())
        assertTrue(insights.all { it.isNotBlank() })
        assertTrue("应提到总次数", insights.any { it.contains("5 次") })
    }

    @Test
    fun `形状与颜色的枚举取值是连续且唯一的`() {
        assertEquals(listOf(0, 1, 2, 3, 4), PoopShape.entries.map { it.value })
        assertEquals(listOf(0, 1, 2, 3, 4, 5), PoopColor.entries.map { it.value })
        assertEquals(listOf(0, 1, 2, 3, 4), PoopResidue.entries.map { it.value })
        // 越界时回退到安全默认值
        assertEquals(PoopShape.IDEAL, PoopShape.of(99))
        assertEquals(PoopColor.BROWN, PoopColor.of(-1))
        assertEquals(PoopResidue.LIGHT, PoopResidue.of(42))
    }

    @Test
    fun `实体能正确解析枚举`() {
        val e = entry(ts(2025, 1, 1, 1, 1), shape = 4, color = 5, residue = 0)
        assertEquals(PoopShape.LIQUID, e.shapeEnum)
        assertEquals(PoopColor.PALE, e.colorEnum)
        assertEquals(PoopResidue.CLEAN, e.residueEnum)
    }

    /* ---------------- 评分区间 ---------------- */

    @Test
    fun `评分区间是真正的 0 到 100`() {
        // 三个维度全部最差 → 必须正好 0 分
        val worst = HealthScore.score(
            PoopShape.VERY_HARD.value,
            PoopColor.PALE.value,
            PoopResidue.ENDLESS.value
        )
        assertEquals("三维度全最差应当是 0 分", 0, worst)

        // 全部最好 → 100 分
        val best = HealthScore.score(
            PoopShape.IDEAL.value,
            PoopColor.BROWN.value,
            PoopResidue.CLEAN.value
        )
        assertEquals(100, best)
    }

    @Test
    fun `权重决定各维度的影响力`() {
        // 颜色权重最高（50%）：颜色最差时，即使形状和残留都是最好，也掉到一般区间
        assertEquals(48, HealthScore.score(2, 5, 0))

        // 形状 40%：形状最差、其余最好 → 63 分
        assertEquals(63, HealthScore.score(0, 1, 0))

        // 残留只占 10%，影响最小：残留最差、其余最好 → 仍接近满分
        assertEquals(90, HealthScore.score(2, 1, 4))

        // 也就是说：颜色 > 形状 > 残留，符合医学上对颜色的重视程度
        assertTrue(HealthScore.score(2, 5, 0) < HealthScore.score(0, 1, 0))
    }

    @Test
    fun `最差组合落在警报等级`() {
        val worst = HealthScore.score(
            PoopShape.VERY_HARD.value,
            PoopColor.PALE.value,
            PoopResidue.ENDLESS.value
        )
        assertEquals(HealthGrade.ALERT, HealthGrade.of(worst))
    }

    /* ---------------- 天数计算 ---------------- */

    @Test
    fun `近7天正好覆盖7个自然日`() {
        val today = ts(2025, 7, 10, 0, 0)
        val endOfToday = ts(2025, 7, 10, 23, 59)
        assertEquals(1L, PoopSummary(today, endOfToday, emptyList()).days)

        // 往前推 6 天 → 共 7 天
        val from7 = ts(2025, 7, 4, 0, 0)
        assertEquals(7L, PoopSummary(from7, endOfToday, emptyList()).days)
    }

    @Test
    fun `天数按日历天算_不受时分秒影响`() {
        // 7/1 23:50 → 7/2 00:10 虽然不足 24 小时，但跨了 2 个自然日
        val from = ts(2025, 7, 1, 23, 50)
        val to = ts(2025, 7, 2, 0, 10)
        assertEquals(2L, PoopSummary(from, to, emptyList()).days)
    }
}
