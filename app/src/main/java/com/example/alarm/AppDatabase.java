package com.example.alarm;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * 앱 전체에서 사용될 Room 데이터베이스를 정의하고 관리하는 클래스.
 * 데이터베이스 인스턴스를 싱글톤(Singleton)으로 생성하여, 앱 내에서 단 하나의 데이터베이스 연결만
 * 존재하도록 보장합니다. 이는 리소스 낭비를 막고 데이터 일관성을 유지하는 데 중요합니다.
 *
 * @Database 어노테이션은 이 클래스가 Room 데이터베이스임을 나타냅니다.
 * - entities: 이 데이터베이스에 포함될 테이블(Entity) 클래스들의 목록입니다. 현재는 Alarm 클래스만 포함합니다.
 * - version: 데이터베이스의 스키마(구조) 버전입니다. 만약 테이블 구조(예: 컬럼 추가/삭제)를 변경했다면,
 *            이 버전을 반드시 올려주어야 합니다. 그렇지 않으면 앱이 비정상 종료될 수 있습니다.
 * - exportSchema: true로 설정하면 Room이 컴파일 시점에 데이터베이스 스키마 정보를 JSON 파일로
 *                 별도 폴더에 추출합니다. 이 스키마 파일을 버전 관리 시스템(Git 등)에 포함하여
 *                 과거 버전의 스키마를 추적하고 복잡한 마이그레이션을 관리하는 데 사용하는 것이 좋습니다.
 *                 간단한 프로젝트에서는 false로 두어도 무방합니다.
 */
@Database(entities = {Alarm.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * 이 추상 메소드는 Room이 컴파일 시점에 자동으로 구현하며,
     * 데이터베이스와 상호작용할 수 있는 DAO(Data Access Object) 인스턴스를 반환합니다.
     * 앱의 다른 부분에서는 이 메소드를 통해 AlarmDao에 접근하게 됩니다.
     * @return AlarmDao의 구현체 인스턴스
     */
    public abstract AlarmDao alarmDao();

    /**
     * 데이터베이스 인스턴스를 저장할 정적(static) 변수.
     * 'volatile' 키워드는 이 변수의 값이 메인 메모리에만 저장되도록 보장합니다.
     * 멀티스레드 환경에서 한 스레드가 INSTANCE 값을 변경하면, 다른 스레드들이
     * 캐시된 낡은 값이 아닌 메인 메모리의 최신 값을 즉시 볼 수 있게 하여,
     * 인스턴스가 여러 개 생성되는 문제를 방지합니다.
     */
    private static volatile AppDatabase INSTANCE;

    /**
     * 데이터베이스 인스턴스를 가져오는 정적(static) 팩토리 메소드.
     * 앱의 어느 곳에서든 AppDatabase.getDatabase(context)를 호출하여
     * 동일한 데이터베이스 인스턴스를 얻을 수 있습니다.
     *
     * @param context 데이터베이스 인스턴스 생성을 위해 필요한 애플리케이션 컨텍스트
     * @return AppDatabase의 싱글톤 인스턴스
     */
    public static AppDatabase getDatabase(final Context context) {
        // 1단계: 인스턴스가 이미 생성되었는지 확인. 이미 있다면 바로 반환하여 불필요한 동기화 비용을 줄입니다.
        if (INSTANCE == null) {
            // 2단계: 인스턴스가 없는 경우에만 동기화 블록으로 진입.
            // 'synchronized' 키워드는 한 번에 단 하나의 스레드만 이 블록 내부의 코드를 실행하도록 보장합니다.
            // 만약 여러 스레드가 거의 동시에 getDatabase()를 호출하더라도,
            // 여러 개의 데이터베이스 인스턴스가 생성되는 것을 막아줍니다.
            synchronized (AppDatabase.class) {
                // 3단계: 동기화 블록 내부에서 다시 한번 인스턴스가 없는지 확인 (Double-Checked Locking).
                // 첫 번째 스레드가 인스턴스를 생성하는 동안 기다리던 두 번째 스레드가
                // 불필요하게 인스턴스를 또 생성하는 것을 방지합니다.
                if (INSTANCE == null) {
                    // 데이터베이스 인스턴스를 생성합니다.
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(), // 애플리케이션 전체 생명주기와 연결된 컨텍스트
                                    AppDatabase.class,               // Room 데이터베이스 클래스
                                    "alarm_database"                 // 데이터베이스 파일의 이름
                            )
                            // .fallbackToDestructiveMigration()
                            // 이 옵션은 데이터베이스 버전이 올라갔을 때(스키마 변경 시) 별도의 마이그레이션(데이터 이전)
                            // 규칙을 정의하지 않으면, 기존의 모든 데이터를 삭제하고 테이블을 새로 만듭니다.
                            // 개발 중에는 편리하지만, 실제 출시된 앱에서는 사용자의 데이터가 사라지므로 절대 사용하면 안 됩니다.
                            // 프로덕션 앱에서는 .addMigrations()를 사용하여 마이그레이션 경로를 명시적으로 제공해야 합니다.
                            .build(); // 데이터베이스 인스턴스 최종 생성
                }
            }
        }
        // 생성되었거나 이미 존재하던 인스턴스를 반환합니다.
        return INSTANCE;
    }
}
