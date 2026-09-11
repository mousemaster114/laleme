package com.laleme.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laleme.app.data.HealthScore
import com.laleme.app.data.PoopColor
import com.laleme.app.data.PoopResidue
import com.laleme.app.data.PoopShape
import com.laleme.app.data.PoopSummary
import com.laleme.app.ui.PoopViewModel
import com.laleme.app.ui.components.AnimatedInt
import com.laleme.app.ui.components.GlassCard
import com.laleme.app.ui.components.PillChip
import com.laleme.app.ui.components.ScoreRing
import com.laleme.app.ui.components.StaggerIn
import com.laleme.app.ui.theme.Cocoa
import com.laleme.app.ui.theme.CocoaFaint
import com.laleme.app.ui.theme.CocoaSoft
import com.laleme.app.ui.theme.Honey
import com.laleme.app.ui.theme.HoneyDeep
import com.laleme.app.ui.theme.color
import com.laleme.app.ui.theme.tint

/* ------------------------------------------------------------------ */
/*  拉屎总结页                                                          */
/* ------------------------------------------------------------------ */

private enum class Preset(val label: String) {
    WEEK7("近 7 天"),
    DAY30("近 30 天"),
    MONTH("本月"),
    YEAR("今年"),
    ALL("全部"),
    CUSTOM("自定义")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(vm: PoopViewModel, onBack: () -> Unit) {
    val summary by vm.summary.collectAsStateWithLifecycle()
    val range by vm.range.collectAsStateWithLifecycle()

    var preset by remember { mutableStateOf(Preset.WEEK7) }
    var showRangePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.statusBarsPadding().height(6.dp))

        // 顶部：返回 + 标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x66FFFFFF))
                    .border(1.dp, Color(0x44FFFFFF), CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = Cocoa,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "拉屎总结",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                    color = Cocoa
                )
                Text(
                    text = "${formatDate(range.first)} - ${formatDate(range.last)} · ${summary.days} 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = CocoaFaint
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 时间段选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Preset.entries.forEach { p ->
                PillChip(
                    text = p.label,
                    selected = preset == p,
                    onClick = {
                        when (p) {
                            Preset.WEEK7 -> {
                                preset = p; vm.setPresetDays(7)
                            }

                            Preset.DAY30 -> {
                                preset = p; vm.setPresetDays(30)
                            }

                            Preset.MONTH -> {
                                preset = p; vm.setThisMonth()
                            }

                            Preset.YEAR -> {
                                preset = p; vm.setThisYear()
                            }

                            Preset.ALL -> {
                                preset = p; vm.setAllTime()
                            }

                            Preset.CUSTOM -> {
                                showRangePicker = true
                            }
                        }
                    },
                    leading = if (p == Preset.CUSTOM) "\uD83D\uDCC5" else null
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            // ---- 总分卡 ----
            StaggerIn(0) {
                OverallCard(summary)
            }

            Spacer(Modifier.height(14.dp))

            // ---- 关键指标 ----
            StaggerIn(1) {
                MetricGrid(summary)
            }

            Spacer(Modifier.height(14.dp))

            // ---- 形状分布 ----
            if (summary.count > 0) {
                StaggerIn(2) {
                    DistributionCard(
                        title = "形状分布",
                        subtitle = "最理想的是「黄金便」和「软香蕉」",
                        items = PoopShape.entries.map { s ->
                            DistItem(s.label, s.emoji, s.tint, summary.shapeDist.getOrElse(s.value) { 0 })
                        }
                    )
                }

                Spacer(Modifier.height(14.dp))

                StaggerIn(3) {
                    DistributionCard(
                        title = "颜色分布",
                        subtitle = "棕色系是健康色，灰白 / 发黑要留意",
                        items = PoopColor.entries.map { c ->
                            DistItem(c.label, c.emoji, c.tint, summary.colorDist.getOrElse(c.value) { 0 })
                        }
                    )
                }

                Spacer(Modifier.height(14.dp))

                StaggerIn(4) {
                    DistributionCard(
                        title = "残留情况",
                        subtitle = "擦得干不干净，反映消化和饮食油腻程度",
                        items = PoopResidue.entries.map { r ->
                            DistItem(r.label, r.emoji, r.tint, summary.residueDist.getOrElse(r.value) { 0 })
                        }
                    )
                }

                Spacer(Modifier.height(14.dp))

                StaggerIn(5) {
                    TrendCard(summary)
                }

                Spacer(Modifier.height(14.dp))

                StaggerIn(6) {
                    InsightsCard(summary)
                }
            } else {
                StaggerIn(2) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), corner = 26.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "\uD83D\uDD73\uFE0F", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "这个时间段没有记录",
                                style = MaterialTheme.typography.titleMedium,
                                color = Cocoa
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "换一个时间段，或者先去记一条",
                                style = MaterialTheme.typography.bodySmall,
                                color = CocoaFaint
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (showRangePicker) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = range.first,
            initialSelectedEndDateMillis = range.last
        )
        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val s = pickerState.selectedStartDateMillis
                    val e = pickerState.selectedEndDateMillis
                    if (s != null && e != null) {
                        preset = Preset.CUSTOM
                        vm.setRange(startOfDay(s), startOfDay(e) + DAY_MILLIS - 1)
                    }
                    showRangePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) { Text("取消") }
            }
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.height(520.dp))
        }
    }
}

/* ------------------------------------------------------------------ */
/*  总分卡                                                              */
/* ------------------------------------------------------------------ */

@Composable
private fun OverallCard(summary: PoopSummary) {
    val grade = summary.avgGrade
    GlassCard(modifier = Modifier.fillMaxWidth(), corner = 28.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreRing(score = summary.avgScore, color = grade.color, size = 104.dp)

                Spacer(Modifier.width(18.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = grade.emoji, fontSize = 17.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = grade.label,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                            color = grade.color
                        )
                    }
                    Spacer(Modifier.height(6.dp))

                    // 次数和日均分开两行：
                    // 挤在同一行时，窄屏上「日均」会被挤到贴边，甚至和数字撞在一起。
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedInt(
                            value = summary.count,
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                            color = Cocoa
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "次",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CocoaSoft,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        // 写清分母，避免被理解成「平均每天只拉这么点」
                        text = "这段时间平均每天 %.2f 次".format(summary.perDay),
                        style = MaterialTheme.typography.bodySmall,
                        color = CocoaSoft
                    )
                }
            }

            if (summary.count > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = grade.advice,
                    style = MaterialTheme.typography.bodySmall,
                    color = CocoaSoft
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  指标网格                                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun MetricGrid(summary: PoopSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCell("有记录的天数", "${summary.activeDays}", "天", Modifier.weight(1f))
            MetricCell("理想成形", "${summary.healthyCount}", "次", Modifier.weight(1f))
            MetricCell("残留偏多", "${summary.messyCount}", "次", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCell(
                "最常见形状",
                summary.dominantShape?.label ?: "-",
                summary.dominantShape?.emoji ?: "",
                Modifier.weight(1f)
            )
            MetricCell(
                "最常见颜色",
                summary.dominantColor?.label ?: "-",
                summary.dominantColor?.emoji ?: "",
                Modifier.weight(1f)
            )
            MetricCell(
                "高频时段",
                summary.peakHour?.let { "%d点".format(it) } ?: "-",
                "\u23F0",
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    val showUnit = unit.isNotEmpty() && unit.length <= 2 && unit.all { it.code < 0x2000 }
    GlassCard(modifier = modifier, corner = 20.dp, elevation = 5.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                color = Cocoa,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            if (showUnit) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = CocoaFaint,
                    maxLines = 1
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = CocoaFaint,
                maxLines = 1
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  分布卡                                                              */
/* ------------------------------------------------------------------ */

private data class DistItem(
    val label: String,
    val emoji: String,
    val color: Color,
    val count: Int
)

@Composable
private fun DistributionCard(
    title: String,
    subtitle: String,
    items: List<DistItem>
) {
    val total = items.sumOf { it.count }.coerceAtLeast(1)
    GlassCard(modifier = Modifier.fillMaxWidth(), corner = 26.dp) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Cocoa
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = CocoaFaint
            )
            Spacer(Modifier.height(14.dp))

            items.forEachIndexed { index, item ->
                val fraction = item.count.toFloat() / total
                val animated by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = tween(900 + index * 90, easing = FastOutSlowInEasing),
                    label = "dist$index"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = item.emoji, fontSize = 13.sp)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = CocoaSoft,
                        modifier = Modifier.width(52.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x14000000))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animated.coerceIn(0f, 1f))
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(item.color.copy(alpha = 0.65f), item.color)
                                    )
                                )
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = "${item.count}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = if (item.count > 0) Cocoa else CocoaFaint.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(16.dp)
                    )
                }
                if (index != items.lastIndex) Spacer(Modifier.height(9.dp))
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  趋势卡                                                              */
/* ------------------------------------------------------------------ */

@Composable
private fun TrendCard(summary: PoopSummary) {
    val byDay = summary.byDay
    val days = remember(summary.from, summary.to) {
        val list = mutableListOf<Long>()
        var d = startOfDay(summary.from)
        val end = startOfDay(summary.to)
        while (d <= end && list.size < 60) {
            list += d
            d += DAY_MILLIS
        }
        list
    }
    val maxCount = (days.maxOfOrNull { byDay[it] ?: 0 } ?: 0).coerceAtLeast(1)

    GlassCard(modifier = Modifier.fillMaxWidth(), corner = 26.dp) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.DateRange,
                    contentDescription = null,
                    tint = HoneyDeep,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "每日次数趋势",
                    style = MaterialTheme.typography.titleMedium,
                    color = Cocoa
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "峰值 $maxCount 次",
                    style = MaterialTheme.typography.labelSmall,
                    color = CocoaFaint
                )
            }
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                days.forEachIndexed { index, day ->
                    val c = byDay[day] ?: 0
                    val target = if (c == 0) 0.035f else (c.toFloat() / maxCount) * 0.86f + 0.14f
                    val animated by animateFloatAsState(
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = 0.62f,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "trend$index"
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (c > 0) {
                            Text(
                                text = "$c",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = HoneyDeep,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.weight((1f - animated).coerceAtLeast(0.001f)))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(animated.coerceAtLeast(0.001f))
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (c == 0) {
                                        Brush.verticalGradient(
                                            listOf(Color(0x1A000000), Color(0x0D000000))
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(Color(0xFFFFD54F), Honey, HoneyDeep)
                                        )
                                    }
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDate(summary.from),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = CocoaFaint
                )
                if (days.size > 1) {
                    Text(
                        text = formatDate(days[days.size / 2]),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = CocoaFaint
                    )
                }
                Text(
                    text = formatDate(summary.to),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = CocoaFaint
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  总结文字卡                                                          */
/* ------------------------------------------------------------------ */

@Composable
private fun InsightsCard(summary: PoopSummary) {
    val insights = remember(summary) { summary.insights() }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        corner = 26.dp,
        tint = Color(0x99FFF6DC)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFFD54F), Honey))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "这段时间的总结",
                    style = MaterialTheme.typography.titleMedium,
                    color = Cocoa
                )
            }

            Spacer(Modifier.height(14.dp))

            insights.forEach { line ->
                Row(modifier = Modifier.padding(bottom = 9.dp)) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(HoneyDeep)
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CocoaSoft
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x1A000000))
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "以上内容基于你记录的数据自动生成，仅供参考，不能代替医生的诊断。",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = CocoaFaint
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  工具                                                                */
/* ------------------------------------------------------------------ */

const val DAY_MILLIS = 24L * 60 * 60 * 1000
