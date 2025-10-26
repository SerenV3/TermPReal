package com.example.alarm;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room 데이터베이스를 설정하고 관리하는 메인 클래스입니다.
 * 이 클래스는 앱 전체에서 단 하나의 인스턴스만 존재해야 합니다 (싱글톤 패턴).
 *
 * @Database 어노테이션은 이 클래스가 Room 데이터베이스임을 나타냅니다.
 *  - entities: 이 데이터베이스에 포함될 테이블(Entity) 클래스들의 목록입니다. 현재는 Alarm 클래스만 있습니다.
 *  - version: 데이터베이스의 버전 번호입니다. 스키마(테이블 구조)가 변경될 때마다 이 번호를 올려야 합니다.
 *  - exportSchema: 스키마 정보를 파일로 내보낼지 여부입니다. 버전 관리 시스템에 스키마 히스토리를 저장하는 데 유용합니다.
 */
// [수정] 데이터베이스 버전을 2에서 3으로 올립니다.
// version 2: isVibrationEnabled 필드 추가
// version 3: 요일 반복(isMondayEnabled 등 7개) 필드 추가로 스키마가 또다시 변경되었기 때문입니다.
@Database(entities = {Alarm.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // 이 데이터베이스와 관련된 DAO(Data Access Object)를 가져오는 추상 메소드입니다.
    // Room이 이 메소드의 구현체를 자동으로 생성해줍니다.
    public abstract AlarmDao alarmDao();

    // 데이터베이스 인스턴스를 저장하기 위한 volatile 변수입니다. (싱글톤 패턴)
    // volatile 키워드는 이 변수가 여러 스레드에서 접근될 때, 항상 최신 값을 가지도록 보장합니다.
    private static volatile AppDatabase INSTANCE;

    /**
     * 데이터베이스 인스턴스를 가져오는 정적 메소드입니다. (싱글톤 패턴)
     * 이 메소드는 앱 전체에서 데이터베이스 인스턴스가 단 하나만 생성되도록 보장합니다.
     * @param context 애플리케이션 컨텍스트
     * @return AppDatabase의 싱글톤 인스턴스
     */
    static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            // synchronized 블록은 여러 스레드가 동시에 이 코드에 접근하여
            // 여러 개의 인스턴스를 생성하는 것을 방지합니다.
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // 데이터베이스 인스턴스를 생성합니다.
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "alarm_database")
                            // [유지] 마이그레이션(Migration) 방법이 없을 때, 데이터베이스를 재생성(기존 데이터 삭제)하도록 허용합니다.
                            // version 2 -> 3으로 올라갈 때도, 별도의 마이그레이션 계획이 없으므로
                            // 이 옵션은 Room이 기존 테이블을 버리고 새로운 스키마(version 3)에 맞춰 테이블을 새로 만들도록 합니다.
                            // 개발 중에는 편리하지만, 출시된 앱에서는 사용자 데이터가 모두 사라지므로 절대 사용하면 안 됩니다.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
