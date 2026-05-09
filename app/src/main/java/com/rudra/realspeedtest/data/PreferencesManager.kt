package com.rudra.realspeedtest.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val AUTO_TEST_ENABLED_KEY = booleanPreferencesKey("auto_test_enabled")
        val AUTO_TEST_INTERVAL_KEY = doublePreferencesKey("auto_test_interval")
        val SPEED_THRESHOLD_KEY = doublePreferencesKey("speed_threshold")
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }

    val autoTestEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_TEST_ENABLED_KEY] ?: false
    }

    val autoTestInterval: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[AUTO_TEST_INTERVAL_KEY] ?: 30.0
    }

    val speedThreshold: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[SPEED_THRESHOLD_KEY] ?: 10.0
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    suspend fun setAutoTestEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_TEST_ENABLED_KEY] = enabled
        }
    }

    suspend fun setAutoTestInterval(minutes: Double) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_TEST_INTERVAL_KEY] = minutes
        }
    }

    suspend fun setSpeedThreshold(threshold: Double) {
        context.dataStore.edit { preferences ->
            preferences[SPEED_THRESHOLD_KEY] = threshold
        }
    }
}