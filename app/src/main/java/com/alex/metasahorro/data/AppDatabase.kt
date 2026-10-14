package com.alex.metasahorro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de datos local. Versión 2: agrega `category` a metas y comprados, y la tabla `achievements`.
 * Cada cambio de esquema necesita su `Migration` (ver [MIGRATION_1_2]) para no perder los datos del usuario.
 */
@Database(
    entities = [Meta::class, Purchased::class, Movement::class, PoolState::class, AchievementUnlock::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao(): AppDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /** v1 → v2: categorías opcionales y tabla de logros. No toca ningún dato existente. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE metas ADD COLUMN category TEXT")
                db.execSQL("ALTER TABLE purchased ADD COLUMN category TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `achievements` " +
                        "(`id` TEXT NOT NULL, `unlockedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "metas_ahorro.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
