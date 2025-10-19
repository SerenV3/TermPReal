package com.example.alarm;

import android.Manifest;
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
 * 앱이 실행 중이지 않은 상태에서도 시스템이 이 클래스의 onReceive()를 호출하여,
 * 지정된 시간에 알람 울림(알림 표시)과 같은 백그라운드 작업을 수행할 수 있습니다.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    private static final String CHANNEL_ID = "alarm_channel_id";
    private static final String CHANNEL_NAME = "Alarm Channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "알람 수신됨!");

        final int alarmId = intent.getIntExtra(MainActivity.ALARM_ID_EXTRA, -1);
        if (alarmId == -1) {
            Log.w(TAG, "유효하지 않은 알람 ID(-1)를 수신하여 작업을 중단합니다.");
            return;
        }

        final AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            Alarm alarm = db.alarmDao().getAlarmByIdNonLive(alarmId);

            if (alarm != null) {
                showNotification(context, alarm);

                if (alarm.isEnabled) {
                    alarm.isEnabled = false;
                    db.alarmDao().update(alarm);
                    Log.d(TAG, "알람 ID " + alarmId + "가 울린 후 데이터베이스에서 비활성화 처리됨.");
                }
            } else {
                Log.w(TAG, "알람 ID " + alarmId + "에 해당하는 데이터를 데이터베이스에서 찾을 수 없습니다.");
            }
        });
        executor.shutdown();
    }

    private void showNotification(Context context, Alarm alarm) {
        createNotificationChannel(context);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                alarm.getId(),
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String timeToDisplay = formatTime(context, alarm.getHour(), alarm.getMinute());
        String notificationText = "설정하신 " + timeToDisplay + " 알람이 울렸습니다.";

        // NotificationCompat.Builder에서는 소리를 직접 설정하지 않고, 채널의 설정을 따르도록 합니다.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("알람")
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "알림 표시 실패: POST_NOTIFICATIONS 권한이 없습니다.");
            return;
        }

        notificationManager.notify(alarm.getId(), builder.build());
        Log.d(TAG, "알림 ID " + alarm.getId() + "으로 알림을 성공적으로 표시했습니다.");
    }

    /**
     * 알림 채널(Notification Channel)을 생성하는 메소드.
     * [수정] 알람 소리, 진동, 중요도 등 알람에 특화된 설정을 채널에 모두 지정합니다.
     */
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = CHANNEL_NAME;
            String description = "알람을 위한 알림 채널";
            // [수정] 중요도를 IMPORTANCE_HIGH로 설정하여 헤드업 알림이 표시되도록 합니다.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // [수정] AudioAttributes: 이 채널의 소리 타입을 '알람'으로 명확히 지정합니다.
            // 이렇게 하면 사용자가 방해금지 모드를 설정했더라도 알람 소리는 울릴 수 있습니다.
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM) // 사용 목적을 '알람'으로 설정
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION) // 타입은 알림음
                    .build();

            // [추정] 기본 알람 소리를 가져옵니다.
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            // [수정] 채널에 소리와 AudioAttributes를 함께 설정합니다.
            channel.setSound(alarmSound, audioAttributes);

            // [추가] 진동 패턴을 설정합니다. (0ms 대기, 500ms 진동, 500ms 대기, 500ms 진동...)
            channel.setVibrationPattern(new long[]{0, 500, 500, 500});
            channel.enableVibration(true);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
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
