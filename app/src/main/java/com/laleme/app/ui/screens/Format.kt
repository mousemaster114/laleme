package com.laleme.app.ui.screens

import java.util.Calendar

/** 周几名称，索引同 Calendar.DAY_OF_WEEK - 1 */
internal val weekNames = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

/** "08:15" */
fun formatFullTime(ts: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    return "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
}

/** "8月5日" */
fun formatDate(ts: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    return "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
}

/** "2025年8月5日" */
fun formatFullDate(ts: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    return "%d年%d月%d日".format(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

/** 当天 00:00 的时间戳 */
fun startOfDay(ts: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = ts
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
