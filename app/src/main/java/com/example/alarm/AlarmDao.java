package com.example.alarm;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object (DAO) for the Alarm entity.
 * 이 인터페이스는 Room 데이터베이스에 접근하여 실제 쿼리를 실행하는 메소드들을 정의합니다.
 * Room 라이브러리가 컴파일 시점에 이 인터페이스의 구현체를 자동으로 생성합니다.
 *
 * @Dao 어노테이션은 이 인터페이스가 Room의 DAO임을 나타냅니다.
 */
@Dao
public interface AlarmDao {

    /**
     * 새로운 알람을 데이터베이스에 삽입(Insert)합니다.
     *
     * @param alarm 삽입할 Alarm 객체
     * @OnConflict는 데이터 삽입 시 충돌이 발생했을 때 어떻게 처리할지를 정의합니다.
     * OnConflictStrategy.REPLACE는 만약 동일한 ID(Primary Key)를 가진 데이터가 이미 존재하면,
     * 기존 데이터를 새로운 데이터로 덮어쓰는 전략입니다.
     * (현재 id가 autoGenerate이므로 이 옵션은 update와 유사하게 동작할 수 있습니다.)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Alarm alarm);

    /**
     * 기존 알람 정보를 데이터베이스에서 업데이트(Update)합니다.
     * Room은 전달된 Alarm 객체의 Primary Key(id)를 사용하여 어떤 행을 업데이트할지 결정합니다.
     *
     * @param alarm 업데이트할 Alarm 객체
     */
    @Update
    void update(Alarm alarm);

    /**
     * 특정 알람을 데이터베이스에서 삭제(Delete)합니다.
     * Room은 전달된 Alarm 객체의 Primary Key(id)를 사용하여 어떤 행을 삭제할지 결정합니다.
     *
     * @param alarm 삭제할 Alarm 객체
     */
    @Delete
    void delete(Alarm alarm);

    /**
     * 'alarms' 테이블의 모든 알람 데이터를 가져옵니다.
     *
     * @Query 어노테이션을 사용하여 직접 SQL 쿼리를 작성할 수 있습니다.
     * "SELECT * FROM alarms"는 'alarms' 테이블의 모든 컬럼과 모든 행을 선택하라는 의미입니다.
     * "ORDER BY hour ASC, minute ASC"는 결과를 'hour'를 기준으로 오름차순 정렬하고,
     * 'hour'가 같을 경우 'minute'를 기준으로 오름차순 정렬하라는 의미입니다.
     *
     * 반환 타입이 LiveData<List<Alarm>>이므로, 데이터베이스에 변경이 생기면
     * 이 LiveData를 구독(observing)하는 UI 컴포넌트가 자동으로 알림을 받아 UI를 갱신할 수 있습니다.
     *
     * @return 알람 객체 리스트를 담고 있는 LiveData
     */
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    LiveData<List<Alarm>> getAllAlarms();

    /**
     * ID를 기준으로 특정 알람 하나만 가져옵니다. (LiveData 버전)
     * UI에서 특정 알람 하나의 정보만 관찰할 때 유용합니다.
     *
     * @param alarmId 찾고자 하는 알람의 ID
     * @return 해당 ID의 알람 객체를 담고 있는 LiveData
     */
    @Query("SELECT * FROM alarms WHERE id = :alarmId")
    LiveData<Alarm> getAlarmById(int alarmId);

    /**
     * ID를 기준으로 특정 알람 하나만 가져옵니다. (Non-LiveData 버전)
     * LiveData를 관찰할 수 없는 백그라운드 서비스나 BroadcastReceiver 등에서
     * 특정 알람 데이터를 즉시 가져와야 할 때 유용합니다.
     *
     * @param alarmId 찾고자 하는 알람의 ID
     * @return 해당 ID의 알람 객체. 데이터가 없으면 null을 반환합니다.
     */
    @Query("SELECT * FROM alarms WHERE id = :alarmId")
    Alarm getAlarmByIdNonLive(int alarmId);

    /**
     * (개발용/선택사항) 데이터베이스의 모든 알람을 삭제합니다.
     * 앱을 테스트하는 동안 데이터를 쉽게 초기화하고 싶을 때 유용하게 사용할 수 있습니다.
     */
    @Query("DELETE FROM alarms")
    void deleteAllAlarms();
}
