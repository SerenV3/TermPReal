package com.example.alarm;

import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.Locale;

/**
 * 새로운 알람을 설정하는 화면(Activity).
 * 사용자는 이 화면에서 TimePicker를 사용해 시간을 선택하고,
 * '저장' 또는 '취소' 버튼을 통해 작업을 완료하거나 취소할 수 있습니다.
 */
public class SetAlarmActivity extends AppCompatActivity {

    private TimePicker timePicker;
    private Button saveAlarmButton;
    private Button cancelButton;
    private AlarmViewModel alarmViewModel;

    /**
     * Activity가 생성될 때 호출되는 메소드.
     * UI 레이아웃을 설정하고, 뷰(View)들을 초기화하며, 이벤트 리스너를 설정하는 등
     * Activity의 초기화 작업을 수행합니다.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // activity_set_alarm.xml 파일을 이 Activity의 UI로 설정합니다.
        setContentView(R.layout.activity_set_alarm);

        // 시스템 바(상태 바, 네비게이션 바)와 앱 컨텐츠가 겹치지 않도록 패딩을 조정하는 코드.
        // Edge-to-Edge 디스플레이를 지원하기 위함입니다.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setAlarm), (v, insets) -> {
            // [오류 수정] 원래의 올바른 코드로 복원
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 이 Activity와 생명주기를 함께하는 AlarmViewModel 인스턴스를 가져옵니다.
        // ViewModelProvider가 ViewModel의 생성을 관리하여, 화면 회전 등에도 데이터가 보존되도록 합니다.
        alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);

        // UI 뷰들을 초기화하는 메소드 호출
        setupViews();

        // 버튼들의 클릭 이벤트를 처리하는 리스너를 설정하는 메소드 호출
        setupListeners();
    }

    /**
     * XML 레이아웃에 정의된 UI 뷰들을 찾아와 멤버 변수에 할당하는 메소드.
     * 코드의 구조를 명확하게 하기 위해 초기화 로직을 별도의 메소드로 분리했습니다.
     */
    private void setupViews() {
        timePicker = findViewById(R.id.timePicker);
        saveAlarmButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        // TimePicker를 24시간제가 아닌 12시간제(AM/PM 표시)로 설정합니다.
        timePicker.setIs24HourView(false);
    }

    /**
     * '저장'과 '취소' 버튼의 클릭 리스너를 설정하는 메소드.
     * 람다(Lambda) 표현식을 사용하여 코드를 더 간결하게 작성했습니다.
     */
    private void setupListeners() {
        // 저장 버튼 클릭 시 saveAlarm() 메소드를 호출합니다.
        saveAlarmButton.setOnClickListener(v -> saveAlarm());

        // 취소 버튼 클릭 시 finish() 메소드를 호출하여 현재 Activity를 종료합니다.
        cancelButton.setOnClickListener(v -> finish());
    }

    /**
     * 사용자가 선택한 시간으로 새로운 알람을 데이터베이스에 저장하는 메소드.
     */
    private void saveAlarm() {
        // 1. TimePicker에서 현재 선택된 시간과 분을 가져옵니다.
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        // 2. 가져온 시간 정보로 새로운 Alarm 객체를 생성합니다.
        //    (id는 Room이 자동으로 생성하므로, 여기서는 id가 없는 생성자를 사용합니다.)
        //    새로 만드는 알람이므로 활성화 상태(isEnabled)는 true로 설정합니다.
        Alarm newAlarm = new Alarm(hour, minute, true);

        // 3. ViewModel에게 새로운 알람을 데이터베이스에 삽입하도록 요청합니다.
        //    ViewModel은 이 작업을 백그라운드 스레드에서 안전하게 처리합니다.
        alarmViewModel.insert(newAlarm);

        // 4. 사용자에게 알람이 저장되었음을 Toast 메시지로 알려줍니다.
        Toast.makeText(this, String.format(Locale.getDefault(), "%02d:%02d 알람이 저장되었습니다.", hour, minute), Toast.LENGTH_SHORT).show();

        // 5. 모든 작업이 완료되었으므로, finish()를 호출하여 현재 Activity를 종료하고
        //    이전 화면(MainActivity)으로 돌아갑니다.
        finish();
    }
}
