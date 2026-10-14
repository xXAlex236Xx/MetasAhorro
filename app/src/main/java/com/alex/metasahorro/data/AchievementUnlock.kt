package com.alex.metasahorro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un logro ya desbloqueado. `id` es el nombre del enum `Achievement`; guardar cuándo se obtuvo
 * evita celebrarlo dos veces y permite mostrar la fecha en la vitrina de logros.
 */
@Entity(tableName = "achievements")
data class AchievementUnlock(
    @PrimaryKey val id: String,
    val unlockedAtMillis: Long
)
