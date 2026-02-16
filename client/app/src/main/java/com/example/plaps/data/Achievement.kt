package com.example.plaps.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String, // 업적 고유 아이디 (예: "FIRST_EVENT")
    val title: String,          // 업적 이름 (예: "첫 걸음")
    val description: String,    // 업적 설명
    val isUnlocked: Boolean = false, // 달성 여부
    val unlockDate: LocalDate? = null, // 달성한 날짜

    val category: String = "일반",    // 업적 분류 (예: "공부", "운동", "출석")
    val goalValue: Int = 1,          // 목표치 (예: 일정 10개 완료라면 10)
    val currentValue: Int = 0,      // 현재 진행도 (예: 현재 3개 완료했다면 3)
)