package com.example.plaps.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plaps.AlarmScheduler
import com.example.plaps.data.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "settings")

@HiltViewModel
class ThemeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val NOTIFICATION_KEY = booleanPreferencesKey("notification_enabled")

    private val alarmScheduler = AlarmScheduler(context)

    val isDarkMode: StateFlow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DARK_MODE_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isNotificationEnabled: StateFlow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFICATION_KEY] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { preferences -> preferences[DARK_MODE_KEY] = isDark }
        }
    }

    fun toggleNotification(isEnabled: Boolean) {
        viewModelScope.launch {
            // 1. 설정 저장
            context.dataStore.edit { preferences -> preferences[NOTIFICATION_KEY] = isEnabled }
            val prefs = context.getSharedPreferences("plaps_settings", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("notification_enabled", isEnabled).apply()

            if (!isEnabled) {
                try {
                    // 전체 일정을 Flow에서 1회성 리스트로 단발 수신 후 cancel 연타
                    val allEvents = eventRepository.getAllEvents().first()
                    allEvents.forEach { event ->
                        alarmScheduler.cancel(event)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}