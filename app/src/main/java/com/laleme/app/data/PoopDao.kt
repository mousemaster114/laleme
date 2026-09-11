package com.laleme.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PoopDao {

    /** 主界面：按时间倒序（最新的在最上面） */
    @Query("SELECT * FROM poop_entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PoopEntry>>

    /** 总结页：按时间正序取区间数据 */
    @Query("SELECT * FROM poop_entries WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    fun observeRange(from: Long, to: Long): Flow<List<PoopEntry>>

    @Query("SELECT * FROM poop_entries WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    suspend fun getRange(from: Long, to: Long): List<PoopEntry>

    @Query("SELECT * FROM poop_entries ORDER BY timestamp ASC")
    suspend fun getAllOnce(): List<PoopEntry>

    @Insert
    suspend fun insert(entry: PoopEntry): Long

    @Update
    suspend fun update(entry: PoopEntry)

    @Delete
    suspend fun delete(entry: PoopEntry)

    @Query("DELETE FROM poop_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
