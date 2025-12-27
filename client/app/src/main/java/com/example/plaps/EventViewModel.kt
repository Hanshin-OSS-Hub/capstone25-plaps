package com.example.plaps

import com.example.plaps.data.Event
import com.example.plaps.data.EventRepository // 👈 새로 만든 Repository import
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel // 👈 Hilt ViewModel Import
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject // 👈 주입을 요청하는 Inject Import

// [1] Hilt ViewModel: Hilt가 이 ViewModel을 만들도록 지시합니다.
@HiltViewModel
// [2] Repository 주입: Hilt가 EventRepository 객체를 만들어서 자동으로 넣어줍니다.
class EventViewModel @Inject constructor(
    private val repository: EventRepository // 👈 DB 대신 Repository를 주입받습니다.
) : ViewModel() { // 👈 AndroidViewModel 대신 일반 ViewModel을 상속합니다.

    // DB의 모든 이벤트를 관찰 (Flow -> StateFlow 변환)
    val allEvents: StateFlow<List<Event>> = repository.getAllEvents() // 👈 Repository에서 데이터를 가져옵니다.
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 이벤트 추가/수정
    fun saveEvent(event: Event) {
        viewModelScope.launch {
            repository.saveEvent(event) // 👈 Repository에게 저장 요청
        }
    }

    // 이벤트 삭제
    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            repository.deleteEvent(event) // 👈 Repository에게 삭제 요청
        }
    }
}