package com.laleme.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 用真实的 Room + SQLite 验证「能不能连续添加多次记录」。
 * 之前的怀疑点是数据库层，这里直接把它跑起来看结果。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PoopDatabaseTest {

    private lateinit var db: PoopDatabase
    private lateinit var dao: PoopDao

    private fun entry(minuteOffset: Long, shape: Int = 2, color: Int = 1, residue: Int = 1) =
        PoopEntry(
            timestamp = 1_700_000_000_000L + minuteOffset * 60_000L,
            shape = shape,
            color = color,
            residue = residue
        )

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, PoopDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.poopDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `连续插入多条都能读出来`() = runTest {
        repeat(5) { i ->
            val id = dao.insert(entry(i.toLong()))
            assertTrue("第 $i 次插入应返回自增主键 > 0，实际 $id", id > 0L)
        }

        val all = dao.getAllOnce()
        assertEquals("应该有 5 条记录", 5, all.size)
        assertEquals("主键应各不相同", 5, all.map { it.id }.distinct().size)
    }

    @Test
    fun `同一分钟内多次记录不会被覆盖`() = runTest {
        // 秒/毫秒归零后，同一分钟的时间戳完全相同
        val sameMinute = 1_700_000_000_000L / 60_000L * 60_000L
        repeat(3) {
            dao.insert(PoopEntry(timestamp = sameMinute, shape = 2, color = 1, residue = 1))
        }
        val all = dao.getAllOnce()
        assertEquals("同一分钟也应保留 3 条", 3, all.size)
        assertTrue(all.all { it.timestamp == sameMinute })
    }

    @Test
    fun `observeAll 是实时的_每次插入都会推送新列表`() = runTest {
        assertEquals(0, dao.observeAll().first().size)

        dao.insert(entry(0))
        assertEquals("第一条", 1, dao.observeAll().first().size)

        dao.insert(entry(1))
        assertEquals("第二条", 2, dao.observeAll().first().size)

        dao.insert(entry(2))
        val list = dao.observeAll().first()
        assertEquals("第三条", 3, list.size)
        // 倒序：最新的在最前面
        assertEquals(list.sortedByDescending { it.timestamp }.map { it.id }, list.map { it.id })
    }

    @Test
    fun `笔记字段与枚举往返正确`() = runTest {
        dao.insert(
            PoopEntry(
                timestamp = 1_700_000_000_000L,
                shape = PoopShape.LIQUID.value,
                color = PoopColor.PALE.value,
                residue = PoopResidue.ENDLESS.value,
                note = "测试备注"
            )
        )
        val e = dao.getAllOnce().single()
        assertEquals(PoopShape.LIQUID, e.shapeEnum)
        assertEquals(PoopColor.PALE, e.colorEnum)
        assertEquals(PoopResidue.ENDLESS, e.residueEnum)
        assertEquals("测试备注", e.note)
    }

    @Test
    fun `更新与删除只影响目标行`() = runTest {
        val id1 = dao.insert(entry(0))
        val id2 = dao.insert(entry(1))
        val id3 = dao.insert(entry(2))

        val target = dao.getAllOnce().first { it.id == id2 }
        dao.update(target.copy(shape = PoopShape.VERY_HARD.value))
        assertEquals(
            PoopShape.VERY_HARD,
            dao.getAllOnce().first { it.id == id2 }.shapeEnum
        )
        assertEquals(3, dao.getAllOnce().size)

        dao.deleteById(id1)
        val left = dao.getAllOnce()
        assertEquals(2, left.size)
        assertTrue(left.none { it.id == id1 })
        assertTrue(left.any { it.id == id3 })
    }

    @Test
    fun `区间查询按时间正序返回`() = runTest {
        for (i in 0 until 10) dao.insert(entry(i.toLong()))
        val base = 1_700_000_000_000L
        val range = dao.getRange(base + 2 * 60_000L, base + 5 * 60_000L)
        assertEquals(4, range.size)
        assertEquals(range.sortedBy { it.timestamp }.map { it.id }, range.map { it.id })
    }
}
