package com.example.alarm;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmViewModel extends AndroidViewModel {

    private AlarmDao alarmDao;
    private LiveData<List<Alarm>> allAlarms;

    // 데이터베이스 작업을 백그라운드 스레드에서 실행하기 위한 ExecutorService
    // 간단한 예제에서는 하나의 스레드만 사용합니다.
    private final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    public AlarmViewModel(@NonNull Application application) {
        super(application);
        // AppDatabase 인스턴스를 가져오고, 이를 통해 AlarmDao 인스턴스를 가져옵니다.
        AppDatabase db = AppDatabase.getDatabase(application);
        alarmDao = db.alarmDao();
        // AlarmDao로부터 모든 알람 목록을 LiveData 형태로 가져옵니다.
        allAlarms = alarmDao.getAllAlarms();
    }

    // UI(Activity/Fragment)에서 관찰할 모든 알람 목록을 반환합니다.
    public LiveData<List<Alarm>> getAllAlarms() {
        return allAlarms;
    }

    // 새로운 알람을 데이터베이스에 삽입합니다.
    // 이 작업은 백그라운드 스레드에서 실행됩니다.
    public void insert(Alarm alarm) {
        databaseWriteExecutor.execute(() -> {
            alarmDao.insert(alarm);
        });
    }

    // 기존 알람 정보를 업데이트합니다.
    // 이 작업은 백그라운드 스레드에서 실행됩니다.
    public void update(Alarm alarm) {
        databaseWriteExecutor.execute(() -> {
            alarmDao.update(alarm);
        });
    }

    // 특정 알람을 삭제합니다. (필요에 따라 추가)
    // 이 작업은 백그라운드 스레드에서 실행됩니다.
    public void delete(Alarm alarm) {
        databaseWriteExecutor.execute(() -> {
            alarmDao.delete(alarm);
        });
    }

    // ViewModel이 소멸될 때 ExecutorService를 종료합니다.
    @Override
    protected void onCleared() {
        super.onCleared();
        databaseWriteExecutor.shutdown();
    }
}
