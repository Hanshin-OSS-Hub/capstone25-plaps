package com.example.plaps.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// [3] DAO: DB 조작 인터페이스
@Dao
interface EventDao {
    @Query("SELECT * FROM events")
    fun getAllEvents(): Flow<List<Event>> // 모든 일정 조회

    @Query("SELECT * FROM events WHERE date = :date")
    fun getEventsByDate(date: LocalDate): Flow<List<Event>> // 특정 날짜 일정 조회

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event) // 일정 추가/수정

    @Delete
    suspend fun deleteEvent(event: Event) // 일정 삭제
}