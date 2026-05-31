package com.example.imageviewer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    
    companion object {
        val DEFAULT_SPEED = doublePreferencesKey("default_speed")
        val LONG_PRESS_MULTIPLIER = doublePreferencesKey("long_press_multiplier")
        val AUTO_HIDE_TIME = intPreferencesKey("auto_hide_time")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
    
    val defaultSpeed: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_SPEED] ?: 1.0
    }
    
    val longPressMultiplier: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[LONG_PRESS_MULTIPLIER] ?: 2.0
    }
    
    val autoHideTime: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[AUTO_HIDE_TIME] ?: 3
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "system"
    }
    
    suspend fun setDefaultSpeed(speed: Double) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_SPEED] = speed
        }
    }
    
    suspend fun setLongPressMultiplier(multiplier: Double) {
        context.dataStore.edit { preferences ->
            preferences[LONG_PRESS_MULTIPLIER] = multiplier
        }
    }
    
    suspend fun setAutoHideTime(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_HIDE_TIME] = seconds
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }
}
