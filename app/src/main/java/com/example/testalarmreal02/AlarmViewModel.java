package com.example.testalarmreal02;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UI(Activity/Fragment)를 위한 데이터를 제공하고, UI의 생명주기로부터 데이터를 보존하는 클래스.
 * UI 컨트롤러와 데이터 소스(DAO) 사이의 중개자 역할을 합니다.
 */
public class AlarmViewModel extends AndroidViewModel {

    private final AlarmDao alarmDao;
    private final LiveData<List<Alarm>> allAlarms;

    // 데이터베이스 I/O(입출력) 작업을 백그라운드 스레드에서 실행하기 위한 ExecutorService.
    // 안드로이드에서는 데이터베이스 접근과 같은 오래 걸리는 작업을 메인 스레드에서 하면 앱이 멈출 수 있습니다.
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    public AlarmViewModel(@NonNull Application application) {
        super(application);
        // AppDatabase의 싱글톤 인스턴스를 통해 DAO를 가져옵니다.
        AppDatabase db = AppDatabase.getDatabase(application);
        this.alarmDao = db.alarmDao();
        // DAO로부터 모든 알람 목록을 LiveData 형태로 가져옵니다.
        this.allAlarms = alarmDao.getAllAlarms();
    }

    /**
     * UI 컨트롤러가 데이터베이스의 모든 알람 목록을 구독(observe)할 수 있도록
     * LiveData 객체를 외부에 제공합니다.
     */
    public LiveData<List<Alarm>> getAllAlarms() {
        return allAlarms;
    }

    /**
     * 새로운 알람을 데이터베이스에 삽입하도록 요청합니다.
     * 실제 작업은 백그라운드 스레드에서 실행됩니다.
     */
    public void insert(Alarm alarm) {
        databaseExecutor.execute(() -> alarmDao.insert(alarm));
    }

    /**
     * 기존 알람 정보를 업데이트하도록 요청합니다.
     */
    public void update(Alarm alarm) {
        databaseExecutor.execute(() -> alarmDao.update(alarm));
    }

    /**
     * 특정 알람을 삭제하도록 요청합니다.
     */
    public void delete(Alarm alarm) {
        databaseExecutor.execute(() -> alarmDao.delete(alarm));
    }

    /**
     * 이 ViewModel이 더 이상 사용되지 않아 소멸될 때 호출됩니다.
     * 여기서 백그라운드 스레드를 정리해주어야 메모리 누수를 방지할 수 있습니다.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        databaseExecutor.shutdown();
    }
}
