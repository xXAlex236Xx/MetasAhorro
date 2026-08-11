package com.alex.metasahorro.repository

import com.alex.metasahorro.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MetasRepository(private val dao: AppDao) {

    val metas: Flow<List<Meta>> = dao.observeMetas()
    val purchased: Flow<List<Purchased>> = dao.observePurchased()
    val movements: Flow<List<Movement>> = dao.observeMovements()
    val pool: Flow<PoolState> = dao.observePool().map { it ?: PoolState() }

    // ---- Metas ----
    suspend fun addMeta(name: String, imageUri: String?, goal: Double, deadlineMillis: Long?) {
        val position = dao.nextPosition()
        dao.insertMeta(Meta(name = name, imageUri = imageUri, goal = goal, deadlineMillis = deadlineMillis, position = position))
    }

    suspend fun updateMeta(meta: Meta) = dao.updateMeta(meta)

    suspend fun deleteMeta(meta: Meta) = dao.deleteMeta(meta)

    /** Mueve una meta un puesto arriba (-1) o abajo (+1) en la lista, intercambiando posiciones. */
    suspend fun moveMeta(current: List<Meta>, meta: Meta, direction: Int) {
        val idx = current.indexOf(meta)
        val newIdx = idx + direction
        if (newIdx < 0 || newIdx >= current.size) return
        val other = current[newIdx]
        dao.updateMeta(meta.copy(position = other.position))
        dao.updateMeta(other.copy(position = meta.position))
    }

    // ---- Dinero: registrar / restar ----
    suspend fun registrar(pool: PoolState, amount: Double, type: MoneyType) {
        val updated = when (type) {
            MoneyType.EFECTIVO -> pool.copy(cash = pool.cash + amount)
            MoneyType.DIGITAL -> pool.copy(digital = pool.digital + amount)
        }
        dao.upsertPool(updated)
        dao.insertMovement(Movement(kind = MovementKind.DEPOSIT, amount = amount, moneyType = type, tsMillis = System.currentTimeMillis()))
    }

    /** Devuelve true si pudo restar (había suficiente saldo de ese tipo). */
    suspend fun restar(pool: PoolState, amount: Double, type: MoneyType): Boolean {
        val disponible = if (type == MoneyType.EFECTIVO) pool.cash else pool.digital
        if (amount > disponible) return false
        val updated = when (type) {
            MoneyType.EFECTIVO -> pool.copy(cash = pool.cash - amount)
            MoneyType.DIGITAL -> pool.copy(digital = pool.digital - amount)
        }
        dao.upsertPool(updated)
        dao.insertMovement(Movement(kind = MovementKind.WITHDRAW, amount = amount, moneyType = type, tsMillis = System.currentTimeMillis()))
        return true
    }

    /**
     * Marca una meta como comprada: la mueve al archivo de "Comprados" y
     * descuenta su precio del pozo (proporcional a la mezcla efectivo/digital
     * que había en ese momento).
     */
    suspend fun marcarComprado(pool: PoolState, meta: Meta) {
        val total = pool.cash + pool.digital
        val ratioCash = if (total > 0) pool.cash / total else 0.0
        val subtractCash = minOf(pool.cash, meta.goal * ratioCash)
        val subtractDigital = minOf(pool.digital, meta.goal - subtractCash)
        dao.upsertPool(pool.copy(cash = pool.cash - subtractCash, digital = pool.digital - subtractDigital))

        dao.insertPurchased(
            Purchased(name = meta.name, imageUri = meta.imageUri, goal = meta.goal, boughtAtMillis = System.currentTimeMillis())
        )
        dao.insertMovement(
            Movement(kind = MovementKind.PURCHASE, amount = meta.goal, moneyType = null, label = meta.name, tsMillis = System.currentTimeMillis())
        )
        dao.deleteMeta(meta)
    }

    /**
     * Cada meta compara su precio contra el TOTAL completo del pozo (no se
     * reparte ni se resta entre metas). Devuelve cuánto se considera
     * "ahorrado" para esa meta puntual.
     */
    fun savedFor(meta: Meta, pool: PoolState): Double {
        val total = pool.cash + pool.digital
        return minOf(total, meta.goal)
    }
}
