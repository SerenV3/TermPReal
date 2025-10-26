package com.example.alarm;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

/**
 * 새로운 알람을 설정하는 화면(Activity).
 */
public class SetAlarmActivity extends AppCompatActivity {

    private TimePicker timePicker;
    private Button saveAlarmButton;
    private Button cancelButton;
    private SwitchMaterial vibrationSwitch;
    private AlarmViewModel alarmViewModel;
    private AlarmScheduler alarmScheduler;
    private Vibrator vibrator;

    // [추가] 요일 선택을 위한 ToggleButton 변수들
    private ToggleButton mondayButton, tuesdayButton, wednesdayButton, thursdayButton, fridayButton, saturdayButton, sundayButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_alarm);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setAlarm), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
        alarmScheduler = new AlarmScheduler(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        setupViews();
        setupListeners();
        observeNewAlarmId();
    }

    /**
     * [수정] XML 레이아웃에 정의된 UI 뷰들을 찾아와 멤버 변수에 할당합니다.
     * 요일 선택 버튼들이 추가되었습니다.
     */
    private void setupViews() {
        timePicker = findViewById(R.id.timePicker);
        saveAlarmButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        vibrationSwitch = findViewById(R.id.vibrationSwitch);
        timePicker.setIs24HourView(false);

        // [수정] mondayButton이 잘못된 ID를 참조하고 있던 버그를 수정합니다.
        mondayButton = findViewById(R.id.mondayToggle);
        tuesdayButton = findViewById(R.id.tuesdayToggle);
        wednesdayButton = findViewById(R.id.wednesdayToggle);
        thursdayButton = findViewById(R.id.thursdayToggle);
        fridayButton = findViewById(R.id.fridayToggle);
        saturdayButton = findViewById(R.id.saturdayToggle);
        sundayButton = findViewById(R.id.sundayToggle);
    }

    private void setupListeners() {
        saveAlarmButton.setOnClickListener(v -> saveAlarm());
        cancelButton.setOnClickListener(v -> finish());

        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                playDefaultVibration();
            }
        });
    }

    private void playDefaultVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(500);
            }
        }
    }

    /**
     * [수정] ViewModel의 newAlarmId LiveData를 관찰하여, ID가 생성되면 알람을 예약합니다.
     * 사용자가 선택한 요일 정보를 포함하여 Alarm 객체를 생성하고 스케줄러에 전달합니다.
     */
    private void observeNewAlarmId() {
        alarmViewModel.getNewAlarmId().observe(this, newAlarmId -> {
            if (newAlarmId != null) {
                int alarmId = newAlarmId.intValue();
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();
                boolean isVibrationEnabled = vibrationSwitch.isChecked();

                // [수정] 사용자가 선택한 요일 버튼들의 상태(checked)를 모두 읽어옵니다.
                Alarm alarmToSchedule = new Alarm(
                        alarmId,
                        hour, minute, true, isVibrationEnabled,
                        mondayButton.isChecked(),
                        tuesdayButton.isChecked(),
                        wednesdayButton.isChecked(),
                        thursdayButton.isChecked(),
                        fridayButton.isChecked(),
                        saturdayButton.isChecked(),
                        sundayButton.isChecked()
                );

                alarmScheduler.schedule(alarmToSchedule);

                Toast.makeText(this, String.format(Locale.getDefault(), "%s %02d:%02d 알람이 저장되었습니다.", (hour < 12 ? "오전" : "오후"), (hour == 0 || hour == 12) ? 12 : hour % 12, minute), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * [수정] 사용자가 선택한 시간과 요일 정보로 새로운 알람을 데이터베이스에 저장하도록 ViewModel에 요청합니다.
     */
    private void saveAlarm() {
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        boolean isVibrationEnabled = vibrationSwitch.isChecked();

        // 1. [수정] Alarm.java에 새로 추가한 생성자를 사용하여 Alarm 객체를 생성합니다.
        //    TimePicker에서 선택한 시간, 진동 스위치 상태, 그리고 각 요일 버튼의 선택 여부를 모두 전달합니다.
        Alarm newAlarm = new Alarm(
                hour, minute, true, isVibrationEnabled,
                mondayButton.isChecked(),
                tuesdayButton.isChecked(),
                wednesdayButton.isChecked(),
                thursdayButton.isChecked(),
                fridayButton.isChecked(),
                saturdayButton.isChecked(),
                sundayButton.isChecked()
        );

        // 2. ViewModel에게 이 새로운 알람 객체의 삽입(저장)을 요청합니다.
        //    ViewModel은 내부적으로 Room 데이터베이스에 이 데이터를 저장할 것입니다.
        alarmViewModel.insert(newAlarm);
    }
}
