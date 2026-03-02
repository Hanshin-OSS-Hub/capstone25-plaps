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

    // 👇 앱이 켜질 때(=ViewModel 생성 시) 기본 업적을 DB에 넣어줍니다.
    init {
        viewModelScope.launch {
            repository.initDefaultAchievements()
        }
    }

    val allEvents: StateFlow<List<Event>> = repository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAchievements: StateFlow<List<Achievement>> = repository.getAllAchievements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 👇 일정 저장 & 첫 등록 업적 처리
    fun saveEvent(event: Event) {
        viewModelScope.launch {
            repository.saveEvent(event)
            // 첫 일정을 등록했으므로 1번 업적 체크
            repository.checkFirstEventAchievement()

            // 만약 저장/수정하면서 완료 상태(isCompleted)가 바뀌었다면 진척도도 업데이트
            updateAchievementProgress()
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            repository.deleteEvent(event)
            // 일정이 삭제되어 완료 개수가 줄어들 수 있으니 진척도 재계산
            updateAchievementProgress()
        }
    }

    // 👇 현재 DB에 있는 '완료된 일정' 개수를 세서 진척도를 업데이트하는 함수
    private suspend fun updateAchievementProgress() {
        // allEvents의 최신 리스트에서 isCompleted가 true인 것의 개수를 구함
        val completedCount = allEvents.value.count { it.isCompleted }
        repository.updateCompletionAchievements(completedCount)
    }
}