package com.example.alarm;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * `AlarmManager`에 의해 예약된 시스템 알람(Broadcast)을 수신하는 클래스.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    // --- [기존 주석] 소리 중복 문제를 해결하기 위해, 두 개의 분리된 알림 채널 ID를 정의합니다. ---
    private static final String DEFAULT_SOUND_CHANNEL_ID = "alarm_channel_default_sound";
    private static final String DEFAULT_SOUND_CHANNEL_NAME = "기본 알람";

    private static final String CUSTOM_SOUND_CHANNEL_ID = "alarm_channel_custom_sound";
    private static final String CUSTOM_SOUND_CHANNEL_NAME = "사용자 지정 알람";

    // [주석] 이 상수는 더 이상 사용하지 않으므로, 삭제하거나 주석 처리해도 무방합니다.
    // public static final String RINGING_ALARM_ID_EXTRA = "com.example.alarm.RINGING_ALARM_ID_EXTRA";

    /**
     * [기존 주석] 알람 시간이 되면 시스템에 의해 이 메소드가 호출됩니다.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "알람 수신됨!");

        final int alarmId = intent.getIntExtra(MainActivity.ALARM_ID_EXTRA, -1);
        if (alarmId == -1) {
            Log.w(TAG, "유효하지 않은 알람 ID(-1)를 수신하여 작업을 중단합니다.");
            return;
        }

        // [기존 주석] 데이터베이스 작업은 메인 스레드에서 처리할 수 없으므로, 별도의 스레드에서 실행합니다.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());
            Alarm alarm = db.alarmDao().getAlarmByIdNonLive(alarmId);

            if (alarm != null) {
                handleCustomSoundAndVibration(context, alarm);
                triggerAlarmScreen(context, alarm);

                if (alarm.isRepeating()) {
                    Log.d(TAG, "알람 ID " + alarmId + "은(는) 반복 알람입니다. 다음 알람을 예약합니다.");
                    AlarmScheduler alarmScheduler = new AlarmScheduler(context);
                    alarmScheduler.schedule(alarm);
                } else {
                    Log.d(TAG, "알람 ID " + alarmId + "은(는) 단일 알람입니다. 알람을 비활성화합니다.");
                    // [핵심 수정] Alarm 클래스의 isEnabled 필드가 private으로 변경되었으므로, 공개된 setter 메소드를 사용해야 합니다.
                    alarm.setEnabled(false);
                    db.alarmDao().update(alarm);
                }

            } else {
                Log.w(TAG, "알람 ID " + alarmId + "에 해당하는 데이터를 데이터베이스에서 찾을 수 없습니다.");
            }
        });
        executor.shutdown();
    }

    /**
     * [기존 주석] 알람 객체의 설정에 따라 '사용자 지정' 알람음과 '사용자 지정' 진동을 처리합니다.
     */
    private void handleCustomSoundAndVibration(Context context, Alarm alarm) {
        if (alarm.getSoundUri() != null && !alarm.getSoundUri().isEmpty()) {
            Log.d(TAG, "사용자 지정 알람음이 있습니다. AlarmSoundService를 시작합니다. URI: " + alarm.getSoundUri());
            Intent serviceIntent = new Intent(context, AlarmSoundService.class);
            serviceIntent.putExtra("SOUND_URI", alarm.getSoundUri());
            context.startService(serviceIntent);
        } else {
            Log.d(TAG, "사용자 지정 알람음이 없습니다. 알림의 기본 소리를 사용합니다.");
        }

        if (alarm.isVibrationEnabled()) {
            Log.d(TAG, "진동 옵션이 활성화되어 있습니다. 진동을 시작합니다.");
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {500, 500};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0), new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        }
    }

    /**
     * [기존 주석] 알림을 생성하고 표시합니다.
     */
    private void triggerAlarmScreen(Context context, Alarm alarm) {
        createNotificationChannels(context);

        final boolean hasCustomSound = alarm.getSoundUri() != null && !alarm.getSoundUri().isEmpty();
        final String channelId = hasCustomSound ? CUSTOM_SOUND_CHANNEL_ID : DEFAULT_SOUND_CHANNEL_ID;
        Log.d(TAG, "사용할 알림 채널 ID: " + channelId);

        Intent fullScreenIntent = new Intent(context, AlarmRingingActivity.class);

        // --- [기존 주석] --- //
        fullScreenIntent.putExtra(MainActivity.ALARM_ID_EXTRA, alarm.getId());
        // -------------------- //

        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, alarm.getId(),
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String timeToDisplay = formatTime(context, alarm.getHour(), alarm.getMinute());
        String notificationText = "현재 울리는 알람: " + timeToDisplay;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("알람")
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        notificationManager.notify(alarm.getId(), builder.build());
        Log.d(TAG, "알람 ID " + alarm.getId() + "으로 전체 화면 알림을 성공적으로 표시했습니다.");
    }

    /**
     * [기존 주석] 앱에 필요한 모든 알림 채널을 미리 생성하는 메소드입니다.
     */
    private void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager == null) return;

            NotificationChannel defaultChannel = new NotificationChannel(DEFAULT_SOUND_CHANNEL_ID, DEFAULT_SOUND_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            defaultChannel.setDescription("기본 '띠리링' 소리가 나는 알람 채널입니다.");
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build();
            defaultChannel.setSound(alarmSound, audioAttributes);
            defaultChannel.enableVibration(false);

            NotificationChannel customChannel = new NotificationChannel(CUSTOM_SOUND_CHANNEL_ID, CUSTOM_SOUND_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            customChannel.setDescription("사용자가 선택한 음악이 재생될 때 사용되는 채널입니다 (알림 자체는 무음). ");
            customChannel.setSound(null, null);
            customChannel.enableVibration(false);

            notificationManager.createNotificationChannel(defaultChannel);
            notificationManager.createNotificationChannel(customChannel);
        }
    }

    private String formatTime(Context context, int hour, int minute) {
        if (DateFormat.is24HourFormat(context)) {
            return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            SimpleDateFormat sdf12 = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return sdf12.format(calendar.getTime());
        }
    }
}
