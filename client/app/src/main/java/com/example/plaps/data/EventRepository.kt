package com.example.plaps.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

// 업적 ID 상수 — 한 곳에서 관리, 추가 시 여기에만 작성
object AchievementId {
    const val FIRST_EVENT   = "1"  // 첫 번째 일정 등록
    const val TWENTY_EVENTS = "2"  // 20개 완료
    const val THIRTY_EVENTS = "3"  // 30개 완료
    const val FIFTY_EVENTS  = "4"  // 50개 완료
}

// [1] Repository: DAO와 ViewModel 사이에서 데이터 접근을 추상화합니다.
@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao,
    private val achievementDao: AchievementDao
) {
    fun getAllEvents(): Flow<List<Event>> = eventDao.getAllEvents()

    // ✨ [개선] 앱이 켜질 때 자동으로 초기화 루틴을 타도록 설정하면 마이페이지 유실 방지 가능
    fun getAllAchievements(): Flow<List<Achievement>> = achievementDao.getAllAchievements()

    // [자동 체크 추가]: 일정을 저장할 때 첫 등록 업적을 자동으로 확인
    suspend fun saveEvent(event: Event) {
        eventDao.insertEvent(event)
        checkFirstEventAchievement() // 저장 직후 자동 실행
    }

    // [자동 체크 추가]: 일정 완료 상태를 변경할 때 업적 진척도를 자동으로 갱신
    suspend fun toggleEventCompletion(event: Event) {
        val updatedEvent = event.copy(isCompleted = !event.isCompleted)
        eventDao.insertEvent(updatedEvent)
        updateCompletionAchievements() // 상태 변경 직후 자동 실행
    }

    // [자동 체크 추가]: 일정을 삭제한 후에도 업적 개수를 다시 계산
    suspend fun deleteEvent(event: Event) {
        eventDao.deleteEvent(event)
        updateCompletionAchievements() // 삭제 후 자동 실행
    }

    // 1. 앱 초기 실행 시 기본 업적 DB에 세팅
    suspend fun initDefaultAchievements() {
        val defaultAchievements = listOf(
            Achievement(AchievementId.FIRST_EVENT,   "위대한 첫걸음",    "첫 번째 일정을 등록했습니다", isUnlocked = false, goalValue = 1,  currentValue = 0),
            Achievement(AchievementId.TWENTY_EVENTS, "벌써 일정 20개!",  "20개의 일정을 완료하세요",    isUnlocked = false, goalValue = 20, currentValue = 0),
            Achievement(AchievementId.THIRTY_EVENTS, "성실한 일정 관리", "30개의 일정을 완료하세요",    isUnlocked = false, goalValue = 30, currentValue = 0),
            Achievement(AchievementId.FIFTY_EVENTS,  "일정 관리의 달인", "50개의 일정을 완료하세요",    isUnlocked = false, goalValue = 50, currentValue = 0)
        )
        achievementDao.insertAchievements(defaultAchievements) // OnConflictStrategy.IGNORE 덕분에 중복 방지
    }

    // 2. 일정을 처음 '등록'했을 때 1번 업적 달성 처리
    private suspend fun checkFirstEventAchievement() {
        val achievement = achievementDao.getAchievementById(AchievementId.FIRST_EVENT)
        if (achievement != null && !achievement.isUnlocked) {
            // ✨ [정밀 검사] 실제로 등록된 총 일정 개수를 번거롭더라도 한 번 확인해서 확실할 때만 해제
            val totalEventCount = eventDao.getTotalCount() // 💡 DAO에 전체 일정 개수 세는 함수가 있다고 가정 (혹은 getAllEvents 활용)
            if (totalEventCount >= 1) {
                achievementDao.updateAchievement(
                    achievement.copy(currentValue = 1, isUnlocked = true, unlockDate = LocalDate.now())
                )
            }
        }
    }

    // 3. 일정을 완료했을 때 2,3,4번 업적 진척도 올리기
    private suspend fun updateCompletionAchievements() {
        val completedCount = eventDao.getCompletedCount() // DAO에 이미 있는 함수
        val targetIds = listOf(
            AchievementId.TWENTY_EVENTS,
            AchievementId.THIRTY_EVENTS,
            AchievementId.FIFTY_EVENTS
        )

        for (id in targetIds) {
            val achievement = achievementDao.getAchievementById(id)
            if (achievement != null) {
                // 🔓 이미 달성한 업적이라도, 일정을 삭제해서 완료 개수가 줄어들면 다시 잠금(false) 처리 하도록 유연성 확보
                val isNowUnlocked = completedCount >= achievement.goalValue

                achievementDao.updateAchievement(
                    achievement.copy(
                        currentValue = completedCount,
                        isUnlocked = isNowUnlocked,
                        unlockDate = if (isNowUnlocked) achievement.unlockDate ?: LocalDate.now() else null
                    )
                )
            }
        }
    }
}