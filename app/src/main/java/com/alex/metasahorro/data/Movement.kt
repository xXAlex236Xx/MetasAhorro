package com.alex.metasahorro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tipo de movimiento del historial: depósito, retiro, compra de una meta o conversión efectivo/digital. */
enum class MovementKind { DEPOSIT, WITHDRAW, PURCHASE, TRANSFORM }

/** De qué forma está guardado el dinero. */
enum class MoneyType { EFECTIVO, DIGITAL }

/**
 * Una línea del historial. `moneyType` solo aplica a depósitos y retiros; `label` guarda el
 * nombre de la meta (compras) o el sentido de la conversión (transformaciones).
 */
@Entity(tableName = "movements")
data class Movement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: MovementKind,
    val amount: Double,
    val moneyType: MoneyType? = null,
    val label: String? = null,
    val tsMillis: Long
)