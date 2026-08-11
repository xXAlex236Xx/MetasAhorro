package com.alex.metasahorro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MovementKind { DEPOSIT, WITHDRAW, PURCHASE }
enum class MoneyType { EFECTIVO, DIGITAL }

/** Un movimiento del historial: registrar plata, restar plata, o comprar una meta. */
@Entity(tableName = "movements")
data class Movement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: MovementKind,
    val amount: Double,
    val moneyType: MoneyType? = null,
    val label: String? = null,
    val tsMillis: Long
)
