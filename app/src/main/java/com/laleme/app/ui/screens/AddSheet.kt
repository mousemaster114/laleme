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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laleme.app.data.HealthGrade
import com.laleme.app.data.HealthScore
import com.laleme.app.data.PoopColor
import com.laleme.app.data.PoopResidue
import com.laleme.app.data.PoopShape
import com.laleme.app.ui.SheetState
import com.laleme.app.ui.components.GestureSlider
import com.laleme.app.ui.components.GlassCard
import com.laleme.app.ui.theme.Cocoa
import com.laleme.app.ui.theme.CocoaFaint
import com.laleme.app.ui.theme.CocoaSoft
import com.laleme.app.ui.theme.Honey
import com.laleme.app.ui.theme.HoneyDeep
import com.laleme.app.ui.theme.color
import com.laleme.app.ui.theme.tint
import java.util.Calendar

/* ------------------------------------------------------------------ */
/*  添加 / 编辑弹层                                                     */
/* ------------------------------------------------------------------ */

/** 把秒和毫秒清零，确保记录精确到分钟 */
private fun normalizeToMinute(ts: Long): Long = Calendar.getInstance().apply {
    timeInMillis = ts
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun AddSheet(
    state: SheetState,
    onDismiss: () -> Unit,
    onSave: (timestamp: Long, shape: Int, color: Int, residue: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val editing = (state as? SheetState.Edit)?.entry
    val visible = state !is SheetState.Hidden

    // 弹层状态一变就重新初始化表单：新增时 key 每次都不一样，
    // 编辑时用记录 id，所以绝不会残留上一次的输入。
    val formKey: Any = when (state) {
        is SheetState.New -> state.key
        is SheetState.Edit -> state.entry.id
        SheetState.Hidden -> "hidden"
    }

    // 时间统一把秒和毫秒归零，保证“精确到分钟”
    var cal by remember(formKey) {
        mutableLongStateOf(normalizeToMinute(editing?.timestamp ?: System.currentTimeMillis()))
    }
    var shape by remember(formKey) {
        mutableIntStateOf(editing?.shape ?: PoopShape.IDEAL.value)
    }
    var color by remember(formKey) {
        mutableIntStateOf(editing?.color ?: PoopColor.BROWN.value)
    }
    var residue by remember(formKey) {
        mutableIntStateOf(editing?.residue ?: PoopResidue.LIGHT.value)
    }

    // ------------------------------------------------------------------
    // 面板整体自下而上滑入
    //
    // 这里不用 AnimatedVisibility + spring：那样进度由物理参数推算，
    // 实测滑得太快、几乎看不出来。改成手动驱动一个 0→1 的进度值，
    // 时长和缓动都是确定的，滑入一定看得见。
    // ------------------------------------------------------------------
    val slide = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            slide.snapTo(0f)
            slide.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 460,
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            slide.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    val progress = slide.value
    // 完全收起后不参与布局与命中测试
    if (progress <= 0.001f) return

    // 用面板的**实际测量高度**作为滑动距离，保证它真的从屏幕外开始
    var panelHeightPx by remember { mutableFloatStateOf(1f) }
    // 缓出曲线：起初位移大、收尾变慢，滑入更有「落位」的重量感
    val eased = 1f - (1f - progress) * (1f - progress) * (1f - progress)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 遮罩：随面板一起淡入淡出；收起过程中不接受点击
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.coerceIn(0f, 1f) }
                .background(Color(0x66201A0E))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = progress > 0.98f
                ) { onDismiss() }
        )

        // 面板：从屏幕下方整体滑上来，内部内容一次性就位、无任何入场动画
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { if (it.height > 0) panelHeightPx = it.height.toFloat() }
                .graphicsLayer {
                    translationY = (1f - eased) * panelHeightPx
                }
        ) {
            SheetBody(
                isEditing = editing != null,
                timestamp = cal,
                onTimestampChange = { cal = it },
                shape = shape,
                onShapeChange = { shape = it },
                color = color,
                onColorChange = { color = it },
                residue = residue,
                onResidueChange = { residue = it },
                onDismiss = onDismiss,
                onSave = { onSave(cal, shape, color, residue) }
            )
        }
    }
}

@Composable
private fun SheetBody(
    isEditing: Boolean,
    timestamp: Long,
    onTimestampChange: (Long) -> Unit,
    shape: Int,
    onShapeChange: (Int) -> Unit,
    color: Int,
    onColorChange: (Int) -> Unit,
    residue: Int,
    onResidueChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val score = HealthScore.score(shape, color, residue)
    val grade = HealthGrade.of(score)

    // 注意：变量名不能叫 shape，否则会遮蔽上面的 shape: Int 参数
    val sheetShape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 680.dp)
            .clip(sheetShape)
    ) {
        GlassCard(
            corner = 34.dp,
            elevation = 22.dp,
            tint = Color(0xF2FFFDF7),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // 注意：面板内容不做任何入场动画。
                // 整个面板是一块整体、自下而上滑上来的，内容一次性就位。
                SheetHandle()

                SheetHeader(
                    isEditing = isEditing,
                    score = score,
                    grade = grade,
                    onDismiss = onDismiss
                )

                Spacer(Modifier.height(18.dp))

                TimeRow(timestamp = timestamp, onTimestampChange = onTimestampChange)

                Spacer(Modifier.height(22.dp))

                // ---- 形状 ----
                val shapeEnum = PoopShape.of(shape)
                SliderSection(
                    title = "形状",
                    valueLabel = shapeEnum.label,
                    desc = shapeEnum.desc,
                    emoji = shapeEnum.emoji,
                    accent = shapeEnum.tint,
                    value = shape,
                    steps = PoopShape.entries.size,
                    onValueChange = onShapeChange,
                    activeColors = listOf(
                        PoopShape.entries.first().tint,
                        PoopShape.entries[2].tint,
                        PoopShape.entries.last().tint
                    ),
                    thumbTint = shapeEnum.tint
                )

                Spacer(Modifier.height(20.dp))

                // ---- 颜色 ----
                val colorEnum = PoopColor.of(color)
                SliderSection(
                    title = "颜色",
                    valueLabel = colorEnum.label,
                    desc = colorEnum.desc,
                    emoji = colorEnum.emoji,
                    accent = colorEnum.tint,
                    value = color,
                    steps = PoopColor.entries.size,
                    onValueChange = onColorChange,
                    trackBrush = Brush.horizontalGradient(
                        PoopColor.entries.map { it.tint }
                    ),
                    activeColors = PoopColor.entries.map { it.tint },
                    thumbTint = colorEnum.tint
                )

                Spacer(Modifier.height(20.dp))

                // ---- 残留 ----
                val residueEnum = PoopResidue.of(residue)
                SliderSection(
                    title = "屁股上的残留",
                    valueLabel = residueEnum.label,
                    desc = residueEnum.desc,
                    emoji = residueEnum.emoji,
                    accent = residueEnum.tint,
                    value = residue,
                    steps = PoopResidue.entries.size,
                    onValueChange = onResidueChange,
                    activeColors = listOf(
                        PoopResidue.entries.first().tint,
                        PoopResidue.entries[2].tint,
                        PoopResidue.entries.last().tint
                    ),
                    thumbTint = residueEnum.tint
                )

                Spacer(Modifier.height(26.dp))

                SaveButton(isEditing = isEditing, onSave = onSave)

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x33000000))
        )
    }
}

@Composable
private fun SheetHeader(
    isEditing: Boolean,
    score: Int,
    grade: HealthGrade,
    onDismiss: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 实时预览的屎 + 分数光环
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(grade.color.copy(alpha = 0.34f), grade.color.copy(alpha = 0.06f))
                    )
                )
                .border(2.dp, grade.color.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "\uD83D\uDCA9", fontSize = 28.sp)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isEditing) "编辑这条记录" else "记录这次拉屎",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                color = Cocoa
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = grade.emoji, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "健康度 ${grade.label} · $score 分",
                    style = MaterialTheme.typography.bodySmall,
                    color = grade.color,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x14000000))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "关闭",
                tint = CocoaSoft,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  时间选择（精确到分钟）                                              */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRow(timestamp: Long, onTimestampChange: (Long) -> Unit) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    val cal = remember(timestamp) { Calendar.getInstance().apply { timeInMillis = timestamp } }
    val dateText = "%d年%d月%d日".format(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
    val timeText = "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    val weekText = weekNames[cal.get(Calendar.DAY_OF_WEEK) - 1]

    GlassCard(
        corner = 24.dp,
        elevation = 5.dp,
        tint = Color(0x99FFFFFF),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = HoneyDeep,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "拉屎时间",
                    style = MaterialTheme.typography.labelLarge,
                    color = Cocoa
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "精确到分钟",
                    style = MaterialTheme.typography.labelSmall,
                    color = CocoaFaint
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // 日期
                PickBox(
                    main = dateText,
                    sub = weekText,
                    modifier = Modifier.weight(1f)
                ) { showDate = true }

                // 时间
                PickBox(
                    main = timeText,
                    sub = "时:分",
                    modifier = Modifier.weight(1f),
                    emphasize = true
                ) { showTime = true }
            }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = timestamp)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked ->
                        val pickedCal = Calendar.getInstance().apply {
                            timeInMillis = picked
                            set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, cal.get(Calendar.MINUTE))
                        }
                        onTimestampChange(pickedCal.timeInMillis)
                    }
                    showDate = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showTime) {
        // Material3 1.2 没有 TimePickerDialog，这里用系统时钟对话框（可精确选到分钟）
        val ctx = LocalContext.current
        androidx.compose.runtime.DisposableEffect(showTime) {
            val dialog = android.app.TimePickerDialog(
                ctx,
                { _, hour, minute ->
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = timestamp
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onTimestampChange(newCal.timeInMillis)
                    showTime = false
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            )
            dialog.setOnCancelListener { showTime = false }
            dialog.show()
            onDispose { dialog.dismiss() }
        }
    }
}

@Composable
private fun PickBox(
    main: String,
    sub: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
    onClick: () -> Unit
) {
    // 日期框和时间框用同一套底色 / 描边，避免一个偏黄一个偏灰；
    // 高度用统一的内边距，两个框永远一样高（之前日期框有年份更宽更高）。
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x3DF2B01E), Color(0x1FFFC93C))
                )
            )
            .border(
                1.dp,
                Color(0x55F2B01E),
                RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = main,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (emphasize) 22.sp else 14.sp
                ),
                color = if (emphasize) HoneyDeep else Cocoa,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = CocoaFaint,
                maxLines = 1
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  滑杆区块                                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun SliderSection(
    title: String,
    valueLabel: String,
    desc: String,
    emoji: String,
    accent: Color,
    value: Int,
    steps: Int,
    onValueChange: (Int) -> Unit,
    activeColors: List<Color>,
    thumbTint: Color,
    trackBrush: Brush? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                color = CocoaFaint
            )
            Spacer(Modifier.width(8.dp))

            // 内容不做动画：emoji、数值、文案都是直接切换
            Text(
                text = emoji,
                fontSize = 17.sp
            )
            Spacer(Modifier.width(7.dp))

            Text(
                text = valueLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                color = accent,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "${value + 1}/$steps",
                style = MaterialTheme.typography.labelSmall,
                color = CocoaFaint
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = CocoaSoft,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(2.dp))

        GestureSlider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            steps = steps,
            trackBrush = trackBrush,
            activeColors = activeColors,
            thumbTint = thumbTint,
            modifier = Modifier.fillMaxWidth()
        )

        // 刻度标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0 until steps) {
                Text(
                    text = "${i + 1}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (i == value) accent else CocoaFaint.copy(alpha = 0.6f),
                    fontWeight = if (i == value) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SaveButton(isEditing: Boolean, onSave: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
        label = "saveScale"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .height(56.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFFFD54F), Honey, HoneyDeep)
                )
            )
            .border(1.5.dp, Color(0x77FFFFFF), RoundedCornerShape(24.dp))
            .clickable {
                pressed = true
                onSave()
                pressed = false
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(21.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isEditing) "保存修改" else "记下了",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                color = Color.White
            )
        }
    }
}
