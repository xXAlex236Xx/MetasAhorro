package com.alex.metasahorro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una meta de ahorro activa (algo que Alex quiere comprar).
 * "position" define el orden manual en la lista (se puede reordenar con las flechas).
 * "deadlineMillis" es opcional: fecha límite autoimpuesta.
 */
@Entity(tableName = "metas")
data class Meta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imageUri: String? = null,
    val goal: Double,
    val deadlineMillis: Long? = null,
    val position: Int = 0
)
