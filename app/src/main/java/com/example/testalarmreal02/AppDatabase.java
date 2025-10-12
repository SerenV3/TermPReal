package com.example.testalarmreal02;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * 앱 전체에서 사용될 Room 데이터베이스를 정의하고 관리하는 메인 클래스.
 *
 * @Database 어노테이션은 이 클래스가 Room 데이터베이스임을 나타냅니다.
 * - entities: 이 데이터베이스에 포함될 테이블(Entity) 클래스들의 목록입니다.
 * - version: 데이터베이스의 스키마(구조) 버전입니다. 만약 테이블 구조를 변경했다면, 이 버전을 반드시 올려주어야 합니다.
 * - exportSchema: 스키마 정보를 파일로 내보낼지 여부입니다. (학습 중에는 false로 두어도 무방합니다)
 */
@Database(entities = {Alarm.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * 이 추상 메소드는 Room이 컴파일 시점에 자동으로 구현하며,
     * 데이터베이스와 상호작용할 수 있는 DAO(Data Access Object) 인스턴스를 반환합니다.
     */
    public abstract AlarmDao alarmDao();

    // 데이터베이스 인스턴스를 싱글톤으로 관리하기 위한 static 변수입니다.
    // volatile 키워드는 여러 스레드에서 INSTANCE 변수에 접근할 때 발생할 수 있는 문제를 방지합니다.
    private static volatile AppDatabase INSTANCE;

    /**
     * 데이터베이스 인스턴스를 가져오는 static 메소드. (싱글톤 패턴)
     * synchronized 키워드는 여러 스레드가 동시에 이 메소드를 호출하여
     * 여러 개의 인스턴스가 생성되는 것을 방지합니다 (스레드 안전성 확보).
     *
     * @param context 데이터베이스 인스턴스 생성을 위해 필요한 컨텍스트
     * @return AppDatabase의 싱글톤 인스턴스
     */
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // 데이터베이스 인스턴스를 생성합니다.
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "alarm-database") // "alarm-database"는 생성될 파일 이름입니다.
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
