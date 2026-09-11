package com.laleme.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laleme.app.data.HealthGrade
import com.laleme.app.data.HealthScore
import com.laleme.app.data.PoopEntry
import com.laleme.app.ui.components.FloatingPoop
import com.laleme.app.ui.components.GlassCard
import com.laleme.app.ui.components.GradientText
import com.laleme.app.ui.components.HealthBars
import com.laleme.app.ui.components.StaggerIn
import com.laleme.app.ui.theme.Cocoa
import com.laleme.app.ui.theme.CocoaFaint
import com.laleme.app.ui.theme.CocoaSoft
import com.laleme.app.ui.theme.Honey
import com.laleme.app.ui.theme.HoneyDeep
import com.laleme.app.ui.theme.color
import com.laleme.app.ui.theme.tint
import kotlinx.coroutines.launch
import java.util.Calendar

/* ------------------------------------------------------------------ */
/*  顶部栏                                                              */
/* ------------------------------------------------------------------ */

@Composable
fun GlassTopBar(
    title: String,
    subtitle: String,
    onSummary: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    /** 弹层打开时把 ＋ 收起来，形成「按钮让位给面板」的过渡 */
    fabHidden: Boolean = false
) {
    // 用 Box 而不是 Row 来排版：
    // 左右两块各自「固定宽度 + 绝对靠边」，中间标题单独一层居中。
    // 这样标题是相对**屏幕**居中，而不是相对左右剩下的空隙居中
    // （之前左边 94dp、右边 52dp，标题就被带偏了）。
    val sideWidth = 78.dp
    val sideHeight = 46.dp
    val fabSize = 52.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(fabSize),
        contentAlignment = Alignment.Center
    ) {
        // ---- 中间：标题（相对屏幕居中）----
        Column(
            modifier = Modifier.padding(horizontal = sideWidth),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GradientText(
                text = title,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = CocoaFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ---- 左上角：拉屎总结 ----
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(sideWidth, sideHeight)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xF0FFFDF7), Color(0xC0FFF6DE))
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(Color(0x88FFFFFF), Color(0x33FFFFFF))),
                    RoundedCornerShape(20.dp)
                )
                .clickable { onSummary() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = HoneyDeep,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "总结",
                    style = MaterialTheme.typography.labelLarge,
                    color = Cocoa,
                    maxLines = 1
                )
            }
        }

        // ---- 右上角：添加 + ----（固定 52dp，永远在屏幕内）
        val scope = rememberCoroutineScope()
        var pressed by remember { mutableStateOf(false) }

        val scale by animateFloatAsState(
            targetValue = if (fabHidden) 0.2f else if (pressed) 0.88f else 1f,
            animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
            label = "addScale"
        )
        val fabAlpha by animateFloatAsState(
            targetValue = if (fabHidden) 0f else 1f,
            animationSpec = tween(200, easing = FastOutSlowInEasing),
            label = "addAlpha"
        )
        // 按下时从按钮中心扩散一圈波纹
        val pressPulse = remember { Animatable(0f) }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(fabSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = fabAlpha
                    rotationZ = if (fabHidden) 90f else 0f
                },
            contentAlignment = Alignment.Center
        ) {
            // 波纹
            if (pressPulse.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .size(fabSize)
                        .graphicsLayer {
                            val p = pressPulse.value
                            scaleX = 1f + p * 0.9f
                            scaleY = 1f + p * 0.9f
                            alpha = (1f - p) * 0.55f
                        }
                        .clip(CircleShape)
                        .background(Honey)
                )
            }

            Box(
                modifier = Modifier
                    .size(fabSize)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFFFD54F), Honey, HoneyDeep))
                    )
                    .border(1.5.dp, Color(0x88FFFFFF), CircleShape)
                    .clickable {
                        pressed = true
                        scope.launch {
                            // 波纹和面板滑入同时开始：不再等动画演完再开面板，
                            // 否则点下去会有一下空档，滑入效果也被吃掉。
                            launch {
                                pressPulse.snapTo(0f)
                                pressPulse.animateTo(
                                    1f,
                                    tween(420, easing = FastOutSlowInEasing)
                                )
                                pressPulse.snapTo(0f)
                            }
                            onAdd()
                            kotlinx.coroutines.delay(130)
                            pressed = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "添加拉屎记录",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  顶部统计条                                                          */
/* ------------------------------------------------------------------ */

@Composable
fun StatsStrip(
    total: Int,
    today: Int,
    week: Int,
    avgScore: Int,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth(), corner = 26.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCell("累计", total.toString(), "次", Modifier.weight(1f))
            Divider()
            StatCell("今天", today.toString(), "次", Modifier.weight(1f))
            Divider()
            StatCell("本周", week.toString(), "次", Modifier.weight(1f))
            Divider()
            StatCell("均分", avgScore.toString(), "分", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                color = Cocoa
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = CocoaFaint,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CocoaFaint
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(Color(0x22000000))
    )
}

/* ------------------------------------------------------------------ */
/*  日期分组标题                                                        */
/* ------------------------------------------------------------------ */

@Composable
fun DayHeader(dayStart: Long, count: Int, modifier: Modifier = Modifier) {
    val cal = Calendar.getInstance().apply { timeInMillis = dayStart }
    val today = Calendar.getInstance()
    val isToday = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    val yesterday = today.clone() as Calendar
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    val isYesterday = cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)

    val label = when {
        isToday -> "今天"
        isYesterday -> "昨天"
        else -> weekNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }
    val date = "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Cocoa
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = date,
            style = MaterialTheme.typography.labelSmall,
            color = CocoaFaint
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0x33F2B01E))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$count 次",
                style = MaterialTheme.typography.labelSmall,
                color = HoneyDeep,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  单条记录（横条）                                                    */
/* ------------------------------------------------------------------ */

@Composable
fun PoopRow(
    entry: PoopEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    /** 刚添加的那一条会播放一次高亮登场 */
    highlight: Boolean = false
) {
    val score = remember(entry.shape, entry.color, entry.residue) {
        HealthScore.score(entry.shape, entry.color, entry.residue)
    }
    val grade = remember(score) { HealthGrade.of(score) }

    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    // 新记录的登场动画：轻微放大 + 一次金色高亮
    val appear = remember { Animatable(if (highlight) 0f else 1f) }
    LaunchedEffect(highlight) {
        if (highlight) {
            appear.snapTo(0f)
            appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
        } else {
            appear.snapTo(1f)
        }
    }
    // 删除动画：先整条向右滑出淡出，再真正删库
    val deleteAnim = remember { Animatable(0f) }
    fun startDelete() {
        if (deleteAnim.value > 0f && deleteAnim.isRunning) return
        scope.launch {
            if (containerWidthPx > 0f) {
                deleteAnim.animateTo(
                    containerWidthPx * 1.1f,
                    tween(320, easing = FastOutSlowInEasing)
                )
            }
            onDelete()
        }
    }

    val highlightGlow by animateFloatAsState(
        targetValue = if (highlight && appear.value > 0.02f && appear.value < 0.999f) 1f else 0f,
        animationSpec = tween(600),
        label = "glow"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
            .graphicsLayer {
                val p = appear.value
                alpha = (0.2f + 0.8f * p).coerceIn(0f, 1f)
                scaleX = 0.94f + 0.06f * p
                scaleY = 0.94f + 0.06f * p
                // 删除时整条向右滑出并淡出
                translationX = deleteAnim.value
                if (deleteAnim.value > 0f && containerWidthPx > 0f) {
                    val t = (deleteAnim.value / (containerWidthPx * 1.1f)).coerceIn(0f, 1f)
                    alpha = alpha * (1f - t)
                    rotationZ = 4f * t
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ---- 记录卡片 ----
        GlassCard(
            corner = 24.dp,
            elevation = if (highlight) 14.dp else 8.dp,
            blurRadius = 26.dp,
            sheen = highlight,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (highlightGlow > 0.01f) {
                        Modifier.drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                drawRoundRect(
                                    color = Color(0xFFFFC93C).copy(alpha = 0.55f * highlightGlow),
                                    cornerRadius = CornerRadius(24.dp.toPx()),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        }
                    } else Modifier
                )
                .clickable { onClick() }
        ) {
            Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：健康程度
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(56.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(grade.color.copy(alpha = 0.30f), grade.color.copy(alpha = 0.08f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = grade.emoji, fontSize = 19.sp)
                        }
                        Spacer(Modifier.height(5.dp))
                        HealthBars(score = score, color = grade.color, barWidth = 5.dp, maxHeight = 26.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = grade.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = grade.color,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = CocoaFaint
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(64.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0x22000000), Color.Transparent)
                                )
                            )
                    )

                    Spacer(Modifier.width(12.dp))

                    // 右侧：细节（小字体）
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatFullTime(entry.timestamp),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            color = Cocoa
                        )
                        Spacer(Modifier.height(6.dp))
                        MiniRow("形状", entry.shapeEnum.emoji, entry.shapeEnum.label, entry.shapeEnum.tint)
                        Spacer(Modifier.height(3.dp))
                        MiniRow("颜色", entry.colorEnum.emoji, entry.colorEnum.label, entry.colorEnum.tint)
                        Spacer(Modifier.height(3.dp))
                        MiniRow("残留", entry.residueEnum.emoji, entry.residueEnum.label, entry.residueEnum.tint)
                    }

                    Spacer(Modifier.width(6.dp))

                    // ---- 删除按钮：在横条内部、靠右垂直居中 ----
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x1AE5533D))
                            .border(1.dp, Color(0x33E5533D), CircleShape)
                            .clickable { startDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "删除这条记录",
                            tint = Color(0xFFE5533D),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }

@Composable
private fun MiniRow(label: String, emoji: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = CocoaFaint,
            modifier = Modifier.width(26.dp)
        )
        Text(text = emoji, fontSize = 10.sp)
        Spacer(Modifier.width(3.dp))
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(valueColor)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
            color = CocoaSoft,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ------------------------------------------------------------------ */
/*  空状态                                                              */
/* ------------------------------------------------------------------ */
@Composable
fun EmptyState(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FloatingPoop(size = 116.dp)
        Spacer(Modifier.height(18.dp))
        Text(
            text = "还没有记录哦",
            style = MaterialTheme.typography.titleLarge,
            color = Cocoa
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "点右上角的 + 记录今天的第一次",
            style = MaterialTheme.typography.bodyMedium,
            color = CocoaFaint
        )
        Spacer(Modifier.height(22.dp))
        GlassCard(
            corner = 22.dp,
            modifier = Modifier.clickable { onAdd() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "\uD83D\uDCA9", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "现在就来一条",
                    style = MaterialTheme.typography.labelLarge,
                    color = Cocoa
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  主界面列表                                                          */
/* ------------------------------------------------------------------ */

@Composable
fun PoopList(
    entries: List<PoopEntry>,
    onEdit: (PoopEntry) -> Unit,
    onDelete: (PoopEntry) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // 只给「新出现的那一条」播一次高亮登场（首次加载不播）
    var seenIds by remember { mutableStateOf<Set<Long>?>(null) }
    var highlightId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(entries) {
        val ids = entries.map { it.id }.toSet()
        val previous = seenIds
        if (previous == null) {
            seenIds = ids                      // 首次加载：全部视为已见
        } else {
            val added = ids - previous
            seenIds = ids
            added.maxOrNull()?.let { highlightId = it }
        }
    }

    if (entries.isEmpty()) {
        Box(modifier = modifier.fillMaxSize()) {
            EmptyState(onAdd = onAdd)
        }
        return
    }

    // 计算每条的所属“天”，用于分组标题
    val dayOf: List<Long> = entries.map { it.timestamp / DAY * DAY }
    // 预先聚合每天的次数，避免在列表项里做 O(n) 统计
    val countPerDay: Map<Long, Int> = remember(dayOf) { dayOf.groupingBy { it }.eachCount() }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        itemsIndexed(
            items = entries,
            key = { _, e -> e.id }
        ) { index, entry ->
            val day = dayOf[index]
            Column {
                if (index == 0 || dayOf[index - 1] != day) {
                    StaggerIn(index = index, baseDelayMs = 40L) {
                        DayHeader(dayStart = day, count = countPerDay[day] ?: 1)
                    }
                }
                StaggerIn(index = index, baseDelayMs = 40L) {
                    PoopRow(
                        entry = entry,
                        onClick = { onEdit(entry) },
                        onDelete = { onDelete(entry) },
                        highlight = entry.id == highlightId,
                        modifier = Modifier.padding(horizontal = 0.dp)
                    )
                }
            }
        }
    }
}

const val DAY = 24L * 60 * 60 * 1000
