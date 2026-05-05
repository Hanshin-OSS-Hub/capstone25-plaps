package com.example.plaps

import com.example.plaps.data.Achievement // 👈 추가
import com.example.plaps.data.Event
import com.example.plaps.data.EventRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    // init 블록 제거 — initDefaultAchievements()는 Application에서 앱 시작 시 한 번만 실행

    val allEvents: StateFlow<List<Event>> = repository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAchievements: StateFlow<List<Achievement>> = repository.getAllAchievements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 👇 일정 저장 — 업적 체크는 Repository 내부에서 자동 처리
    fun saveEvent(event: Event) {
        viewModelScope.launch {
            repository.saveEvent(event)
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            repository.deleteEvent(event)
            // 일정이 삭제되어 완료 개수가 줄어들 수 있으니 진척도 재계산은 Repository 내부에서 자동 실행
        }
    }

    // 👇 일정 완료 체크박스 토글 — 진척도 업데이트는 Repository 내부에서 자동 실행
    fun toggleEventCompletion(event: Event) {
        viewModelScope.launch {
            repository.toggleEventCompletion(event)
        }
    }
}