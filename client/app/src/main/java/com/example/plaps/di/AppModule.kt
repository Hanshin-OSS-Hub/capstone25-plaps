package com.example.plaps.di

import android.content.Context
import androidx.room.Room
import com.example.plaps.data.AppDatabase
import com.example.plaps.data.EventDao
import com.example.plaps.data.AchievementDao // 👈 추가
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// [2] Hilt Module: 이 파일이 Hilt에게 객체를 만드는 방법을 알려주는 설계도임을 선언합니다.
@Module
// 이 모듈에서 제공하는 부품들을 앱 전체 수명 주기 동안 유지하도록 설정합니다.
@InstallIn(SingletonComponent::class)
object AppModule {

    // [2-1] Database 제공: Room Database를 생성하고 앱 전체에서 단 하나만 사용하도록 제공합니다.
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "event_database" // DB 파일 이름
        )
            .fallbackToDestructiveMigration() // 스키마 변경 시 DB 초기화 옵션
            .build()
    }

    // [2-2] EventDao 제공: Database 인스턴스를 사용하여 EventDao를 제공합니다.
    @Provides
    fun provideEventDao(database: AppDatabase): EventDao {
        // Hilt가 provideDatabase 함수가 반환한 AppDatabase 인스턴스를 자동으로 주입해 줍니다.
        return database.eventDao()
    }
    // [2-3] AchievementDao 제공 (👈 이 부분을 새로 추가했습니다!)
    @Provides
    fun provideAchievementDao(database: AppDatabase): AchievementDao {
        // Hilt가 위에서 만든 database 인스턴스를 사용해 AchievementDao를 가져옵니다.
        return database.achievementDao()
    }
}