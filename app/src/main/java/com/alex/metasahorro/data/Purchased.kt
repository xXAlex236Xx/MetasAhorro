package com.alex.metasahorro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Registro histórico de una meta ya comprada (vitrina de "logros"). */
@Entity(tableName = "purchased")
data class Purchased(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imageUri: String? = null,
    val goal: Double,
    val boughtAtMillis: Long
)
