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

        Calendar alarmCalendar = Calendar.getInstance();
        alarmCalendar.set(Calendar.HOUR_OF_DAY, hour);
        alarmCalendar.set(Calendar.MINUTE, minute);
        alarmCalendar.set(Calendar.SECOND, 0);
        alarmCalendar.set(Calendar.MILLISECOND, 0);

        if (alarmCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
            alarmCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                MainActivity.ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

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
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }
}
