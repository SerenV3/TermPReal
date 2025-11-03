package com.example.alarm;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UI(Activity/Fragment)를 위한 데이터를 제공하고, UI의 생명주기로부터 데이터를 보존하는 클래스.
 * UI 컨트롤러(예: MainActivity)와 데이터 소스(Repository 또는 DAO) 사이의 중개자 역할을 합니다.
 *
 * `AndroidViewModel`을 상속받는 이유:
 *  - `ViewModel`은 보통 생성자에서 Context를 직접 참조하면 안 됩니다. (Activity의 Context는 생명주기에 따라 소멸되므로 메모리 누수 발생)
 *  - 하지만 데이터베이스 인스턴스를 얻는 등 `Application`의 컨텍스트가 꼭 필요한 경우가 있습니다.
 *  - `AndroidViewModel`은 생명주기로부터 안전한 `Application` 컨텍스트를 생성자를 통해 제공해 주므로,
 *    이러한 경우에 안심하고 사용할 수 있습니다.
 */
public class AlarmViewModel extends AndroidViewModel {

    // 데이터베이스에 접근하기 위한 DAO(Data Access Object)
    private final AlarmDao alarmDao;

    // 데이터베이스의 모든 알람 목록을 관찰(observe) 가능한 형태로 들고 있는 LiveData.
    // 이 LiveData는 데이터베이스에 변경이 있을 때마다 UI에 자동으로 새로운 데이터를 전달합니다.
    private final LiveData<List<Alarm>> allAlarms;

    // [추가] 새로 생성된 알람의 ID를 UI 컨트롤러에 전달하기 위한 LiveData.
    // MutableLiveData는 값을 변경할 수 있는 LiveData입니다.
    private final MutableLiveData<Long> newAlarmId = new MutableLiveData<>();

    // 데이터베이스 I/O(입출력) 작업을 백그라운드 스레드에서 실행하기 위한 ExecutorService.
    private final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    public AlarmViewModel(@NonNull Application application) {
        super(application);
        // AppDatabase의 싱글톤 인스턴스를 가져옵니다.
        AppDatabase db = AppDatabase.getDatabase(application);
        // 데이터베이스 인스턴스를 통해 DAO 인터페이스의 구현체를 얻습니다.
        this.alarmDao = db.alarmDao();
        // DAO를 통해 모든 알람 목록을 가져와 LiveData 멤버 변수에 할당합니다.
        this.allAlarms = alarmDao.getAllAlarms();
    }

    /**
     * UI 컨트롤러(Activity/Fragment)가 데이터베이스의 모든 알람 목록을 구독(observe)할 수 있도록
     * LiveData 객체를 외부에 제공합니다.
     *
     * @return 모든 알람 목록을 담고 있는 LiveData
     */
    public LiveData<List<Alarm>> getAllAlarms() {
        return allAlarms;
    }

    /**
     * [새로운 메소드] '수정 모드'에서 특정 ID의 알람 정보를 가져오기 위한 메소드
     * @param alarmId 조회할 알람의 고유 ID
     * @return 해당 알람 정보를 담고 있는 LiveData<Alarm> 객체
     */
    public LiveData<Alarm> getAlarmById(int alarmId) {
        // [핵심 수정] DAO에 있는 동일한 이름의 메소드를 호출하여 결과를 반환합니다.
        return alarmDao.getAlarmById(alarmId);
    }

    /**
     * [추가] 새로 생성된 알람의 ID를 관찰하기 위한 LiveData를 외부에 제공합니다.
     * @return 새로 생성된 알람의 ID를 담는 LiveData
     */
    public LiveData<Long> getNewAlarmId() {
        return newAlarmId;
    }

    /**
     * [수정] 새로운 알람을 데이터베이스에 삽입하고, 생성된 ID를 newAlarmId LiveData에 전달합니다.
     * 실제 작업은 백그라운드 스레드에서 실행됩니다.
     *
     * @param alarm 삽입할 Alarm 객체
     */
    public void insert(Alarm alarm) {
        databaseWriteExecutor.execute(() -> {
            // alarmDao.insert(alarm)은 이제 새로 생성된 row의 ID(long 타입)를 반환합니다.
            long id = alarmDao.insert(alarm);
            // postValue()는 백그라운드 스레드에서 LiveData의 값을 안전하게 업데이트하는 데 사용됩니다.
            newAlarmId.postValue(id);
        });
    }

    /**
     * 기존 알람 정보를 업데이트하도록 요청합니다.
     * 실제 작업은 백그라운드 스레드에서 실행됩니다.
     *
     * @param alarm 업데이트할 Alarm 객체
     */
    public void update(Alarm alarm) {
        databaseWriteExecutor.execute(() -> {
            alarmDao.update(alarm);
        });
    }

    /**
     * 특정 알람을 삭제하도록 요청합니다.
     * 실제 작업은 백그라운드 스레드에서 실행됩니다.
     *
     * @param alarm 삭제할 Alarm 객체
     */
    public void delete(Alarm alarm) {
        databaseWriteExecutor.execute(() -> {
            alarmDao.delete(alarm);
        });
    }

    /**
     * 이 ViewModel이 더 이상 사용되지 않아 소멸될 때 호출되는 콜백 메소드입니다.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        // ExecutorService를 정상적으로 종료시킵니다.
        databaseWriteExecutor.shutdown();
    }
}
