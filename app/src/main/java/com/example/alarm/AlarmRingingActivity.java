package com.example.alarm;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * [새 클래스] 알람이 울릴 때 잠금화면 위로 나타나는 전용 Activity 입니다.
 * 이 화면의 주된 역할은 사용자에게 알람이 울리고 있음을 알리고, 알람을 해제할 수 있는 버튼을 제공하는 것입니다.
 */
public class AlarmRingingActivity extends AppCompatActivity {

    private static final String TAG = "AlarmRingingActivity";

    private TextView currentTimeTextView;
    private Button dismissButton;
    private Vibrator vibrator; // 진동을 멈추기 위해 필요합니다.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_ringing);
        Log.d(TAG, "알람 울림 화면 생성됨.");

        // --- 1. 잠금화면 위로 Activity를 표시하기 위한 설정 --- //
        // 이 설정들은 안드로이드 버전 및 기기 정책에 따라 동작이 다를 수 있습니다.
        // 잠금 화면 위로 창을 표시하도록 설정합니다.
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

        // --- 2. UI 요소 초기화 및 설정 --- //
        currentTimeTextView = findViewById(R.id.currentTimeTextView);
        dismissButton = findViewById(R.id.dismissButton);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // 현재 시간을 TextView에 표시합니다.
        // '오전/오후 h:mm' 형식 (예: 오후 3:30)으로 보여줍니다.
        SimpleDateFormat sdf = new SimpleDateFormat("a h:mm", Locale.getDefault());
        currentTimeTextView.setText(sdf.format(new Date()));

        // --- 3. "알람 해제" 버튼 클릭 리스너 설정 --- //
        dismissButton.setOnClickListener(v -> {
            Log.d(TAG, "'알람 해제' 버튼 클릭됨.");
            dismissAlarm();
        });

        // --- 4. [새로운 내용] 뒤로 가기 버튼 처리를 위한 최신 방식 (OnBackPressedDispatcher) --- //
        // [새 주석] 사용자가 지적한 대로, 기존의 onBackPressed()는 최신 안드로이드에서 제스처에 대해 동작하지 않을 수 있습니다.
        // AndroidX의 OnBackPressedDispatcher를 사용하는 것이 권장되는 방식이며,
        // 사용자가 뒤로 가기 제스처를 하거나, 하단의 네비게이션 바의 뒤로 가기 버튼을 눌렀을 때의 동작을 일관되게 처리할 수 있습니다.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // [새 주석] 뒤로 가기 이벤트가 감지되면, 알람 해제 로직을 실행합니다.
                Log.d(TAG, "뒤로가기 이벤트 감지됨(최신 방식). 알람 해제.");
                dismissAlarm();
            }
        });
    }

    /**
     * 알람을 완전히 해제하는 메소드.
     * 음악 재생 서비스와 진동을 모두 중지하고, 현재 화면을 닫습니다.
     */
    private void dismissAlarm() {
        // 1. 음악 재생 서비스(AlarmSoundService)를 중지시킵니다.
        //    Intent를 사용하여 어떤 서비스를 중지할지 명시적으로 지정합니다.
        Intent stopServiceIntent = new Intent(this, AlarmSoundService.class);
        stopService(stopServiceIntent);
        Log.d(TAG, "AlarmSoundService 중지 명령 전송.");

        // 2. 진동(Vibrator)을 중지시킵니다.
        if (vibrator != null) {
            vibrator.cancel();
            Log.d(TAG, "진동 중지.");
        }

        // 3. 현재 Activity를 종료하여 화면을 닫습니다.
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "알람 울림 화면 소멸됨.");
    }
}
