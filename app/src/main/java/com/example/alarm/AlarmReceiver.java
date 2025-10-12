package com.example.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log; // 로그 확인용

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "alarm_channel_id";
    private static final String CHANNEL_NAME = "Alarm Channel";
    private static final String CHANNEL_DESCRIPTION = "Channel for alarm notifications";
    private static final int NOTIFICATION_ID_OFFSET = 1000; // 알림 ID 충돌 방지를 위한 오프셋

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmReceiver", "Alarm received!"); // 로그 추가

        // MainActivity에서 전달받은 알람 ID 가져오기
        // 기본값 -1은 유효하지 않은 ID를 의미합니다.
        int alarmId = intent.getIntExtra(MainActivity.ALARM_ID_EXTRA, -1);

        if (alarmId != -1) {
            Log.d("AlarmReceiver", "Alarm ID: " + alarmId);
            // 알림 채널 생성 (API 26 이상에서만 필요)
            createNotificationChannel(context);

            // 알림 클릭 시 MainActivity를 열도록 Intent 설정
            Intent notificationIntent = new Intent(context, MainActivity.class);
            // 기존 MainActivity가 있다면 उसको foreground로 가져오도록 플래그 설정
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            // 알림 클릭 시 어떤 알람에서 왔는지 알리기 위해 알람 ID를 전달할 수도 있습니다 (선택 사항)
            // notificationIntent.putExtra(MainActivity.ALARM_ID_EXTRA, alarmId);

            PendingIntent pendingIntent = PendingIntent.getActivity(context,
                    alarmId, // 각 알림에 대한 PendingIntent가 고유하도록 requestCode에 alarmId 사용
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // 알림 내용 설정 (DB에서 알람 시간 등을 가져와서 표시할 수 있습니다)
            // 여기서는 간단히 "알람!"으로 표시
            // 실제 앱에서는 AlarmViewModel 등을 통해 해당 alarmId의 상세 정보를 가져와서 표시하는 것이 좋습니다.
            String notificationTitle = "알람!";
            String notificationText = "설정하신 알람 시간이 되었습니다. (ID: " + alarmId + ")";


            // 알림 빌더 생성
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground) // 알림 아이콘 (ic_launcher_foreground는 예시, 실제 아이콘으로 변경 필요)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationText)
                    .setPriority(NotificationCompat.PRIORITY_HIGH) // 높은 우선순위 (헤드업 알림으로 표시될 수 있음)
                    .setContentIntent(pendingIntent) // 알림 클릭 시 실행될 PendingIntent
                    .setAutoCancel(true); // 알림 클릭 시 자동으로 알림 제거

            // 알림 표시
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            // 각 알림이 고유 ID를 갖도록 alarmId를 사용 (또는 오프셋 추가)
            // 주의: Android 12 (API 31) 이상에서는 포그라운드 서비스에서 알림을 시작해야 하는 경우가 있습니다.
            //       정확한 시간에 알림을 표시하려면 추가적인 권한 및 설정이 필요할 수 있습니다.
            try {
                notificationManager.notify(NOTIFICATION_ID_OFFSET + alarmId, builder.build());
                Log.d("AlarmReceiver", "Notification sent for alarm ID: " + alarmId);
            } catch (SecurityException e) {
                Log.e("AlarmReceiver", "Failed to send notification due to SecurityException. Check POST_NOTIFICATIONS permission.", e);
                // Android 13 (API 33) 이상에서는 POST_NOTIFICATIONS 권한이 필요합니다.
                // 사용자에게 권한 요청 로직이 필요할 수 있습니다.
            }

        } else {
            Log.w("AlarmReceiver", "Invalid Alarm ID received.");
        }

        // TODO: 알람이 울린 후 처리 (예: 반복 알람이 아니라면 데이터베이스에서 isEnabled = false로 업데이트 등)
        // 이 부분은 AlarmViewModel을 통해 백그라운드 스레드에서 처리해야 할 수 있습니다.
        // 예를 들어, 해당 알람을 비활성화 하려면:
        // AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());
        // AlarmDao dao = db.alarmDao();
        // Executors.newSingleThreadExecutor().execute(() -> {
        //     Alarm alarmToUpdate = dao.getAlarmByIdNonLive(alarmId); // LiveData가 아닌 직접 객체를 가져오는 메소드 필요
        //     if (alarmToUpdate != null) {
        //         alarmToUpdate.isEnabled = false;
        //         dao.update(alarmToUpdate);
        //     }
        // });
    }

    // 알림 채널 생성 메소드 (API 26, Oreo 이상에서 필요)
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // 높은 중요도
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            // 채널을 시스템에 등록
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d("AlarmReceiver", "Notification channel created.");
            }
        }
    }
}
