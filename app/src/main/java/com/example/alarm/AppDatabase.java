package com.example.alarm;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// 데이터베이스 설정을 위한 어노테이션입니다.
// entities 배열에는 이 데이터베이스에 포함될 모든 @Entity 클래스들을 나열합니다.
// version은 데이터베이스 스키마의 버전입니다. 스키마 변경 시 버전을 올려야 합니다.
// exportSchema는 스키마 정보를 파일로 내보낼지 여부입니다. 프로덕션에서는 true로 하고 버전 관리에 포함하는 것이 좋습니다.
@Database(entities = {Alarm.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // 이 데이터베이스와 관련된 DAO(@Dao 인터페이스)를 반환하는 추상 메소드를 선언합니다.
    // ROOM 라이브러리가 이 메소드의 구현체를 자동으로 생성합니다.
    public abstract AlarmDao alarmDao();

    // 데이터베이스 인스턴스를 싱글톤으로 관리하기 위한 static 변수입니다.
    // volatile 키워드는 여러 스레드에서 접근 시 발생할 수 있는 문제를 방지합니다.
    private static volatile AppDatabase INSTANCE;

    // 데이터베이스 인스턴스를 가져오는 static 메소드입니다.
    // synchronized 키워드는 여러 스레드에서 동시에 이 메소드를 호출하여
    // 여러 인스턴스가 생성되는 것을 방지합니다 (스레드 안전성 확보).
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // 데이터베이스 인스턴스를 생성합니다.
                    // Room.databaseBuilder()를 사용하여 데이터베이스를 빌드합니다.
                    // 첫 번째 인자는 Application Context입니다.
                    // 두 번째 인자는 RoomDatabase를 상속받은 클래스입니다 (AppDatabase.class).
                    // 세 번째 인자는 데이터베이스 파일의 이름입니다 (예: "alarm_database").
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "alarm_database")
                            // 스키마 변경 시 이전 데이터를 유지하지 않고 데이터베이스를 새로 만드는 옵션입니다.
                            // 실제 프로덕션 앱에서는 마이그레이션 전략을 구현해야 합니다.
                            // .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
