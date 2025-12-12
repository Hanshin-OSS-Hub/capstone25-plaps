package com.example.plaps

import com.example.plaps.data.AppDatabase // AppDatabase.kt 파일에서 가져옴
import com.example.plaps.data.Event // 👈 New Import
import com.example.plaps.data.EventDao     // EventDao.kt 파일에서 가져옴
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EventViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.eventDao()

    // DB의 모든 이벤트를 관찰 (Flow -> StateFlow 변환)
    val allEvents: StateFlow<List<Event>> = dao.getAllEvents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 이벤트 추가/수정
    fun saveEvent(event: Event) {
        viewModelScope.launch {
            dao.insertEvent(event)
        }
    }

    // 이벤트 삭제
    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            dao.deleteEvent(event)
        }
    }
}