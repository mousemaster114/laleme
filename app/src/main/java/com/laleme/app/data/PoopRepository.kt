package com.laleme.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * 写入结果：把「成功 / 失败」显式暴露给 UI，
 * 避免以前那种「协程里抛异常、界面上什么反应都没有」的静默失败。
 */
sealed interface SaveResult {
    data class Success(val rowId: Long) : SaveResult
    data class Failure(val message: String) : SaveResult
}

class PoopRepository(private val dao: PoopDao) {

    /**
     * 数据库读取失败时不要让整个 Flow 静默死掉（否则界面会一直停在旧数据，
     * 看起来就像「怎么加都只有一条」）。这里兜住异常并退化成空列表。
     */
    fun observeAll(): Flow<List<PoopEntry>> = dao.observeAll()
        .catch { emit(emptyList()) }

    fun observeRange(from: Long, to: Long): Flow<List<PoopEntry>> = dao.observeRange(from, to)
        .catch { emit(emptyList()) }

    /** 显式返回插入结果，失败时带上原因 */
    suspend fun add(entry: PoopEntry): SaveResult = try {
        val id = dao.insert(entry)
        if (id > 0L) SaveResult.Success(id)
        else SaveResult.Failure("数据库返回了无效的行号：$id")
    } catch (t: Throwable) {
        SaveResult.Failure(t.message ?: t.javaClass.simpleName)
    }

    suspend fun update(entry: PoopEntry): SaveResult = try {
        dao.update(entry)
        SaveResult.Success(entry.id)
    } catch (t: Throwable) {
        SaveResult.Failure(t.message ?: t.javaClass.simpleName)
    }

    suspend fun delete(entry: PoopEntry) = dao.delete(entry)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun allOnce(): List<PoopEntry> = dao.getAllOnce()
}
