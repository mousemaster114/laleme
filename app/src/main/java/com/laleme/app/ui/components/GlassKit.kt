package com.laleme.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laleme.app.ui.theme.Amber
import com.laleme.app.ui.theme.Cocoa
import com.laleme.app.ui.theme.CocoaFaint
import com.laleme.app.ui.theme.CocoaSoft
import com.laleme.app.ui.theme.Cream
import com.laleme.app.ui.theme.CreamDeep
import com.laleme.app.ui.theme.Custard
import com.laleme.app.ui.theme.GlassStroke
import com.laleme.app.ui.theme.GlassWhite
import com.laleme.app.ui.theme.Honey
import com.laleme.app.ui.theme.HoneyDeep
import kotlin.math.cos
import kotlin.math.sin

/* ------------------------------------------------------------------ */
/*  背景：暖黄渐变 + 缓慢游走的模糊光斑                                 */
/* ------------------------------------------------------------------ */

/** 光斑的参数，背景和卡片背板共用同一套，保证色调一致 */
internal data class Blob(
    val baseX: Float,
    val baseY: Float,
    val radius: Float,
    val driftX: Float,
    val driftY: Float,
    val phase: Float,
    val speed: Float,
    val color: Color,
    val alpha: Float
)

internal val backgroundBlobs = listOf(
    Blob(0.10f, 0.08f, 0.58f, 0.12f, 0.08f, 0.0f, 1.0f, Color(0xFFFFD54F), 0.95f),
    Blob(0.94f, 0.22f, 0.54f, 0.09f, 0.10f, 1.7f, 1.3f, Color(0xFFFFB74D), 0.70f),
    Blob(0.22f, 0.94f, 0.64f, 0.12f, 0.07f, 3.1f, 0.85f, Color(0xFFFFF176), 0.62f),
    Blob(0.82f, 0.80f, 0.48f, 0.10f, 0.11f, 4.4f, 1.1f, Color(0xFFFFE082), 0.74f),
    Blob(0.50f, 0.46f, 0.40f, 0.14f, 0.12f, 2.2f, 0.70f, Color(0xFFFFCC80), 0.44f)
)

/**
 * 全局背景。除了渐变和光斑，再叠一层极缓慢旋转的锥形渐变，
 * 让整块背景始终有细微的“活”感。
 */
@Composable
fun AnimatedBlobBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bg")

    val pulses = backgroundBlobs.mapIndexed { i, _ ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (20000 / backgroundBlobs[i].speed).toInt().coerceAtLeast(7000),
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "blob$i"
        )
    }

    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // 锥形渐变缓慢自转，给纯色区域加一点极微妙的色相流动
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // 1. 奶油渐变打底
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Cream, Color(0xFFFFF1CC), CreamDeep, Color(0xFFFFF6DE)),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
        )

        // 2. 缓慢自转的锥形渐变，让纯色区域也有极微妙的色彩流动
        rotate(spin) {
            drawRect(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0x14FFC93C),
                        Color(0x08FFFFFF),
                        Color(0x14FFB74D),
                        Color(0x06FFFFFF),
                        Color(0x14FFC93C)
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.4f)
                ),
                topLeft = Offset(-size.maxDimension * 0.5f, -size.maxDimension * 0.5f),
                size = Size(
                    size.width + size.maxDimension,
                    size.height + size.maxDimension
                )
            )
        }

        // 3. 游走的大光斑
        backgroundBlobs.forEachIndexed { i, b ->
            val t = pulses[i].value
            val cx = size.width * (b.baseX + b.driftX * sin(t + b.phase))
            val cy = size.height * (b.baseY + b.driftY * cos(t * 0.8f + b.phase))
            val r = size.minDimension * b.radius * (0.9f + 0.14f * breathe)
            val a = b.alpha * (0.70f + 0.30f * breathe)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        b.color.copy(alpha = a * 0.85f),
                        b.color.copy(alpha = a * 0.28f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  卡片毛玻璃背板                                                      */
/* ------------------------------------------------------------------ */

/**
 * 卡片内部的毛玻璃背板：
 * 先画一层暖色渐变，再画两个真实高斯模糊的大色斑，
 * 最后叠一点高光。这样卡片里是真·模糊内容，而不是单纯的半透明色块。
 *
 * 写成 BoxScope 的扩展是因为需要 matchParentSize()。
 */
@Composable
private fun BoxScope.FrostedBackdrop(
    seed: Int,
    corner: Dp,
    tint: Color,
    blurRadius: Dp
) {
    val transition = rememberInfiniteTransition(label = "frost$seed")

    val phaseA = remember(seed) { (seed * 1.37f) % 6.283f }
    val phaseB = remember(seed) { (seed * 2.71f + 1.9f) % 6.283f }

    val tA by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(17000 + (seed % 5) * 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "frostA"
    )
    val tB by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(21000 + (seed % 4) * 1700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "frostB"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000 + (seed % 3) * 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "frostBreathe"
    )

    val colorA = remember(seed) {
        listOf(
            Color(0xFFFFD54F), Color(0xFFFFB74D), Color(0xFFFFE082),
            Color(0xFFFFCC80), Color(0xFFFFF176)
        )[seed % 5]
    }
    val colorB = remember(seed) {
        listOf(
            Color(0xFFFFE082), Color(0xFFFFF176), Color(0xFFFFCC80),
            Color(0xFFFFD54F), Color(0xFFFFB74D)
        )[(seed + 2) % 5]
    }

    Box(
        modifier = Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(corner))
    ) {
        // 暖色底
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            tint,
                            lerp(tint, Custard, 0.35f),
                            lerp(tint, Amber, 0.22f)
                        )
                    )
                )
        )
        // 真·模糊色斑
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        ) {
            val r1 = size.maxDimension * 0.85f * breathe
            val c1 = Offset(
                size.width * (0.22f + 0.20f * sin(tA + phaseA)),
                size.height * (0.18f + 0.26f * cos(tA * 0.9f + phaseA))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colorA.copy(alpha = 0.85f), colorA.copy(alpha = 0.2f), Color.Transparent),
                    center = c1,
                    radius = r1
                ),
                radius = r1,
                center = c1
            )

            val r2 = size.maxDimension * 0.75f * (1.9f - breathe)
            val c2 = Offset(
                size.width * (0.82f + 0.18f * cos(tB + phaseB)),
                size.height * (0.86f + 0.22f * sin(tB * 0.8f + phaseB))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colorB.copy(alpha = 0.7f), colorB.copy(alpha = 0.16f), Color.Transparent),
                    center = c2,
                    radius = r2
                ),
                radius = r2,
                center = c2
            )
        }
        // 玻璃高光：左上角一团柔光 + 顶部一条细亮线
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    onDrawWithContent {
                        drawContent()
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.34f), Color.Transparent),
                                center = Offset(w * 0.16f, -h * 0.05f),
                                radius = h * 0.85f
                            ),
                            radius = h * 0.85f,
                            center = Offset(w * 0.16f, -h * 0.05f)
                        )
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent)
                            ),
                            start = Offset(w * 0.07f, 1.2f),
                            end = Offset(w * 0.93f, 1.2f),
                            strokeWidth = 2.2f,
                            cap = StrokeCap.Round
                        )
                    }
                }
        )
    }
}

/* ------------------------------------------------------------------ */
/*  毛玻璃卡片                                                          */
/* ------------------------------------------------------------------ */

/**
 * 由卡片自身的染色值推出一个稳定的种子，用来给背板色斑选色和错开动画相位。
 * 不用自增计数器：那样在重组/回收时会漂移，导致同一张卡变色。
 */
private fun seedOf(tint: Color, corner: Dp): Int {
    val c = (tint.value shr 32).toInt()
    return (c * 31 + (corner.value * 7f).toInt()) and 0x7FFFFFFF
}

/**
 * @param blurRadius 背板色斑的高斯模糊半径，0 表示关闭模糊（省电模式）
 * @param sheen 是否周期性地扫过一道高光
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    tint: Color = GlassWhite,
    elevation: Dp = 10.dp,
    blurRadius: Dp = 26.dp,
    sheen: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    val seed = remember(tint, corner) { seedOf(tint, corner) }

    // 周期性的斜向高光扫过
    var sheenProgress by remember { mutableFloatStateOf(-1f) }
    if (sheen) {
        LaunchedEffect(seed) {
            while (true) {
                kotlinx.coroutines.delay(2600L + (seed % 5) * 380L)
                androidx.compose.animation.core.animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(1150, easing = FastOutSlowInEasing)
                ) { value, _ -> sheenProgress = value }
                sheenProgress = -1f
            }
        }
    }

    Box(
        modifier = modifier
            .shadow(
                elevation,
                shape,
                clip = false,
                ambientColor = Color(0x33D9A521),
                spotColor = Color(0x40C9971A)
            )
            .clip(shape)
    ) {
        // 1. 模糊背板
        if (blurRadius > 0.dp) {
            FrostedBackdrop(seed = seed, corner = corner, tint = tint, blurRadius = blurRadius)
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(tint)
            )
        }

        // 2. 半透明染色，保证文字对比度
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            tint.copy(alpha = (tint.alpha * 0.72f).coerceIn(0f, 0.82f)),
                            tint.copy(alpha = (tint.alpha * 0.5f).coerceIn(0f, 0.7f))
                        )
                    )
                )
        )

        // 3. 扫光
        if (sheen && sheenProgress >= 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        drawContent()
                        val w = size.width
                        val bandW = w * 0.34f
                        val cx = -bandW + (w + bandW * 2f) * sheenProgress
                        drawRect(
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.42f),
                                    Color.Transparent
                                ),
                                start = Offset(cx - bandW / 2f, 0f),
                                end = Offset(cx + bandW / 2f, size.height)
                            )
                        )
                    }
            )
        }

        // 4. 渐变描边
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            GlassStroke,
                            Color.White.copy(alpha = 0.14f),
                            Color(0x59FFFFFF),
                            Color.White.copy(alpha = 0.10f)
                        )
                    ),
                    shape = shape
                )
        )

        content()
    }
}

/* ------------------------------------------------------------------ */
/*  渐变文字                                                            */
/* ------------------------------------------------------------------ */

/** 会缓慢流动的渐变文字 */
@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displaySmall,
    colors: List<Color> = listOf(Cocoa, HoneyDeep, Cocoa)
) {
    val transition = rememberInfiniteTransition(label = "gradText")
    val shift by transition.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shift"
    )
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(shift * 260f, 0f),
                end = Offset(shift * 260f + 240f, 90f)
            )
        )
    )
}

/* ------------------------------------------------------------------ */
/*  数字滚动                                                            */
/* ------------------------------------------------------------------ */

@Composable
fun AnimatedInt(
    value: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = Cocoa,
    suffix: String = ""
) {
    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "int"
    )
    Text(
        text = "${animated.toInt()}$suffix",
        modifier = modifier,
        style = style,
        color = color
    )
}

/* ------------------------------------------------------------------ */
/*  自定义滑杆                                                          */
/* ------------------------------------------------------------------ */

@Composable
fun GestureSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    steps: Int,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 16.dp,
    thumbSize: Dp = 36.dp,
    trackBrush: Brush? = null,
    activeColors: List<Color>,
    thumbTint: Color = Honey
) {
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    var widthPx by remember { mutableFloatStateOf(1f) }
    var dragging by remember { mutableFloatStateOf(0f) }

    val denom = (steps - 1).coerceAtLeast(1)
    val fraction = (value / denom).coerceIn(0f, 1f)
    val halfThumbPx = with(LocalDensity.current) { thumbSize.toPx() / 2f }

    // pointerInput 的协程在整段手势里只创建一次，
    // 用 rememberUpdatedState 保证里面读到的永远是最新的回调，避免捕获过期闭包。
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    // 记录上一次真正震动过的档位，只有「跨过节点」时才震一次
    var lastHapticIndex by remember { mutableIntStateOf(Int.MIN_VALUE) }

    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "sliderFrac"
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (dragging > 0f) 1.24f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "thumbScale"
    )
    val thumbColor by animateColorAsState(thumbTint, tween(320), label = "thumbColor")
    // 拖动时滑块外圈发光
    val glow by animateFloatAsState(
        targetValue = if (dragging > 0f) 1f else 0f,
        animationSpec = tween(260),
        label = "glow"
    )

    // 手指位置 -> 档位下标（与绘制保持一致：轨道左右各内缩半个滑块）
    fun indexAt(x: Float): Int {
        val trackWidth = (widthPx - halfThumbPx * 2f).coerceAtLeast(1f)
        val f = ((x - halfThumbPx) / trackWidth).coerceIn(0f, 1f)
        return Math.round(f * denom).coerceIn(0, denom)
    }

    /** 跨过档位节点时震一下。
     *  优先用系统为滑杆停顿专门提供的 SEGMENT_TICK（有干脆的“咔哒”感），
     *  部分机型不支持该常量时退回 Compose 的通用轻震。
     *  不传 FLAG_IGNORE_GLOBAL_SETTING，尊重用户在系统里关掉触感的设置。 */
    fun tick() {
        val ok = view.performHapticFeedback(android.view.HapticFeedbackConstants.SEGMENT_TICK)
        if (!ok) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    /**
     * @param allowHaptic 只有手指按下 / 拖动时才允许震动。
     *   程序化改变 value（例如打开面板初始化）不该触发马达。
     */
    fun emit(x: Float, allowHaptic: Boolean) {
        val snapped = indexAt(x)
        if (allowHaptic && snapped != lastHapticIndex) {
            lastHapticIndex = snapped
            tick()
        }
        currentOnValueChange(snapped.toFloat())
    }

    Box(
        modifier = modifier
            .height(thumbSize + 14.dp)
            .pointerInput(steps) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    dragging = 1f
                    lastHapticIndex = Int.MIN_VALUE
                    emit(down.position.x, allowHaptic = true)

                    var dragged = false
                    val touchSlop = viewConfiguration.touchSlop
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (!dragged && kotlin.math.abs(change.position.x - down.position.x) > touchSlop) {
                            dragged = true
                        }
                        if (dragged) {
                            change.consume()
                            emit(change.position.x, allowHaptic = true)
                        }
                    }
                    dragging = 0f
                    lastHapticIndex = Int.MIN_VALUE
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight + 12.dp)
        ) {
            widthPx = size.width
            val cy = size.height / 2f
            val halfThumb = thumbSize.toPx() / 2f
            // 轨道左右各留出半个滑块，滑块圆心才能在两端精确对齐刻度
            val trackStart = halfThumb
            val trackEnd = (size.width - halfThumb).coerceAtLeast(trackStart + 1f)
            val trackWidth = trackEnd - trackStart
            val th = trackHeight.toPx()
            val radius = CornerRadius(th / 2f)

            // 底轨
            drawRoundRect(
                color = Color(0x22A08A68),
                topLeft = Offset(trackStart, cy - th / 2f),
                size = Size(trackWidth, th),
                cornerRadius = radius
            )

            if (trackBrush != null) {
                // 颜色滑杆：整条轨道就是颜色预览
                drawRoundRect(
                    brush = trackBrush,
                    topLeft = Offset(trackStart, cy - th / 2f),
                    size = Size(trackWidth, th),
                    cornerRadius = radius
                )
            } else {
                // 已填充部分
                val fillW = (trackWidth * animatedFraction).coerceAtLeast(0.001f)
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = activeColors,
                        startX = trackStart,
                        endX = trackEnd
                    ),
                    topLeft = Offset(trackStart, cy - th / 2f),
                    size = Size(fillW, th),
                    cornerRadius = radius
                )
            }

            // 刻度点
            for (i in 0 until steps) {
                val x = trackStart + trackWidth * (i.toFloat() / denom)
                val on = i.toFloat() <= animatedFraction * denom + 0.01f
                drawCircle(
                    color = if (on) Color.White.copy(alpha = 0.8f) else Color(0x33FFFFFF),
                    radius = 2.6f,
                    center = Offset(x, cy)
                )
            }

            // 滑块
            val cx = trackStart + trackWidth * animatedFraction

            // 拖动时外发光
            if (glow > 0.01f) {
                val gr = halfThumb * (1.6f + 0.5f * glow)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(thumbColor.copy(alpha = 0.42f * glow), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = gr
                    ),
                    radius = gr,
                    center = Offset(cx, cy)
                )
            }

            drawCircle(color = Color(0x33000000), radius = halfThumb, center = Offset(cx, cy + 2f))
            drawCircle(color = Color.White, radius = halfThumb, center = Offset(cx, cy))
            drawCircle(
                color = thumbColor,
                radius = halfThumb - 1.5f,
                center = Offset(cx, cy),
                style = Stroke(width = 4.dp.toPx() * thumbScale)
            )
            // 滑块中心的小圆点
            drawCircle(
                color = thumbColor.copy(alpha = 0.85f),
                radius = 3.2f,
                center = Offset(cx, cy)
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  健康指示器：五格能量柱 + 等级                                       */
/* ------------------------------------------------------------------ */

@Composable
fun HealthBars(
    score: Int,
    color: Color,
    modifier: Modifier = Modifier,
    barWidth: Dp = 5.dp,
    maxHeight: Dp = 26.dp
) {
    val animated by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "healthBars"
    )
    val levels = 5

    Row(
        modifier = modifier.height(maxHeight),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0 until levels) {
            val threshold = (i + 1).toFloat() / levels
            val on = animated >= threshold - 0.06f
            val target = if (on) maxHeight * (0.45f + 0.55f * threshold) else maxHeight * 0.28f
            val h by animateFloatAsState(
                targetValue = target.value,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
                label = "bar$i"
            )
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(h.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (on) {
                            Brush.verticalGradient(listOf(lerp(color, Color.White, 0.35f), color))
                        } else {
                            Brush.verticalGradient(listOf(Color(0x1F000000), Color(0x14000000)))
                        }
                    )
            )
        }
    }
}

/** 分数环 */
@Composable
fun ScoreRing(
    score: Int,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    strokeWidth: Dp = 10.dp
) {
    val animated by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "ring"
    )
    val sweep by animateFloatAsState(
        targetValue = animated * 360f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessVeryLow),
        label = "sweep"
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = strokeWidth.toPx()
            val inset = sw / 2f
            drawArc(
                color = Color(0x1A000000),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - sw, this.size.height - sw),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        lerp(color, Color.White, 0.45f),
                        color,
                        lerp(color, Color.Black, 0.12f),
                        lerp(color, Color.White, 0.45f)
                    )
                ),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - sw, this.size.height - sw),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animated.times(100).toInt()}",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                color = Cocoa
            )
            Text("健康分", style = MaterialTheme.typography.labelSmall, color = CocoaFaint)
        }
    }
}

/* ------------------------------------------------------------------ */
/*  小零件                                                              */
/* ------------------------------------------------------------------ */

@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: String? = null
) {
    val bg by animateColorAsState(
        if (selected) Honey else Color(0x66FFFFFF),
        tween(280),
        label = "chipBg"
    )
    val fg by animateColorAsState(
        if (selected) Color.White else CocoaSoft,
        tween(280),
        label = "chipFg"
    )
    val scale by animateFloatAsState(
        if (selected) 1.05f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "chipScale"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                1.dp,
                if (selected) Color.Transparent else Color(0x33FFFFFF),
                RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (leading != null) "$leading $text" else text,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1
        )
    }
}

/** 在两段文字之间插入分隔点 */
@Composable
fun InlineLabelValue(
    label: String,
    value: String,
    valueColor: Color = Cocoa,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = CocoaFaint, fontSize = 10.sp)) { append(label) }
            append(" ")
            withStyle(SpanStyle(color = valueColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)) {
                append(value)
            }
        },
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1
    )
}

/**
 * 入场动效：淡入 + 轻微上移。
 * 刻意不做缩放——列表项缩放会显得廉价，纯位移 + 缓动更耐看。
 */
@Composable
fun StaggerIn(
    index: Int,
    modifier: Modifier = Modifier,
    baseDelayMs: Long = 55L,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index.coerceAtMost(14) * baseDelayMs))
        visible = true
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 640, easing = FastOutSlowInEasing),
        label = "stagger"
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.coerceIn(0f, 1f)
            translationY = (1f - progress) * 44f
        }
    ) {
        content()
    }
}

/**
 * 反馈条：保存成功 / 失败都会出现，带弹性升起 + 自动消失。
 *
 * 注意：它默认放在屏幕**底部**。
 * 之前放在顶部且占满整宽，结果盖住了右上角的「＋」按钮、把点击都吃掉了，
 * 表现就是「加了一条之后就再也加不了」。
 */
@Composable
fun FeedbackPill(
    text: String,
    success: Boolean,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(visible, text) {
        if (visible) {
            progress.snapTo(0f)
            progress.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
        } else {
            progress.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
        }
    }

    if (progress.value <= 0.001f) return

    val accent = if (success) Color(0xFF7CB342) else Color(0xFFE5533D)

    Box(
        // 关键：这个提示条只用来“看”，绝不能挡住下面的按钮。
        // 之前它带 fillMaxWidth 又放在顶部，正好盖住右上角的 ＋，把点击吃掉了。
        modifier = modifier.graphicsLayer {
            // 完全收起时直接从命中测试里移除
            if (progress.value <= 0.001f) alpha = 0f
        },
        contentAlignment = Alignment.BottomCenter
    ) {
        GlassCard(
            corner = 50.dp,
            elevation = 14.dp,
            tint = Color(0xF0FFFDF7),
            blurRadius = 22.dp,
            modifier = Modifier.graphicsLayer {
                val p = progress.value
                alpha = p.coerceIn(0f, 1f)
                translationY = (1f - p) * 90f
                scaleX = 0.88f + 0.12f * p
                scaleY = 0.88f + 0.12f * p
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (success) "✓" else "!",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = Cocoa,
                    maxLines = 1
                )
            }
        }
    }
}

/** 空状态：一颗上下漂浮的屎 */
@Composable
fun FloatingPoop(modifier: Modifier = Modifier, size: Dp = 108.dp) {
    val transition = rememberInfiniteTransition(label = "poop")
    val y by transition.animateFloat(
        initialValue = -9f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    val halo by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo"
    )
    // 轻微左右摇摆，比纯上下浮动更生动
    val tilt by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt"
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(halo)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0x4DF2B01E), Color.Transparent)))
        )
        Text(
            text = "\uD83D\uDCA9",
            fontSize = (size.value * 0.46f).sp,
            modifier = Modifier.graphicsLayer {
                translationY = y
                rotationZ = tilt
            }
        )
    }
}
