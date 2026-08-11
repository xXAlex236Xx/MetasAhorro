package com.alex.metasahorro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila única (id siempre 0) con el estado del "pozo" de dinero: cuánto
 * efectivo y cuánto digital hay guardado en total. No se reparte entre
 * metas: cada meta compara su precio contra (cash + digital).
 */
@Entity(tableName = "pool_state")
data class PoolState(
    @PrimaryKey val id: Int = 0,
    val cash: Double = 0.0,
    val digital: Double = 0.0
)
