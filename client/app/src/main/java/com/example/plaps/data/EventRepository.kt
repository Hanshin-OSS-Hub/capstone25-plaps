package com.example.plaps.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

// [1] Repository: DAO와 ViewModel 사이에서 데이터 접근을 추상화합니다.
// @Inject constructor를 사용하여 Hilt에게 이 클래스를 만들고 주입할 수 있도록 알립니다.
@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao,
    private val achievementDao: AchievementDao
) {
    fun getAllEvents(): Flow<List<Event>> = eventDao.getAllEvents()
    fun getAllAchievements(): Flow<List<Achievement>> = achievementDao.getAllAchievements()

    suspend fun saveEvent(event: Event) {
        eventDao.insertEvent(event)
    }

    suspend fun deleteEvent(event: Event) {
        eventDao.deleteEvent(event)
    }

    // 1. 앱 초기 실행 시 기본 업적 DB에 세팅(id 지금 4개로 업적 4개만 등록된 상태임. 언제든 추가 가능)
    suspend fun initDefaultAchievements() {
        val defaultAchievements = listOf(
            Achievement("1", "위대한 첫걸음", "첫 번째 일정을 등록했습니다", isUnlocked = false, goalValue = 1, currentValue = 0),
            Achievement("2", "벌써 일정 20개!", "20개의 일정을 완료하세요", isUnlocked = false, goalValue = 20, currentValue = 0),
            Achievement("3", "성실한 일정 관리", "30개의 일정을 완료하세요", isUnlocked = false, goalValue = 30, currentValue = 0),
            Achievement("4", "일정 관리의 달인", "50개의 일정을 완료하세요", isUnlocked = false, goalValue = 50, currentValue = 0)
        )
        achievementDao.insertAchievements(defaultAchievements) // OnConflictStrategy.IGNORE 덕분에 중복으로 들어가지 않습니다.
    }

    // 2. 일정을 처음 '등록'했을 때 1번 업적 달성 처리
    suspend fun checkFirstEventAchievement() {
        val achievement = achievementDao.getAchievementById("1")
        if (achievement != null && !achievement.isUnlocked) {
            achievementDao.updateAchievement(
                achievement.copy(currentValue = 1, isUnlocked = true, unlockDate = LocalDate.now())
            )
        }
    }

    // 3. 일정을 완료했을 때 2,3,4번 업적 진척도 올리기
    suspend fun updateCompletionAchievements(completedCount: Int) {
        val targetIds = listOf("2", "3", "4") // 완료 관련 업적 ID들

        for (id in targetIds) {
            val achievement = achievementDao.getAchievementById(id)
            if (achievement != null && !achievement.isUnlocked) {
                // 달성 여부 확인 (현재 완료 개수가 목표치 이상인가?)
                val isNowUnlocked = completedCount >= achievement.goalValue

                achievementDao.updateAchievement(
                    achievement.copy(
                        currentValue = completedCount,
                        isUnlocked = isNowUnlocked,
                        unlockDate = if (isNowUnlocked) LocalDate.now() else null
                    )
                )
            }
        }
    }
}