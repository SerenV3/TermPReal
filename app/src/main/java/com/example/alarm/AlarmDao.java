package com.example.alarm;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao // 이 인터페이스가 DAO임을 나타냅니다.
public interface AlarmDao {

    @Insert // 새로운 알람을 데이터베이스에 삽입합니다.
    void insert(Alarm alarm);

    @Update // 기존 알람 정보를 업데이트합니다.
    void update(Alarm alarm);

    @Delete // 특정 알람을 데이터베이스에서 삭제합니다.
    void delete(Alarm alarm);

    // 모든 알람을 시간 오름차순으로 정렬하여 가져옵니다.
    // LiveData를 사용하여 데이터 변경 시 UI가 자동으로 업데이트되도록 할 수 있습니다.
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    LiveData<List<Alarm>> getAllAlarms();

    // ID를 기준으로 특정 알람 하나만 가져옵니다. (필요에 따라 사용)
    @Query("SELECT * FROM alarms WHERE id = :alarmId")
    LiveData<Alarm> getAlarmById(int alarmId);

    // (선택 사항) 모든 알람을 삭제하는 메소드 (개발 중 유용할 수 있음)
    // @Query("DELETE FROM alarms")
    // void deleteAllAlarms();
}
