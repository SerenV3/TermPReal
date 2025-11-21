package com.example.alarm;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * [기존 주석] 알람이 울릴 때 잠금화면 위로 나타나는 전용 Activity 입니다.
 * 이 화면의 주된 역할은 사용자에게 알람이 울리고 있음을 알리고, 알람을 해제할 수 있는 버튼을 제공하는 것입니다.
 */
public class AlarmRingingActivity extends AppCompatActivity {

    private static final String TAG = "AlarmRingingActivity";

    private TextView currentTimeTextView;
    // [기존 주석] 알람 이름을 표시할 TextView를 멤버 변수로 선언합니다.
    private TextView alarmNameTextView;
    private Button dismissButton;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_ringing);
        Log.d(TAG, "알람 울림 화면 생성됨.");

        // --- [기존 주석] 1. 잠금화면 위로 Activity를 표시하기 위한 설정 --- //
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null)
                keyguardManager.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        // --- [기존 주석] 2. UI 요소 초기화 및 설정 --- //
        currentTimeTextView = findViewById(R.id.currentTimeTextView);
        // [기존 주석] XML 레이아웃의 ringing_alarm_name TextView를 코드와 연결합니다.
        alarmNameTextView = findViewById(R.id.ringing_alarm_name);
        dismissButton = findViewById(R.id.dismissButton);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // [기존 주석] 현재 시간을 TextView에 표시합니다.
        SimpleDateFormat sdf = new SimpleDateFormat("a h:mm", Locale.getDefault());
        currentTimeTextView.setText(sdf.format(new Date()));

        // --- [핵심 수정] 3. 알람 이름 처리 로직 최종 보강 --- //
        // [새로운 주석] AlarmReceiver로부터 전달받은 Intent에서 알람 ID를 가져옵니다.
        int alarmId = getIntent().getIntExtra(MainActivity.ALARM_ID_EXTRA, -1);
        // [새로운 디버깅 주석] Receiver로부터 어떤 알람 ID를 받았는지 확인하기 위해 Logcat에 명확히 기록합니다.
        // 이 로그를 통해 데이터 전달의 첫 단계가 성공했는지 바로 확인할 수 있습니다.
        Log.d(TAG, "Intent로부터 전달받은 알람 ID: " + alarmId);

        if (alarmId != -1) {
            // [기존 주석] 유효한 ID가 있다면, ViewModel을 통해 데이터베이스에서 해당 알람 정보를 가져옵니다.
            AlarmViewModel alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
            alarmViewModel.getAlarmById(alarmId).observe(this, alarm -> {
                if (alarm != null) {
                    // [기존 주석] 알람 정보를 성공적으로 가져왔을 때, 이름을 UI에 표시하는 메소드를 호출합니다.
                    displayAlarmName(alarm.getName());
                    // [기존 주석] 한 번만 데이터를 가져오면 되므로, 더 이상 관찰할 필요가 없습니다.
                    alarmViewModel.getAlarmById(alarmId).removeObservers(this);
                } else {
                    // [새로운 디버깅 주석] ID는 올바르게 전달받았지만, 데이터베이스에서 해당 ID의 알람을 찾지 못한 경우에 대한 로그입니다.
                    // 이 로그가 보인다면, 알람이 DB에서 삭제되었거나 다른 문제가 있음을 의미합니다.
                    Log.w(TAG, "ID " + alarmId + "에 해당하는 알람을 DB에서 찾지 못했습니다.");
                }
            });
        } else {
            // [새로운 예외 처리 주석] 애초에 유효하지 않은 ID(-1)를 전달받은 경우에 대한 처리입니다.
            // 이 경우, 알람 이름을 표시할 수 없으므로 TextView를 숨깁니다.
            Log.w(TAG, "유효하지 않은 알람 ID(-1)를 전달받았습니다. 알람 이름을 표시할 수 없습니다.");
            displayAlarmName(null);
        }

        // --- [기존 주석] 4. "알람 해제" 버튼 클릭 리스너 설정 --- //
        dismissButton.setOnClickListener(v -> {
            Log.d(TAG, "\'알람 해제\' 버튼 클릭됨.");
            dismissAlarm();
        });

        // --- [기존 주석] 5. 뒤로 가기 버튼 처리 --- //
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "뒤로가기 이벤트 감지됨. 알람 해제.");
                dismissAlarm();
            }
        });
    }

    /**
     * [기존 주석] 전달받은 알람 이름을 TextView에 표시하거나, 이름이 없으면 숨기는 역할을 합니다.
     * @param alarmName 데이터베이스에서 가져온 알람의 이름. null일 수 있습니다.
     */
    private void displayAlarmName(String alarmName) {
        // [기존 주석] TextUtils.isEmpty()는 문자열이 null이거나, 빈 문자열("")일 경우 true를 반환합니다.
        if (!TextUtils.isEmpty(alarmName)) {
            // [기존 주석] 알람 이름이 있다면, TextView에 이름을 설정하고 화면에 보이도록 합니다.
            alarmNameTextView.setText(alarmName);
            alarmNameTextView.setVisibility(View.VISIBLE);
            Log.d(TAG, "알람 이름 표시: " + alarmName);
        } else {
            // [기존 주석] 알람 이름이 없다면, TextView를 화면에서 완전히 숨겨 공간을 차지하지 않게 합니다.
            alarmNameTextView.setVisibility(View.GONE);
            Log.d(TAG, "표시할 알람 이름이 없음.");
        }
    }

    /**
     * [기존 주석] 알람을 완전히 해제하는 메소드.
     */
    private void dismissAlarm() {
        Intent stopServiceIntent = new Intent(this, AlarmSoundService.class);
        stopService(stopServiceIntent);
        Log.d(TAG, "AlarmSoundService 중지 명령 전송.");

        if (vibrator != null) {
            vibrator.cancel();
            Log.d(TAG, "진동 중지.");
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "알람 울림 화면 소멸됨.");
    }
}
