package com.alex.metasahorro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // ---- Metas activas ----
    @Query("SELECT * FROM metas ORDER BY position ASC")
    fun observeMetas(): Flow<List<Meta>>

    @Query("SELECT * FROM metas ORDER BY position ASC")
    suspend fun getMetas(): List<Meta>

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
    suspend fun insertPurchased(purchased: Purchased): Long

    @Query("DELETE FROM purchased WHERE id = :id")
    suspend fun deletePurchased(id: Long)

    // ---- Historial de movimientos ----
    @Query("SELECT * FROM movements ORDER BY tsMillis DESC")
    fun observeMovements(): Flow<List<Movement>>

    @Query("SELECT * FROM movements ORDER BY tsMillis DESC")
    suspend fun getMovements(): List<Movement>

    @Query("SELECT * FROM movements WHERE id = :id")
    suspend fun getMovement(id: Long): Movement?

    @Insert
    suspend fun insertMovement(movement: Movement): Long

    @Query("DELETE FROM movements WHERE id = :id")
    suspend fun deleteMovement(id: Long)

    // ---- Pozo de dinero (fila única) ----
    @Query("SELECT * FROM pool_state WHERE id = 0")
    fun observePool(): Flow<PoolState?>

    @Query("SELECT * FROM pool_state WHERE id = 0")
    suspend fun getPool(): PoolState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPool(pool: PoolState)

    // ---- Logros ----
    @Query("SELECT * FROM achievements")
    fun observeAchievements(): Flow<List<AchievementUnlock>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(unlock: AchievementUnlock): Long
}
