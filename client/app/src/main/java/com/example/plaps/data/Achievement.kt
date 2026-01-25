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
    val unlockDate: LocalDate? = null // 달성한 날짜
)