package com.example.plaps.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

// [2] Entity: DB 테이블 구조 정의
@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: LocalDate,
    val title: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val location: String,
    val notes: String,
    val colorIndex: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val roadAddress: String? = null
)