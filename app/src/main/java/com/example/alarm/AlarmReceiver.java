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
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID = "alarm_channel_id";
    private static final String CHANNEL_NAME = "Alarm Channel";
    public static final String RINGING_ALARM_ID_EXTRA = "com.example.alarm.RINGING_ALARM_ID_EXTRA";

    /**
     * [수정] 알람 시간이 되면 시스템에 의해 이 메소드가 호출됩니다.
     * 알람을 울리고, 알림을 표시하며, 만약 반복 알람이라면 다음 알람을 예약하는 로직이 추가됩니다.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "알람 수신됨!");

        final int alarmId = intent.getIntExtra(MainActivity.ALARM_ID_EXTRA, -1);
        if (alarmId == -1) {
            Log.w(TAG, "유효하지 않은 알람 ID(-1)를 수신하여 작업을 중단합니다.");
            return;
        }

        final AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());
        // 데이터베이스 작업은 메인 스레드에서 처리할 수 없으므로, 별도의 스레드에서 실행합니다.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // 전달받은 ID를 사용하여 데이터베이스에서 현재 울린 알람의 전체 정보를 가져옵니다.
            Alarm alarm = db.alarmDao().getAlarmByIdNonLive(alarmId);

            if (alarm != null) {
                // 1. 알림 및 화면 켜짐 로직을 실행합니다.
                triggerAlarmScreen(context, alarm);

                // 2. [수정] 알람의 종류에 따라 다음 동작을 결정합니다.
                if (alarm.isRepeating()) {
                    // --- 반복 알람인 경우 ---
                    // AlarmScheduler를 다시 호출하여, 이 알람 객체를 그대로 전달합니다.
                    // 그러면 AlarmScheduler는 이 알람의 다음 반복 시간을 계산하여 다시 예약할 것입니다.
                    // 이것이 바로 "연쇄 반응" 로직의 핵심입니다.
                    Log.d(TAG, "알람 ID " + alarmId + "은(는) 반복 알람입니다. 다음 알람을 예약합니다.");
                    AlarmScheduler alarmScheduler = new AlarmScheduler(context);
                    alarmScheduler.schedule(alarm);
                } else {
                    // --- 반복되지 않는 단일 알람인 경우 ---
                    // 알람이 한 번 울렸으므로, 데이터베이스에서 이 알람의 상태를 '비활성(disabled)'으로 업데이트합니다.
                    // 이렇게 해야 메인 화면의 토글 스위치가 꺼진 상태로 올바르게 표시됩니다.
                    Log.d(TAG, "알람 ID " + alarmId + "은(는) 단일 알람입니다. 알람을 비활성화합니다.");
                    alarm.isEnabled = false;
                    db.alarmDao().update(alarm);
                }

            } else {
                Log.w(TAG, "알람 ID " + alarmId + "에 해당하는 데이터를 데이터베이스에서 찾을 수 없습니다.");
            }
        });
        executor.shutdown();
    }

    private void triggerAlarmScreen(Context context, Alarm alarm) {
        createNotificationChannel(context, alarm.isVibrationEnabled());

        Intent fullScreenIntent = new Intent(context, MainActivity.class);
        fullScreenIntent.putExtra(RINGING_ALARM_ID_EXTRA, alarm.getId());
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, alarm.getId(),
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String timeToDisplay = formatTime(context, alarm.getHour(), alarm.getMinute());
        String notificationText = "현재 울리는 알람: " + timeToDisplay;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("알람")
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        notificationManager.notify(alarm.getId(), builder.build());
        Log.d(TAG, "알람 ID " + alarm.getId() + "으로 전체 화면 알림을 성공적으로 표시했습니다.");
    }

    private void createNotificationChannel(Context context, boolean enableVibration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("알람을 위한 알림 채널");

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            channel.setSound(alarmSound, audioAttributes);

            if (enableVibration) {
                channel.setVibrationPattern(new long[]{0, 500, 500, 500});
                channel.enableVibration(true);
            } else {
                channel.enableVibration(false);
            }

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
