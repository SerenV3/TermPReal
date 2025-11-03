package com.example.alarm;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * 알람이 울릴 때 실제 음악을 백그라운드에서 재생하는 역할을 담당하는 Service 입니다.
 * Service는 Activity와 달리 화면(UI)이 없는 상태에서도 동작할 수 있는 앱 구성요소입니다.
 *
 * 이 Service는 AlarmReceiver에 의해 시작되며, 전달받은 음악 파일(URI)을 재생합니다.
 */
public class AlarmSoundService extends Service {

    private static final String TAG = "AlarmSoundService";

    // 음악 재생을 위한 안드로이드의 핵심 클래스
    private MediaPlayer mediaPlayer;

    // Service가 생성될 때 한 번만 호출됩니다.
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "서비스 생성됨.");
    }

    /**
     * 외부(예: AlarmReceiver)에서 startService()를 호출할 때마다 실행되는 메소드입니다.
     * 이 메소드에서 실질적인 음악 재생 로직이 시작됩니다.
     * @param intent startService() 호출 시 전달된 Intent 객체. 알람음 URI 등의 데이터를 포함합니다.
     * @param flags 추가적인 플래그.
     * @param startId 이 요청을 식별하는 고유 ID.
     * @return 서비스가 시스템에 의해 강제 종료되었을 때 어떻게 동작할지를 결정합니다.
     *         START_NOT_STICKY는 강제 종료 후 자동으로 재시작하지 않음을 의미합니다.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "서비스 시작됨.");

        String soundUriString = null;
        if (intent != null) {
            // AlarmReceiver로부터 전달받은 알람음 URI 문자열을 꺼냅니다.
            soundUriString = intent.getStringExtra("SOUND_URI");
        }

        try {
            if (soundUriString != null) {
                // 전달받은 URI 문자열이 있는 경우 (사용자가 알람음을 선택한 경우)
                Log.d(TAG, "사용자 지정 알람음 재생: " + soundUriString);
                Uri soundUri = Uri.parse(soundUriString);

                // 만약 이전에 재생중인 음악이 있었다면, 깨끗이 정리하고 새로 시작합니다.
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }

                mediaPlayer = new MediaPlayer();
                // 안드로이드 10 이상에서는 포그라운드 서비스 타입 명시가 권장되지만,
                // 알람 기능의 특수성을 고려하여 일단은 이대로 진행합니다.
                mediaPlayer.setDataSource(this, soundUri);
                mediaPlayer.setLooping(true); // 음악을 무한 반복합니다.
                mediaPlayer.prepareAsync(); // 비동기적으로 음악 파일을 준비합니다. (네트워크 스트리밍 등에서도 유용)
                mediaPlayer.setOnPreparedListener(mp -> {
                    Log.d(TAG, "음악 준비 완료. 재생 시작.");
                    mp.start(); // 준비가 완료되면 재생을 시작합니다.
                });

            } else {
                // TODO: 사용자가 알람음을 선택하지 않은 경우, 기본 시스템 알람음을 재생하는 로직을 추가할 수 있습니다.
                Log.d(TAG, "사용자 지정 알람음 없음. 기본 알람음 재생 필요.");
            }
        } catch (Exception e) {
            Log.e(TAG, "알람음 재생 중 오류 발생", e);
            // 오류 발생 시, 서비스를 스스로 중지하여 리소스를 정리합니다.
            stopSelf();
        }

        // START_NOT_STICKY : 서비스가 강제로 종료되어도 시스템이 자동으로 재시작하지 않습니다.
        // 알람은 정확한 시간에 한 번만 울리면 되므로, 이 설정이 적합합니다.
        return START_NOT_STICKY;
    }

    /**
     * 서비스가 소멸(종료)될 때 호출됩니다.
     * 여기서 MediaPlayer 등의 리소스를 반드시 해제해야 메모리 누수를 막을 수 있습니다.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            // MediaPlayer 객체가 사용하던 모든 리소스를 해제합니다.
            mediaPlayer.release();
            mediaPlayer = null;
            Log.d(TAG, "서비스 소멸됨. MediaPlayer 리소스 해제 완료.");
        }
    }

    /**
     * 이 서비스는 다른 컴포넌트와 연결(bind)하여 사용하는 방식이 아니므로, null을 반환합니다.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
