package com.laleme.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.laleme.app.data.PoopDatabase
import com.laleme.app.data.PoopEntry
import com.laleme.app.data.PoopRepository
import com.laleme.app.data.PoopSummary
import com.laleme.app.data.SaveResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/** 保存后的反馈，用于弹出的提示条 */
data class SaveFeedback(
    val success: Boolean,
    val text: String,
    /** true 时由界面用「当前实际条数」动态拼文案，避免这里读到过期数字 */
    val showCount: Boolean = false,
    /** 用于让同样的内容也能重新触发一次动画 */
    val stamp: Long = System.currentTimeMillis()
)

class PoopViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PoopRepository(PoopDatabase.get(app).poopDao())

    /** 主界面数据：按时间倒序 */
    val entries: StateFlow<List<PoopEntry>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _range = MutableStateFlow(defaultRange())
    val range: StateFlow<LongRange> = _range.asStateFlow()

    private val _feedback = MutableStateFlow<SaveFeedback?>(null)
    val feedback: StateFlow<SaveFeedback?> = _feedback.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val summary: StateFlow<PoopSummary> = _range
        .flatMapLatest { r -> repo.observeRange(r.first, r.last) }
        .map { PoopSummary(_range.value.first, _range.value.last, it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PoopSummary(_range.value.first, _range.value.last, emptyList())
        )

    fun dismissFeedback() {
        _feedback.value = null
    }

    fun setRange(from: Long, to: Long) {
        _range.value = from..to
    }

    /**
     * 近 N 天：**今天算第 1 天**，往前推 N-1 天。
     * 所以「近 7 天」= 今天 + 前 6 天，共 7 个自然日。
     */
    fun setPresetDays(days: Int) {
        val n = days.coerceAtLeast(1)
        val start = startOfToday() - (n - 1L) * DAY
        setRange(start, endOfToday())
    }

    /**
     * 全部：**从有记录的那一天开始**，而不是从时间戳 0（1970 年）开始，
     * 否则「全部」的天数会显示成几万天。
     */
    fun setAllTime() {
        val earliest = entries.value.minOfOrNull { it.timestamp }
        val start = if (earliest != null) startOfDayOf(earliest) else startOfToday()
        setRange(start, endOfToday())
    }

    /** 本月 1 号 00:00 至今 */
    fun setThisMonth() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        setRange(cal.timeInMillis, endOfToday())
    }

    /** 今年 1 月 1 日 00:00 至今 */
    fun setThisYear() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        setRange(cal.timeInMillis, endOfToday())
    }

    /**
     * 新增记录。写入结果会通过 [feedback] 反馈到界面，
     * 失败时不再静默 —— 这样「加不上」能立刻看到原因。
     */
    fun add(
        timestamp: Long,
        shape: Int,
        color: Int,
        residue: Int,
        note: String = ""
    ) {
        viewModelScope.launch {
            val entry = PoopEntry(
                timestamp = timestamp,
                shape = shape,
                color = color,
                residue = residue,
                note = note
            )
            when (val r = repo.add(entry)) {
                is SaveResult.Success -> {
                    // 文案里的条数交给界面用最新列表长度去拼，避免这里读到旧值
                    _feedback.value = SaveFeedback(true, "已记录", showCount = true)
                }

                is SaveResult.Failure -> {
                    _feedback.value = SaveFeedback(false, "保存失败：${r.message}")
                }
            }
        }
    }

    fun update(entry: PoopEntry) {
        viewModelScope.launch {
            when (val r = repo.update(entry)) {
                is SaveResult.Success -> _feedback.value = SaveFeedback(true, "已保存修改")
                is SaveResult.Failure -> _feedback.value = SaveFeedback(false, "保存失败：${r.message}")
            }
        }
    }

    /** 删除并返回被删的记录，便于「撤销」 */
    fun delete(entry: PoopEntry) {
        viewModelScope.launch {
            runCatching { repo.delete(entry) }
                .onFailure { _feedback.value = SaveFeedback(false, "删除失败：${it.message}") }
        }
    }

    /** 撤销删除：重新插回同一条数据 */
    fun restore(entry: PoopEntry) {
        viewModelScope.launch {
            repo.add(entry.copy(id = 0L))
        }
    }

    private fun startOfToday(): Long = startOfDayOf(System.currentTimeMillis())

    /** 任意时间戳所在那一天的 00:00 */
    private fun startOfDayOf(ts: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ts
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun endOfToday(): Long = startOfToday() + DAY - 1

    companion object {
        const val DAY = 24L * 60 * 60 * 1000

        /** 默认显示近 7 天（今天 + 前 6 天） */
        private fun defaultRange(): LongRange {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val todayStart = cal.timeInMillis
            return (todayStart - 6L * DAY)..(todayStart + DAY - 1)
        }
    }
}
