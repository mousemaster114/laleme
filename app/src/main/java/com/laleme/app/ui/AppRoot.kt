package com.laleme.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laleme.app.data.HealthScore
import com.laleme.app.data.PoopEntry
import com.laleme.app.ui.components.AnimatedBlobBackground
import com.laleme.app.ui.components.FeedbackPill
import com.laleme.app.ui.screens.AddSheet
import com.laleme.app.ui.screens.GlassTopBar
import com.laleme.app.ui.screens.PoopList
import com.laleme.app.ui.screens.StatsStrip
import com.laleme.app.ui.screens.SummaryScreen
import com.laleme.app.ui.screens.startOfDay
import java.util.Calendar

enum class Screen { HOME, SUMMARY }

/**
 * 弹层状态。用一个密封类代替「visible + editing」两个独立变量：
 * 二者一旦不同步就会出现「加完第一条之后再也加不上」这类问题。
 * 新增时带上自增 key，保证每次打开表单都被重新初始化。
 */
sealed interface SheetState {
    data object Hidden : SheetState
    data class New(val key: Long) : SheetState
    data class Edit(val entry: PoopEntry) : SheetState
}

@Composable
fun AppRoot(vm: PoopViewModel = viewModel()) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var sheet by remember { mutableStateOf<SheetState>(SheetState.Hidden) }
    var newKey by remember { mutableLongStateOf(0L) }

    val entries by vm.entries.collectAsStateWithLifecycle()
    val feedback by vm.feedback.collectAsStateWithLifecycle()

    // 反馈条显示 2.2 秒后自动消失
    LaunchedEffect(feedback?.stamp) {
        if (feedback != null) {
            kotlinx.coroutines.delay(2200)
            vm.dismissFeedback()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 动态毛玻璃背景
        AnimatedBlobBackground()

        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                if (targetState == Screen.SUMMARY) {
                    (slideInHorizontally(tween(420)) { it / 3 } + fadeIn(tween(340))) togetherWith
                        (slideOutHorizontally(tween(380)) { -it / 8 } + fadeOut(tween(260)))
                } else {
                    (slideInHorizontally(tween(420)) { -it / 3 } + fadeIn(tween(340))) togetherWith
                        (slideOutHorizontally(tween(380)) { it / 8 } + fadeOut(tween(260)))
                }
            },
            label = "screen"
        ) { target ->
            when (target) {
                Screen.HOME -> HomeScreen(
                    entries = entries,
                    onSummary = { screen = Screen.SUMMARY },
                    onAdd = {
                        newKey += 1
                        sheet = SheetState.New(newKey)
                    },
                    onEdit = { e -> sheet = SheetState.Edit(e) },
                    onDelete = { vm.delete(it) },
                    // 弹层出现时把 ＋ 收起来，形成「按钮让位给面板」的过渡
                    fabHidden = sheet !is SheetState.Hidden
                )

                Screen.SUMMARY -> SummaryScreen(
                    vm = vm,
                    onBack = { screen = Screen.HOME }
                )
            }
        }

        // 保存 / 删除结果的反馈条：贴在屏幕底部，且只占自身大小，绝不遮挡顶部按钮
        FeedbackPill(
            text = feedback?.let { fb ->
                if (fb.success && fb.showCount) "已记录第 ${entries.size} 次" else fb.text
            }.orEmpty(),
            success = feedback?.success ?: true,
            visible = feedback != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .wrapContentSize(align = Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 26.dp)
        )

        // 添加 / 编辑弹层
        AddSheet(
            state = sheet,
            onDismiss = { sheet = SheetState.Hidden },
            onSave = { ts, shape, color, residue ->
                when (val s = sheet) {
                    is SheetState.Edit -> vm.update(
                        s.entry.copy(
                            timestamp = ts,
                            shape = shape,
                            color = color,
                            residue = residue
                        )
                    )

                    is SheetState.New -> vm.add(ts, shape, color, residue)

                    SheetState.Hidden -> Unit
                }
                sheet = SheetState.Hidden
            }
        )
    }
}

@Composable
private fun HomeScreen(
    entries: List<PoopEntry>,
    onSummary: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (PoopEntry) -> Unit,
    onDelete: (PoopEntry) -> Unit,
    fabHidden: Boolean = false
) {
    val now = System.currentTimeMillis()
    val todayStart = remember { startOfDay(now) }
    val weekStart = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        // 周一为一周的开始
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val diff = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -diff)
        cal.timeInMillis
    }

    val todayCount = entries.count { it.timestamp >= todayStart }
    val weekCount = entries.count { it.timestamp >= weekStart }
    val avgScore = if (entries.isEmpty()) 0 else entries.sumOf { HealthScore.score(it) } / entries.size

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.statusBarsPadding().height(6.dp))

        GlassTopBar(
            title = "拉了吗",
            subtitle = if (entries.isEmpty()) "记录你的每一次顺畅" else "已记录 ${entries.size} 次 · 点 + 继续",
            onSummary = onSummary,
            onAdd = onAdd,
            fabHidden = fabHidden
        )

        Spacer(Modifier.height(14.dp))

        StatsStrip(
            total = entries.size,
            today = todayCount,
            week = weekCount,
            avgScore = avgScore,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        )

        Spacer(Modifier.height(10.dp))

        PoopList(
            entries = entries,
            onEdit = onEdit,
            onDelete = onDelete,
            onAdd = onAdd,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}
