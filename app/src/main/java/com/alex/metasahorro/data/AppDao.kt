package com.alex.metasahorro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // ---- Metas activas ----
    @Query("SELECT * FROM metas ORDER BY position ASC")
    fun observeMetas(): Flow<List<Meta>>

    @Insert
    suspend fun insertMeta(meta: Meta): Long

    @Update
    suspend fun updateMeta(meta: Meta)

    @Delete
    suspend fun deleteMeta(meta: Meta)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM metas")
    suspend fun nextPosition(): Int

    // ---- Comprados ----
    @Query("SELECT * FROM purchased ORDER BY boughtAtMillis DESC")
    fun observePurchased(): Flow<List<Purchased>>

    @Insert
    suspend fun insertPurchased(purchased: Purchased)

    // ---- Historial de movimientos ----
    @Query("SELECT * FROM movements ORDER BY tsMillis DESC")
    fun observeMovements(): Flow<List<Movement>>

    @Insert
    suspend fun insertMovement(movement: Movement)

    // ---- Pozo de dinero (fila única) ----
    @Query("SELECT * FROM pool_state WHERE id = 0")
    fun observePool(): Flow<PoolState?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPool(pool: PoolState)
}
