package com.example.testalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build; // 추가된 import
import android.os.Bundle;
import android.provider.Settings; // 추가된 import
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class SetAlarmActivity extends AppCompatActivity {

    private TimePicker timePicker;
    private Button saveButton;
    private Button cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_alarm);

        timePicker = findViewById(R.id.timePicker);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        timePicker.setIs24HourView(true);

        saveButton.setOnClickListener(view -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();
            saveAlarm(hour, minute);
            // 권한 요청 후 바로 finish() 되는 것을 방지하기 위해,
            // saveAlarm 내부에서 실제 알람이 설정되었을 때만 finish() 하거나,
            // 또는 사용자가 권한 설정 후 돌아왔을 때의 흐름을 고려해야 합니다.
            // 지금은 단순화를 위해 saveAlarm 호출 후 finish()를 그대로 둡니다.
            // 실제 앱에서는 onActivityResult 또는 ActivityResultLauncher를 사용하여
            // 설정 화면에서 돌아온 결과를 처리하는 것이 좋습니다.
        });

        cancelButton.setOnClickListener(view -> {
            finish();
        });
    }

    private void saveAlarm(int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // API 31 (Android 12) 이상 버전에 대한 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "정확한 알람 설정을 위해 권한이 필요합니다. 설정 화면으로 이동합니다.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                // 권한이 없으므로 알람 설정 로직을 진행하지 않고 종료
                // 사용자가 권한을 부여하고 돌아오면 다시 알람 설정을 시도해야 함
                return;
            }
        }

        Calendar alarmCalendar = Calendar.getInstance(); //java.util.calendar
        alarmCalendar.set(Calendar.HOUR_OF_DAY, hour);
        alarmCalendar.set(Calendar.MINUTE, minute);
        alarmCalendar.set(Calendar.SECOND, 0);
        alarmCalendar.set(Calendar.MILLISECOND, 0);

        if (alarmCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
            alarmCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast( // AlarmManager가 특정 시간이 되었을 때 브로드캐스트(방송)할 수 있는 PendingIntent를 생성
                this,
                MainActivity.ALARM_REQUEST_CODE, //1001
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT //보안상의 이유로 Android 12 (API 레벨 31) 이상을 타겟팅하는 앱에서는 PendingIntent를 생성할 때 FLAG_IMMUTABLE 또는 FLAG_MUTABLE 중 하나를 반드시 명시.FLAG_IMMUTABLE을 사용하는 것이 보안상 권장
        );                                                                      //이 플래그는 시스템에 이미 동일한 PendingIntent(동일한 context, requestCode, 그리고 intent.filterEquals()로 비교했을 때 동일한 내부 Intent)가 존재할 경우의 동작을 결정

        if (alarmManager != null) {
            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(alarmCalendar.getTimeInMillis(), getMainActivityPendingIntent());
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);

            SharedPreferences prefs = getSharedPreferences(MainActivity.ALARM_PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(MainActivity.PREF_KEY_ALARM_TIME_MILLIS, alarmCalendar.getTimeInMillis());
            editor.apply();

            Toast.makeText(this, "알람이 " + hour + "시 " + minute + "분에 설정되었습니다.", Toast.LENGTH_SHORT).show();
            finish(); // 알람이 성공적으로 설정된 후 액티비티 종료
        } else {
            Toast.makeText(this, "알람 매니저를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private PendingIntent getMainActivityPendingIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //사용자가 알람 UI를 통해 앱으로 돌아올 때, 앱이 이전에 어떤 상태였든 상관없이 깔끔하게 MainActivity부터 시작하도록 하는 효과가 있습니다. 예를 들어, 사용자가 MainActivity -> SomeOtherActivity -> 홈 화면으로 나간 상태에서 알람 UI를 통해 앱을 다시 실행하면, SomeOtherActivity는 제거되고 MainActivity가 표시
        return PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }
}
