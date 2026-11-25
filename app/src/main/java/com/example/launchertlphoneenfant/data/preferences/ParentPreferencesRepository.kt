package com.example.launchertlphoneenfant.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "parent_preferences")

class ParentPreferencesRepository(private val context: Context) {

    companion object {
        val PARENT_PIN = stringPreferencesKey("parent_pin")
        val DAILY_TIME_LIMIT_MINUTES = intPreferencesKey("daily_time_limit_minutes")
        val BONUS_TIME_MINUTES = intPreferencesKey("bonus_time_minutes")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val LAST_RESET_DATE = stringPreferencesKey("last_reset_date")
        val FAMILY_CODE = stringPreferencesKey("family_code")
    }

    val parentPin: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PARENT_PIN] ?: "0000"
    }

    val dailyTimeLimitMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[DAILY_TIME_LIMIT_MINUTES] ?: 60 // 1 heure par défaut
    }

    val bonusTimeMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[BONUS_TIME_MINUTES] ?: 0
    }

    val selectedTheme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_THEME] ?: "SHONEN"
    }

    val lastResetDate: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LAST_RESET_DATE] ?: ""
    }

    val familyCode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[FAMILY_CODE] ?: "FAM-8492"
    }

    suspend fun setParentPin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[PARENT_PIN] = pin
        }
    }

    suspend fun setDailyTimeLimit(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[DAILY_TIME_LIMIT_MINUTES] = minutes
        }
    }

    suspend fun addBonusTime(minutes: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[BONUS_TIME_MINUTES] ?: 0
            prefs[BONUS_TIME_MINUTES] = current + minutes
        }
    }

    suspend fun setSelectedTheme(theme: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_THEME] = theme
        }
    }

    suspend fun setFamilyCode(code: String) {
        context.dataStore.edit { prefs ->
            prefs[FAMILY_CODE] = code
        }
    }

    suspend fun updateResetDate(dateString: String) {
        context.dataStore.edit { prefs ->
            prefs[LAST_RESET_DATE] = dateString
            prefs[BONUS_TIME_MINUTES] = 0 // Réinitialise le bonus du jour
        }
    }
}
