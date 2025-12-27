package com.example.plaps.data

import androidx.room.*

// [4] Database: Room 데이터베이스의 추상 클래스를 정의합니다.
// entities: 이 DB에 포함될 테이블(엔티티) 목록을 지정합니다.
// version: DB 스키마 버전입니다. 스키마 변경 시 버전을 올려야 합니다.
@Database(entities = [Event::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class) // DB에 저장할 수 없는 타입(LocalDate, LocalTime)을 변환하는 Converter를 연결합니다.
// Hilt를 사용하므로 더 이상 Companion Object를 통한 수동 Singleton 관리가 필요하지 않습니다.
abstract class AppDatabase : RoomDatabase() {

    // 이 메서드를 통해 Event 테이블에 접근하는 DAO 인터페이스를 외부에 노출합니다.
    abstract fun eventDao(): EventDao

    // 기존의 Companion Object (getDatabase 함수)는 Hilt가 대신하므로 제거
}