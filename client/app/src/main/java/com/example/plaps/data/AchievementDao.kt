package com.example.plaps.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Update
    suspend fun updateAchievement(achievement: Achievement)

    // 특정 업적 정보 가져오기
    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievementById(id: String): Achievement?
}