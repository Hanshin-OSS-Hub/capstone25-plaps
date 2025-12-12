package com.example.plaps.data

import android.content.Context
import androidx.room.*

// [4] Database: Room 데이터베이스의 추상 클래스를 정의합니다.
// entities: 이 DB에 포함될 테이블(엔티티) 목록을 지정합니다.
// version: DB 스키마 버전입니다. 스키마 변경 시 버전을 올려야 합니다.
@Database(entities = [Event::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class) // DB에 저장할 수 없는 타입(LocalDate, LocalTime)을 변환하는 Converter를 연결합니다.
abstract class AppDatabase : RoomDatabase() {

    // 이 메서드를 통해 Event 테이블에 접근하는 DAO 인터페이스를 외부에 노출합니다.
    abstract fun eventDao(): EventDao

    companion object {
        // 다른 스레드에서 즉시 변경 사항을 볼 수 있도록 해주는 키워드입니다. (싱글톤 패턴 필수)
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // DB 인스턴스를 얻는 함수 (싱글톤 패턴 적용)
        fun getDatabase(context: Context): AppDatabase {
            // INSTANCE가 null이 아니면 기존 인스턴스를 반환
            return INSTANCE ?: synchronized(this) {
                // INSTANCE가 null이면 새롭게 DB 인스턴스를 생성
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "event_database" // DB 파일 이름
                )
                    // 복잡한 쿼리 처리를 위해 Coroutine 사용을 보장합니다.
                    .fallbackToDestructiveMigration() // 스키마 변경 시 DB 초기화 옵션 (간단한 앱에서 주로 사용)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}