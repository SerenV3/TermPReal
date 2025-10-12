package com.example.alarm;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

public class SetAlarmActivity extends AppCompatActivity {

    private TimePicker timePicker;
    private Button saveAlarmButton;
    private AlarmViewModel alarmViewModel;
    private Button cancelButton; // 취소 버튼 변수 선언

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_alarm);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setAlarm), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ViewModel 인스턴스 가져오기
        alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);

        // UI 요소 초기화
        timePicker = findViewById(R.id.timePicker);
        saveAlarmButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton); // cancelButton을 XML과 연결하는 코드 추가

        // TimePicker를 12시간 형식 (AM/PM 표시)으로 설정
        timePicker.setIs24HourView(false);

        // 저장 버튼 클릭 리스너 설정
        saveAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAlarm();
            }
        });

        // 취소 버튼 클릭 리스너 설정
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 현재 액티비티를 종료
            }
        });
    }

    private void saveAlarm() {
        int hour;
        int minute;

        hour = timePicker.getHour();
        minute = timePicker.getMinute();

        Alarm newAlarm = new Alarm(hour, minute, true);
        alarmViewModel.insert(newAlarm);

        Toast.makeText(this, String.format("%02d:%02d 알람이 저장되었습니다.", hour, minute), Toast.LENGTH_SHORT).show();
        finish();
    }
}
