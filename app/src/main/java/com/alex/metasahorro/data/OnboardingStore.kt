package com.alex.metasahorro.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")
private val SEEN_ONBOARDING_KEY = booleanPreferencesKey("seen_onboarding")

class OnboardingStore(private val context: Context) {

    val hasSeenOnboarding = context.onboardingDataStore.data.map { prefs ->
        prefs[SEEN_ONBOARDING_KEY] ?: false
    }

    suspend fun markSeen() {
        context.onboardingDataStore.edit { it[SEEN_ONBOARDING_KEY] = true }
    }
}
