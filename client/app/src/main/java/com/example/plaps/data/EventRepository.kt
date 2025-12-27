package com.example.plaps.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

// [1] Repository: DAO와 ViewModel 사이에서 데이터 접근을 추상화합니다.
// @Inject constructor를 사용하여 Hilt에게 이 클래스를 만들고 주입할 수 있도록 알립니다.
@Singleton // 앱 전체에서 단 하나의 인스턴스만 사용하도록 지정
class EventRepository @Inject constructor(
    private val eventDao: EventDao // Hilt가 EventDao 객체를 알아서 주입해 줍니다.
) {
    // 모든 이벤트를 Flow 형태로 반환합니다.
    fun getAllEvents(): Flow<List<Event>> = eventDao.getAllEvents()

    // 일정 추가/수정
    suspend fun saveEvent(event: Event) {
        eventDao.insertEvent(event)
    }

    // 일정 삭제
    suspend fun deleteEvent(event: Event) {
        eventDao.deleteEvent(event)
    }
}