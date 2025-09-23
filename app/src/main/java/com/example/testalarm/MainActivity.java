package com.example.testalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast; // 사용자에게 간단한 메시지를 표시하기 위해 추가

import androidx.activity.result.ActivityResultLauncher; // Activity 결과를 받기 위해 추가
import androidx.activity.result.contract.ActivityResultContracts; // Activity 결과를 받기 위한 계약
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // 주석: UI 요소들을 위한 멤버 변수 선언
    private TextView currentTimeTextView; // 현재 시간을 표시할 TextView
    private TextView alarmStatusTextView; // 설정된 알람 상태를 표시할 TextView
    private Button cancelAlarmButton;     // 알람 취소 버튼
    private FloatingActionButton setAlarmFab; // 알람 설정 화면으로 이동하는 FAB

    // 주석: 시간 업데이트를 위한 Handler 및 Runnable
    private Handler timeHandler;
    private Runnable timeRunnable;

    // 주석: SharedPreferences를 사용하기 위한 상수 정의
    // SharedPreferences는 앱의 간단한 데이터를 키-값 쌍으로 저장하는 데 사용됩니다.
    public static final String ALARM_PREFERENCES = "AlarmPrefs"; // SharedPreferences 파일의 이름
    public static final String PREF_KEY_ALARM_TIME_MILLIS = "alarmTimeMillis"; // 알람 시간을 저장할 키

    // 주석: AlarmManager에서 사용할 요청 코드 (PendingIntent 식별용)
    public static final int ALARM_REQUEST_CODE = 1001;

    // 주석: SetAlarmActivity에서 결과를 받아오기 위한 ActivityResultLauncher
    // 이 방법은 deprecated된 onActivityResult를 대체하는 현대적인 방식입니다.
    private ActivityResultLauncher<Intent> setAlarmLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 주석: activity_main.xml 레이아웃 파일을 이 액티비티의 화면으로 설정합니다.
        setContentView(R.layout.activity_main);

        // 주석: XML 레이아웃의 UI 요소들과 멤버 변수를 연결합니다.
        currentTimeTextView = findViewById(R.id.currentTimeTextView);
        alarmStatusTextView = findViewById(R.id.alarmStatusTextView);
        cancelAlarmButton = findViewById(R.id.cancelAlarmButton);
        setAlarmFab = findViewById(R.id.setAlarmFab);

        // 주석: SetAlarmActivity 실행 및 결과 처리를 위한 ActivityResultLauncher 초기화
        // SetAlarmActivity가 종료되면 이 콜백이 호출됩니다.
        setAlarmLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 주석: SetAlarmActivity에서 어떤 결과로 종료되었는지 확인할 수 있지만,
                    // 이 간단한 앱에서는 onResume에서 알람 상태를 다시 로드하므로 특별한 처리는 하지 않습니다.
                    // 필요하다면 result.getResultCode() 와 result.getData()를 사용할 수 있습니다.
                    // 예를 들어, 알람이 성공적으로 설정되었는지 여부를 Intent로 받아올 수 있습니다.
                }
        );

        // 주석: 알람 설정 FAB 클릭 리스너 설정
        setAlarmFab.setOnClickListener(view -> {
            // 주석: SetAlarmActivity를 시작하기 위한 Intent 생성
            Intent intent = new Intent(MainActivity.this, SetAlarmActivity.class);
            // 주석: ActivityResultLauncher를 사용하여 SetAlarmActivity를 시작합니다.
            setAlarmLauncher.launch(intent);
        });

        // 주석: 알람 취소 버튼 클릭 리스너 설정
        cancelAlarmButton.setOnClickListener(view -> {
            cancelAlarm(); // 알람 취소 로직 호출
        });

        // 주석: 현재 시간 업데이트를 위한 Handler 및 Runnable 초기화
        timeHandler = new Handler(Looper.getMainLooper());
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                // 주석: 현재 시간을 HH:mm 형식으로 가져옵니다.
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String currentTime = sdf.format(new Date());
                currentTimeTextView.setText(currentTime); // TextView에 현재 시간 업데이트

                // 주석: 1초마다 이 Runnable을 다시 실행하도록 예약합니다.
                timeHandler.postDelayed(this, 1000);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 주석: 액티비티가 화면에 다시 나타날 때마다 현재 시간 업데이트 시작
        timeHandler.post(timeRunnable);
        // 주석: 저장된 알람 상태를 확인하고 UI를 업데이트합니다.
        updateAlarmStatusUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 주석: 액티비티가 화면에서 사라질 때 현재 시간 업데이트 중지 (배터리 절약)
        timeHandler.removeCallbacks(timeRunnable);
    }

    /**
     * 주석: SharedPreferences에 저장된 알람 정보를 바탕으로 UI(알람 상태 텍스트, 취소 버튼 표시 여부)를 업데이트합니다.
     */
    private void updateAlarmStatusUI() {
        // 주석: "AlarmPrefs"라는 이름의 SharedPreferences 인스턴스를 가져옵니다.
        // MODE_PRIVATE은 이 앱 내에서만 접근 가능하도록 설정합니다.
        SharedPreferences prefs = getSharedPreferences(ALARM_PREFERENCES, Context.MODE_PRIVATE);
        // 주석: "alarmTimeMillis" 키로 저장된 알람 시간을 가져옵니다. 저장된 값이 없으면 0을 반환합니다.
        long alarmTimeMillis = prefs.getLong(PREF_KEY_ALARM_TIME_MILLIS, 0);

        if (alarmTimeMillis > 0) {
            // 주석: 알람이 설정되어 있는 경우
            Calendar alarmCalendar = Calendar.getInstance();
            alarmCalendar.setTimeInMillis(alarmTimeMillis);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String alarmTimeFormatted = sdf.format(alarmCalendar.getTime());

            // 주석: strings.xml에 정의된 포맷 문자열을 사용하여 알람 시간 표시
            alarmStatusTextView.setText(getString(R.string.alarm_status_alarm_set_at, alarmTimeFormatted));
            cancelAlarmButton.setVisibility(View.VISIBLE); // 알람 취소 버튼 보이기
        } else {
            // 주석: 설정된 알람이 없는 경우
            alarmStatusTextView.setText(R.string.alarm_status_no_alarm_set);
            cancelAlarmButton.setVisibility(View.GONE); // 알람 취소 버튼 숨기기
        }
    }

    /**
     * 주석: 설정된 알람을 취소하고 SharedPreferences에서 알람 정보를 제거합니다.
     */
    private void cancelAlarm() {
        // 주석: AlarmManager 시스템 서비스를 가져옵니다.
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        // 주석: 알람을 설정할 때 사용했던 것과 동일한 Intent 및 PendingIntent를 생성해야 취소할 수 있습니다.
        Intent intent = new Intent(this, AlarmReceiver.class); // AlarmReceiver를 타겟으로 하는 Intent
        // 주석: PendingIntent.FLAG_IMMUTABLE은 PendingIntent가 변경 불가능하도록 설정합니다.
        // 안드로이드 12 (API 31) 이상에서는 PendingIntent의 mutability를 명시해야 합니다.
        // FLAG_NO_CREATE는 기존 PendingIntent가 없으면 새로 생성하지 않고 null을 반환하도록 합니다.
        // 여기서는 취소 목적이므로, 동일한 PendingIntent를 찾기 위해 사용합니다. 만약 생성도 고려한다면 0 또는 FLAG_UPDATE_CURRENT 등을 사용합니다.
        // 취소를 위해서는 FLAG_NO_CREATE 보다는 0이나 FLAG_UPDATE_CURRENT가 더 적합할 수 있습니다. 여기서는 확실한 취소를 위해 PendingIntent를 새로 만듭니다.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT // 기존 것이 있으면 업데이트, 없으면 새로 생성
        );

        if (alarmManager != null && pendingIntent != null) {
            alarmManager.cancel(pendingIntent); // AlarmManager에 등록된 알람 취소
            pendingIntent.cancel(); // PendingIntent 자체도 취소 (선택적이지만 권장)
        }

        // 주석: SharedPreferences에서 알람 시간 정보를 제거합니다.
        SharedPreferences prefs = getSharedPreferences(ALARM_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PREF_KEY_ALARM_TIME_MILLIS); // "alarmTimeMillis" 키와 값 제거
        editor.apply(); // 변경사항 비동기적으로 저장

        // 주석: UI 업데이트
        updateAlarmStatusUI();
        Toast.makeText(this, "알람이 취소되었습니다.", Toast.LENGTH_SHORT).show();
    }
}
