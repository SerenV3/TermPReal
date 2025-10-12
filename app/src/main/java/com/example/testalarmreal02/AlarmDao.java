package com.example.testalarmreal02;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object (DAO) for the Alarm entity.
 * 이 인터페이스는 데이터베이스에 대한 모든 CRUD(Create, Read, Update, Delete) 작업을 정의합니다.
 * Room 라이브러리가 이 인터페이스의 구현체를 자동으로 생성해 줍니다.
 */
@Dao
public interface AlarmDao {

    /**
     * 새로운 알람을 데이터베이스에 삽입합니다.
     * @param alarm 삽입할 Alarm 객체
     */
    @Insert
    void insert(Alarm alarm);

    /**
     * 기존 알람 정보를 업데이트합니다.
     * @param alarm 업데이트할 Alarm 객체
     */
    @Update
    void update(Alarm alarm);

    /**
     * 특정 알람을 데이터베이스에서 삭제합니다.
     * @param alarm 삭제할 Alarm 객체
     */
    @Delete
    void delete(Alarm alarm);

    /**
     * 'alarms' 테이블의 모든 알람 데이터를 가져옵니다.
     * 결과를 시간 오름차순으로 정렬합니다. (예: 08:00, 09:30, 14:00 순)
     *
     * 반환 타입이 LiveData<List<Alarm>>이므로, 데이터베이스에 변경이 생기면
     * 이 LiveData를 구독(observing)하는 UI 컴포넌트가 자동으로 알림을 받아 UI를 갱신할 수 있습니다.
     *
     * @return 알람 객체 리스트를 담고 있는 LiveData
     */
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    LiveData<List<Alarm>> getAllAlarms();
}
