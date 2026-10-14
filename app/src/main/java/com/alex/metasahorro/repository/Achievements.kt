package com.alex.metasahorro.repository

import androidx.annotation.StringRes
import com.alex.metasahorro.R

/** Logros que se desbloquean solos al cumplir su condición. El nombre del enum es el id que se guarda en Room. */
enum class Achievement(val emoji: String, @StringRes val title: Int, @StringRes val description: Int) {
    FIRST_DEPOSIT("🌱", R.string.ach_first_deposit_title, R.string.ach_first_deposit_desc),
    STREAK_3("🔥", R.string.ach_streak3_title, R.string.ach_streak3_desc),
    STREAK_7("⚡", R.string.ach_streak7_title, R.string.ach_streak7_desc),
    STREAK_30("🏅", R.string.ach_streak30_title, R.string.ach_streak30_desc),
    FIRST_PURCHASE("🏆", R.string.ach_first_purchase_title, R.string.ach_first_purchase_desc),
    PURCHASES_5("🛍️", R.string.ach_purchases5_title, R.string.ach_purchases5_desc),
    SAVED_100K("💰", R.string.ach_saved100k_title, R.string.ach_saved100k_desc),
    SAVED_500K("💎", R.string.ach_saved500k_title, R.string.ach_saved500k_desc),
    SAVED_1M("👑", R.string.ach_saved1m_title, R.string.ach_saved1m_desc),
    FIRST_TRANSFORM("🔁", R.string.ach_transform_title, R.string.ach_transform_desc),
    CATEGORIES_3("🏷️", R.string.ach_categories_title, R.string.ach_categories_desc)
}

/** Datos de la app que miran las condiciones de los logros. */
data class AchievementStats(
    val deposits: Int,
    val transforms: Int,
    val purchases: Int,
    val streak: Int,
    val totalSaved: Double,
    val categories: Int
)

/** Todos los logros cuya condición se cumple con estos datos (incluye los ya desbloqueados antes). */
fun achievementsReached(s: AchievementStats): Set<Achievement> = buildSet {
    if (s.deposits >= 1) add(Achievement.FIRST_DEPOSIT)
    if (s.streak >= 3) add(Achievement.STREAK_3)
    if (s.streak >= 7) add(Achievement.STREAK_7)
    if (s.streak >= 30) add(Achievement.STREAK_30)
    if (s.purchases >= 1) add(Achievement.FIRST_PURCHASE)
    if (s.purchases >= 5) add(Achievement.PURCHASES_5)
    if (s.totalSaved >= 100_000) add(Achievement.SAVED_100K)
    if (s.totalSaved >= 500_000) add(Achievement.SAVED_500K)
    if (s.totalSaved >= 1_000_000) add(Achievement.SAVED_1M)
    if (s.transforms >= 1) add(Achievement.FIRST_TRANSFORM)
    if (s.categories >= 3) add(Achievement.CATEGORIES_3)
}
