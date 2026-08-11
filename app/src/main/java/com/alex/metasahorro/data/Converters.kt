package com.alex.metasahorro.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMovementKind(value: MovementKind): String = value.name

    @TypeConverter
    fun toMovementKind(value: String): MovementKind = MovementKind.valueOf(value)

    @TypeConverter
    fun fromMoneyType(value: MoneyType?): String? = value?.name

    @TypeConverter
    fun toMoneyType(value: String?): MoneyType? = value?.let { MoneyType.valueOf(it) }
}
