package com.laleme.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一次拉屎记录
 *
 * @param timestamp 拉屎时间（毫秒时间戳，精确到分钟由 UI 保证）
 * @param shape 形状 0-4，见 [PoopShape]
 * @param color 颜色 0-5，见 [PoopColor]
 * @param residue 残留 0-4，见 [PoopResidue]
 * @param note 可选备注
 */
@Entity(tableName = "poop_entries")
data class PoopEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,
    val shape: Int,
    val color: Int,
    val residue: Int,
    val note: String = ""
) {
    val shapeEnum: PoopShape get() = PoopShape.of(shape)
    val colorEnum: PoopColor get() = PoopColor.of(color)
    val residueEnum: PoopResidue get() = PoopResidue.of(residue)
}
