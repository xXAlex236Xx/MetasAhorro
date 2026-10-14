package com.alex.metasahorro.repository

import androidx.room.withTransaction
import com.alex.metasahorro.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Largo máximo de una categoría, para que quepa bien en las tarjetas. */
const val MAX_CATEGORY_LENGTH = 24

/** Deja la categoría lista para guardar: sin espacios sobrantes, cortada al máximo, y null si quedó vacía. */
fun normalizeCategory(raw: String?): String? = raw?.trim()?.take(MAX_CATEGORY_LENGTH)?.ifBlank { null }

/**
 * Cómo deshacer una operación ya hecha. Lo devuelve cada operación del repositorio y se le pasa a
 * [MetasRepository.undo]; guarda lo mínimo para dejar los datos como estaban.
 */
sealed interface UndoAction {
    /** Vuelve a crear una meta borrada (con su mismo id y posición). */
    data class RestoreMeta(val meta: Meta) : UndoAction

    /** Revierte un depósito, retiro o transformación y lo saca del historial. */
    data class RevertMovement(val movementId: Long) : UndoAction

    /** Devuelve una compra: repone la meta, devuelve el dinero al pozo y quita la compra y su movimiento. */
    data class RevertPurchase(
        val meta: Meta,
        val purchasedId: Long,
        val movementId: Long,
        val cash: Double,
        val digital: Double
    ) : UndoAction
}

/**
 * Todas las operaciones que tocan el pozo corren dentro de una transacción de Room y
 * leen el pozo actual de la base (no un valor viejo de la UI), así dos acciones rápidas
 * seguidas no se pisan y un fallo a medias no deja datos inconsistentes.
 */
class MetasRepository(private val db: AppDatabase) {

    private val dao = db.dao()

    val metas: Flow<List<Meta>> = dao.observeMetas()
    val purchased: Flow<List<Purchased>> = dao.observePurchased()
    val movements: Flow<List<Movement>> = dao.observeMovements()
    val pool: Flow<PoolState> = dao.observePool().map { it ?: PoolState() }
    val achievements: Flow<List<AchievementUnlock>> = dao.observeAchievements()

    /** Crea una meta nueva al final de la lista (la posición se calcula como la última + 1). */
    suspend fun addMeta(name: String, imageUri: String?, goal: Double, deadlineMillis: Long?, category: String? = null) =
        db.withTransaction {
            val position = dao.nextPosition()
            dao.insertMeta(
                Meta(
                    name = name, imageUri = imageUri, goal = goal, deadlineMillis = deadlineMillis,
                    position = position, category = normalizeCategory(category)
                )
            )
        }

    /** Guarda los cambios de una meta existente (nombre, precio, foto, fecha límite o categoría). */
    suspend fun updateMeta(meta: Meta) = dao.updateMeta(meta.copy(category = normalizeCategory(meta.category)))

    /** Borra una meta. No toca el pozo ni el historial. Devuelve cómo deshacerlo. */
    suspend fun deleteMeta(meta: Meta): UndoAction {
        dao.deleteMeta(meta)
        return UndoAction.RestoreMeta(meta)
    }

    /** Intercambia la posición de una meta con su vecina (`direction` = -1 sube, +1 baja). */
    suspend fun moveMeta(current: List<Meta>, meta: Meta, direction: Int) = db.withTransaction {
        val idx = current.indexOf(meta)
        val newIdx = idx + direction
        if (idx < 0 || newIdx < 0 || newIdx >= current.size) return@withTransaction
        val other = current[newIdx]
        dao.updateMeta(meta.copy(position = other.position))
        dao.updateMeta(other.copy(position = meta.position))
    }

    /** Suma dinero al pozo (efectivo o digital) y lo anota como `DEPOSIT` en el historial. Cuenta para la racha. Null si el monto no es válido. */
    suspend fun registrar(amount: Double, type: MoneyType): UndoAction? {
        if (!amount.isValidAmount()) return null
        return db.withTransaction {
            val pool = dao.getPool() ?: PoolState()
            val updated = when (type) {
                MoneyType.EFECTIVO -> pool.copy(cash = round2(pool.cash + amount))
                MoneyType.DIGITAL -> pool.copy(digital = round2(pool.digital + amount))
            }
            dao.upsertPool(updated)
            val id = dao.insertMovement(Movement(kind = MovementKind.DEPOSIT, amount = amount, moneyType = type, tsMillis = System.currentTimeMillis()))
            UndoAction.RevertMovement(id)
        }
    }

    /** Descuenta dinero del tipo indicado y lo anota como `WITHDRAW`. Devuelve null si no hay saldo suficiente. */
    suspend fun restar(amount: Double, type: MoneyType): UndoAction? {
        if (!amount.isValidAmount()) return null
        return db.withTransaction {
            val pool = dao.getPool() ?: PoolState()
            val disponible = if (type == MoneyType.EFECTIVO) pool.cash else pool.digital
            if (amount > disponible) return@withTransaction null
            val updated = when (type) {
                MoneyType.EFECTIVO -> pool.copy(cash = round2(pool.cash - amount))
                MoneyType.DIGITAL -> pool.copy(digital = round2(pool.digital - amount))
            }
            dao.upsertPool(updated)
            val id = dao.insertMovement(Movement(kind = MovementKind.WITHDRAW, amount = amount, moneyType = type, tsMillis = System.currentTimeMillis()))
            UndoAction.RevertMovement(id)
        }
    }

    /**
     * Convierte dinero de efectivo a digital (o al revés) sin cambiar el total. Anota un `TRANSFORM`
     * (con el tipo de origen en `moneyType`). Devuelve null si no hay saldo.
     */
    suspend fun transformar(amount: Double, from: MoneyType): UndoAction? {
        if (!amount.isValidAmount()) return null
        return db.withTransaction {
            val pool = dao.getPool() ?: PoolState()
            val disponible = if (from == MoneyType.EFECTIVO) pool.cash else pool.digital
            if (amount > disponible) return@withTransaction null
            val updated = if (from == MoneyType.EFECTIVO) {
                pool.copy(cash = round2(pool.cash - amount), digital = round2(pool.digital + amount))
            } else {
                pool.copy(digital = round2(pool.digital - amount), cash = round2(pool.cash + amount))
            }
            dao.upsertPool(updated)
            val label = if (from == MoneyType.EFECTIVO) "Efectivo → Digital" else "Digital → Efectivo"
            val id = dao.insertMovement(
                Movement(kind = MovementKind.TRANSFORM, amount = amount, moneyType = from, label = label, tsMillis = System.currentTimeMillis())
            )
            UndoAction.RevertMovement(id)
        }
    }

    /**
     * Marca una meta como comprada: descuenta su precio del pozo (repartido en proporción
     * efectivo/digital), la archiva en `purchased` (con su categoría), anota un `PURCHASE` y la borra de las activas.
     * Devuelve null (sin tocar nada) si el pozo no alcanza para el precio de la meta.
     */
    suspend fun marcarComprado(meta: Meta): UndoAction? = db.withTransaction {
        val pool = dao.getPool() ?: PoolState()
        if (pool.cash + pool.digital < meta.goal - 0.01) return@withTransaction null

        val (subtractCash, subtractDigital) = splitPurchase(pool.cash, pool.digital, meta.goal)
        dao.upsertPool(pool.copy(cash = round2(pool.cash - subtractCash), digital = round2(pool.digital - subtractDigital)))

        val now = System.currentTimeMillis()
        val purchasedId = dao.insertPurchased(
            Purchased(name = meta.name, imageUri = meta.imageUri, goal = meta.goal, boughtAtMillis = now, category = meta.category)
        )
        val movementId = dao.insertMovement(
            Movement(kind = MovementKind.PURCHASE, amount = meta.goal, moneyType = null, label = meta.name, tsMillis = now)
        )
        dao.deleteMeta(meta)
        UndoAction.RevertPurchase(meta, purchasedId, movementId, subtractCash, subtractDigital)
    }

    /** Deshace una operación. Devuelve false si ya no se puede (por ejemplo, el dinero que habría que quitar ya se gastó). */
    suspend fun undo(action: UndoAction): Boolean = when (action) {
        is UndoAction.RestoreMeta -> {
            dao.insertMeta(action.meta)
            true
        }
        is UndoAction.RevertMovement -> revertMovement(action.movementId)
        is UndoAction.RevertPurchase -> db.withTransaction {
            val pool = dao.getPool() ?: PoolState()
            dao.upsertPool(pool.copy(cash = round2(pool.cash + action.cash), digital = round2(pool.digital + action.digital)))
            dao.insertMeta(action.meta)
            dao.deletePurchased(action.purchasedId)
            dao.deleteMovement(action.movementId)
            true
        }
    }

    /**
     * Borra un depósito, retiro o transformación del historial y revierte su efecto en el pozo. Falla (false)
     * si es una compra, si no existe, o si revertirlo dejaría un saldo negativo.
     */
    suspend fun revertMovement(movementId: Long): Boolean = db.withTransaction {
        val m = dao.getMovement(movementId) ?: return@withTransaction false
        val pool = dao.getPool() ?: PoolState()
        val updated: PoolState = when (m.kind) {
            MovementKind.DEPOSIT -> {
                val type = m.moneyType ?: return@withTransaction false
                val available = if (type == MoneyType.EFECTIVO) pool.cash else pool.digital
                if (m.amount > available + 0.01) return@withTransaction false
                if (type == MoneyType.EFECTIVO) pool.copy(cash = round2(pool.cash - m.amount))
                else pool.copy(digital = round2(pool.digital - m.amount))
            }
            MovementKind.WITHDRAW -> {
                val type = m.moneyType ?: return@withTransaction false
                if (type == MoneyType.EFECTIVO) pool.copy(cash = round2(pool.cash + m.amount))
                else pool.copy(digital = round2(pool.digital + m.amount))
            }
            MovementKind.TRANSFORM -> {
                // Los movimientos antiguos no guardaban el origen: se deduce del texto "Efectivo → Digital".
                val from = m.moneyType ?: if (m.label?.startsWith("Efectivo") == true) MoneyType.EFECTIVO else MoneyType.DIGITAL
                val destination = if (from == MoneyType.EFECTIVO) pool.digital else pool.cash
                if (m.amount > destination + 0.01) return@withTransaction false
                if (from == MoneyType.EFECTIVO) pool.copy(cash = round2(pool.cash + m.amount), digital = round2(pool.digital - m.amount))
                else pool.copy(digital = round2(pool.digital + m.amount), cash = round2(pool.cash - m.amount))
            }
            MovementKind.PURCHASE -> return@withTransaction false
        }
        dao.upsertPool(updated)
        dao.deleteMovement(movementId)
        true
    }

    /** Guarda los logros recién alcanzados. Devuelve solo los que de verdad eran nuevos (los repetidos se ignoran). */
    suspend fun unlockAchievements(reached: Collection<Achievement>): List<Achievement> {
        val now = System.currentTimeMillis()
        return reached.filter { dao.insertAchievement(AchievementUnlock(it.name, now)) != -1L }
    }

    /** Cuánto lleva ahorrado una meta: el total del pozo, con tope en el precio de la meta (no se reparte entre metas). */
    fun savedFor(meta: Meta, pool: PoolState): Double {
        val total = pool.cash + pool.digital
        return minOf(total, meta.goal)
    }

    private fun Double.isValidAmount() = !isNaN() && !isInfinite() && this > 0
}
