package com.example.alarm;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * [기존 주석] Room 데이터베이스를 설정하고 관리하는 메인 클래스입니다.
 */
// [핵심 수정] 데이터베이스 버전을 5에서 6으로 올립니다.
// version 2: isVibrationEnabled 필드 추가
// version 3: 요일 반복(isMondayEnabled 등 7개) 필드 추가
// version 4: 알람음(soundUri) 필드 추가
// version 5: 알람 이름(name) 필드 추가
// version 6: 날씨 TTS 기능(isWeatherTtsEnabled) 필드가 추가되어 스키마가 변경되었기 때문입니다.
@Database(entities = {Alarm.class}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract AlarmDao alarmDao();

    private static volatile AppDatabase INSTANCE;

    // [기존 주석] 버전 3 -> 4 마이그레이션.
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE alarms ADD COLUMN sound_uri TEXT");
        }
    };

    // [기존 주석] 버전 4 -> 5 마이그레이션.
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE alarms ADD COLUMN name TEXT");
        }
    };

    // --- [새로운 내용] --- //
    /**
     * [새로운 주석] 데이터베이스 스키마 변경(버전 5 -> 6)을 처리하는 마이그레이션 객체입니다.
     * Room에게 버전 5에서 버전 6으로 어떻게 업그레이드할지 알려주는 '설명서' 역할을 합니다.
     */
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // [새로운 주석] "alarms" 테이블에 "is_weather_tts_enabled" 이라는 새로운 컬럼(칸)을 추가합니다.
            // 이 컬럼은 정수(INTEGER) 타입을 가지며, null이 될 수 없고(NOT NULL), 기본값(DEFAULT)은 0 (false) 입니다.
            database.execSQL("ALTER TABLE alarms ADD COLUMN is_weather_tts_enabled INTEGER NOT NULL DEFAULT 0");
        }
    };
    // ------------------- //


    /**
     * [기존 주석] 데이터베이스 인스턴스를 가져오는 정적 메소드입니다. (싱글톤 패턴)
     */
    static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "alarm_database")
                            // [핵심 수정] 새로 만든 MIGRATION_5_6을 마이그레이션 경로에 추가합니다.
                            // Room은 버전에 맞는 마이그레이션을 순서대로 실행하여 데이터 손실 없이 DB 구조를 변경합니다.
                            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
