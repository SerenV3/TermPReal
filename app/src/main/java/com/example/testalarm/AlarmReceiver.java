package com.example.testalarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Toast; // 간단한 피드백을 위해 추가

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {

    // 주석: 알림 채널 ID는 앱 내에서 고유해야 합니다.
    private static final String ALARM_NOTIFICATION_CHANNEL_ID = "alarm_notification_channel";
    // 주석: 알림 ID도 앱 내에서 (또는 특정 유형의 알림에 대해) 고유해야 합니다.
    private static final int ALARM_NOTIFICATION_ID = 123;

    /**
     * 주석: 이 메소드는 AlarmManager가 알람을 실행할 때 호출됩니다.
     * @param context BroadcastReceiver가 실행되는 Context입니다.
     * @param intent 이 BroadcastReceiver를 트리거한 Intent입니다. (AlarmManager가 보낸 Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // 주석: 사용자에게 알람이 울렸음을 알리는 간단한 토스트 메시지를 표시합니다. (주로 디버깅 또는 간단한 피드백용)
        Toast.makeText(context, "알람 시간입니다!", Toast.LENGTH_LONG).show();

        // 주석: 알람 알림(Notification)을 표시합니다.
        sendAlarmNotification(context);

        // 주석: 알람이 울린 후 SharedPreferences에서 해당 알람 정보를 제거합니다. (일회성 알람 처리)
        // 이렇게 하면 MainActivity에서 알람 상태 UI가 "설정된 알람 없음"으로 업데이트됩니다.
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.ALARM_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(MainActivity.PREF_KEY_ALARM_TIME_MILLIS);
        editor.apply();

        // 주석: (선택 사항) 만약 반복 알람을 구현한다면, 여기서 다음 알람을 다시 설정할 수 있습니다.
        // 이 간단한 앱에서는 일회성 알람이므로, 여기서는 추가 설정 로직이 없습니다.
    }

    /**
     * 주석: 사용자에게 알람이 울렸음을 알리는 시스템 알림(Notification)을 생성하고 표시합니다.
     * @param context 알림을 생성하고 표시하는 데 필요한 Context입니다.
     */
    private void sendAlarmNotification(Context context) {
        // 주석: NotificationManager 시스템 서비스를 가져옵니다. 알림을 관리하는 역할을 합니다.
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 주석: 안드로이드 Oreo (API 26) 이상에서는 알림을 표시하기 전에 반드시 NotificationChannel을 생성하고 등록해야 합니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 주석: 알림 채널의 이름과 중요도를 설정합니다.
            // 이름은 사용자에게 설정 화면에 표시될 수 있습니다.
            // 중요도는 알림이 사용자에게 어떻게 방해를 줄지 결정합니다 (소리, 진동, 화면 팝업 등).
            CharSequence channelName = context.getString(R.string.alarm_notification_channel_name); // strings.xml에서 가져옴
            String channelDescription = context.getString(R.string.alarm_notification_channel_description); // strings.xml에서 가져옴
            int importance = NotificationManager.IMPORTANCE_HIGH; // 높은 중요도 (소리와 함께 헤드업 알림으로 표시될 수 있음)

            NotificationChannel channel = new NotificationChannel(ALARM_NOTIFICATION_CHANNEL_ID, channelName, importance);
            channel.setDescription(channelDescription);
            // 주석: (선택 사항) 채널에 대한 추가 설정을 할 수 있습니다 (예: 진동 패턴, 라이트 색상 등).
            // channel.enableLights(true);
            // channel.setLightColor(Color.RED);
            // channel.enableVibration(true);
            // channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

            // 주석: 생성된 NotificationChannel을 NotificationManager에 등록합니다.
            // 이 작업은 앱 시작 시 한 번만 수행해도 되지만, 여기서 호출해도 문제가 없습니다.
            // 이미 생성된 채널을 다시 생성하려고 하면 아무 작업도 수행하지 않습니다.
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // 주석: 알림을 클릭했을 때 MainActivity를 시작하는 Intent를 생성합니다.
        Intent notificationIntent = new Intent(context, MainActivity.class);
        // 주석: 액티비티 스택을 관리하기 위한 플래그입니다.
        // FLAG_ACTIVITY_NEW_TASK: 새로운 태스크에서 액티비티를 시작합니다 (BroadcastReceiver에서 액티비티를 시작할 때 필요할 수 있음).
        // FLAG_ACTIVITY_CLEAR_TASK: 이 액티비티를 시작하기 전에 기존 태스크를 모두 제거합니다 (앱의 메인 화면으로 돌아갈 때 유용).
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // 주석: 위에서 만든 Intent를 실행할 PendingIntent를 생성합니다.
        // 요청 코드(0)는 이 PendingIntent를 식별하는 데 사용될 수 있습니다.
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0, // 요청 코드
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT // 플래그
        );

        // 주석: NotificationCompat.Builder를 사용하여 알림을 구성합니다.
        // NotificationCompat는 이전 안드로이드 버전과의 호환성을 제공합니다.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ALARM_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // 주석: 알림에 표시될 작은 아이콘 (앱 아이콘 등)
                                                                 // TODO: ic_alarm 같은 적절한 알람 아이콘으로 교체하는 것이 좋습니다. (res/drawable에 추가 필요)
                .setContentTitle(context.getString(R.string.alarm_notification_title)) // 주석: 알림의 제목 (strings.xml에서 가져옴)
                .setContentText(context.getString(R.string.alarm_notification_text)) // 주석: 알림의 내용 (strings.xml에서 가져옴)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // 주석: 알림의 중요도 (Oreo 이전 버전용)
                .setContentIntent(pendingIntent) // 주석: 사용자가 알림을 탭했을 때 실행될 PendingIntent
                .setAutoCancel(true); // 주석: 사용자가 알림을 탭하면 알림이 자동으로 사라지도록 설정

        // 주석: (선택 사항) 알림에 소리, 진동 등을 추가할 수 있습니다.
        // .setDefaults(NotificationCompat.DEFAULT_ALL); // 기본 소리, 진동, 빛 사용
        // .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)); // 기본 알람 소리 사용 (권한 필요할 수 있음)
        // .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000}); // 진동 패턴

        // 주석: NotificationManager를 사용하여 알림을 시스템에 표시합니다.
        // ALARM_NOTIFICATION_ID는 이 알림을 식별하는 고유 ID입니다. 나중에 이 ID를 사용하여 알림을 업데이트하거나 취소할 수 있습니다.
        if (notificationManager != null) {
            notificationManager.notify(ALARM_NOTIFICATION_ID, builder.build());
        }
    }
}
