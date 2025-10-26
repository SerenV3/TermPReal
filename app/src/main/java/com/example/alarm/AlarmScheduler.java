package com.example.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

/**
 * 시스템의 AlarmManager를 사용하여 실제 알람을 예약(schedule)하고 취소하는 역할을 담당하는 클래스.
 */
public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";
    private final Context context;
    private final AlarmManager alarmManager;

    public AlarmScheduler(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * [수정] 전달된 Alarm 객체를 기반으로 알람을 예약합니다.
     * 이 메소드는 이제 알람이 반복되는지 여부를 확인하고, 그에 따라 다른 예약 로직을 처리합니다.
     * @param alarm 예약할 알람 객체. 시간, 활성화 여부, 그리고 이제 반복 요일 정보를 포함합니다.
     */
    public void schedule(Alarm alarm) {
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager를 가져올 수 없습니다. 스케줄링을 중단합니다.");
            return;
        }

        // 알람이 울릴 시간을 나타내는 Calendar 객체를 생성합니다.
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        calendar.set(Calendar.MINUTE, alarm.getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // 1. 알람이 반복되는지 여부를 확인합니다.
        if (alarm.isRepeating()) {
            // --- 반복 알람 처리 로직 ---
            scheduleRepeatingAlarm(alarm, calendar);
        } else {
            // --- 반복되지 않는 단일 알람 처리 로직 (기존 로직과 동일) ---
            // 만약 설정한 시간이 현재 시간보다 이전이라면, 알람을 내일 날짜로 설정합니다.
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            scheduleSingleAlarm(alarm.getId(), calendar.getTimeInMillis());
        }
    }

    /**
     * [추가] 반복 알람을 스케줄링하는 새로운 메소드입니다.
     * 오늘부터 시작하여, 사용자가 선택한 다음 반복 요일에 알람을 설정합니다.
     * @param alarm 예약할 반복 알람 객체
     * @param baseCalendar 사용자가 설정한 시간(시, 분) 정보가 담긴 Calendar 객체
     */
    private void scheduleRepeatingAlarm(Alarm alarm, Calendar baseCalendar) {
        // 오늘 요일을 Calendar 상수(일요일=1, 월요일=2, ...)로 가져옵니다.
        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        // 사용자가 설정한 알람 시간이 현재 시간보다 이전인지 여부를 확인합니다.
        boolean isTimePassed = baseCalendar.getTimeInMillis() <= System.currentTimeMillis();

        // 가장 가까운 다음 알람 요일을 찾기 위한 변수, -1은 아직 못찾았음을 의미합니다.
        int nextAlarmDay = -1;

        // 1. 가장 가까운 다음 알람 요일 찾기
        // 오늘부터 시작하여 7일 동안 반복하며, 사용자가 선택한 가장 가까운 요일을 찾습니다.
        for (int i = 0; i < 7; i++) {
            int dayToFind = (today + i - 1) % 7 + 1; // 오늘, 내일, 모레... 순서로 요일을 확인 (1=일, 2=월, ...)

            // 오늘이고 아직 시간이 지나지 않았다면, 오늘부터 확인합니다.
            // 오늘인데 시간이 이미 지났다면, 내일부터 확인해야 하므로 i=0 (오늘) 경우는 건너뜁니다.
            if (i == 0 && isTimePassed) {
                continue;
            }

            // isDaySelected 메소드를 호출하여, 현재 확인 중인 요일(dayToFind)을 사용자가 선택했는지 확인합니다.
            if (isDaySelected(alarm, dayToFind)) {
                nextAlarmDay = dayToFind; // 찾았다! 해당 요일을 저장하고
                baseCalendar.add(Calendar.DAY_OF_YEAR, i); // Calendar에 i일 만큼 더해서 날짜를 맞춘 뒤
                break; // 반복문을 탈출합니다.
            }
        }

        // 2. 만약 위 로직에서 다음 알람 요일을 찾지 못했다면 (예: 오늘 월요일, 선택된 요일도 월요일인데 시간이 지난 경우)
        // 이 경우는 다음 주로 넘어가야 하므로, 가장 처음 선택된 요일을 다시 찾아서 7일을 더해줍니다.
        if (nextAlarmDay == -1) {
            for (int i = 0; i < 7; i++) {
                int dayToFind = (today + i - 1) % 7 + 1;
                if (isDaySelected(alarm, dayToFind)) {
                    baseCalendar.add(Calendar.DAY_OF_YEAR, i + 7); // i일에 7일을 추가로 더해서 다음 주로 설정
                    break;
                }
            }
        }

        // 3. 최종 계산된 시간에 알람을 예약합니다.
        scheduleSingleAlarm(alarm.getId(), baseCalendar.getTimeInMillis());
    }

    /**
     * [추가] 특정 요일 상수가 Alarm 객체에서 선택되었는지 확인하는 헬퍼 메소드입니다.
     * @param alarm 확인할 알람 객체
     * @param dayOfWeek Calendar의 요일 상수 (예: Calendar.MONDAY)
     * @return 해당 요일이 선택되었으면 true, 아니면 false
     */
    private boolean isDaySelected(Alarm alarm, int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY: return alarm.isMondayEnabled();
            case Calendar.TUESDAY: return alarm.isTuesdayEnabled();
            case Calendar.WEDNESDAY: return alarm.isWednesdayEnabled();
            case Calendar.THURSDAY: return alarm.isThursdayEnabled();
            case Calendar.FRIDAY: return alarm.isFridayEnabled();
            case Calendar.SATURDAY: return alarm.isSaturdayEnabled();
            case Calendar.SUNDAY: return alarm.isSundayEnabled();
            default: return false;
        }
    }

    /**
     * [수정] 특정 시간에 단일 알람을 예약하는 메소드입니다. (내부적으로만 사용)
     * Android 12 이상에서는 정확한 알람 예약 권한을 확인하는 로직이 추가되었습니다.
     * @param alarmId 예약할 알람의 고유 ID
     * @param triggerAtMillis 알람이 울릴 시간 (밀리초 단위)
     */
    private void scheduleSingleAlarm(int alarmId, long triggerAtMillis) {
        PendingIntent pendingIntent = createPendingIntent(alarmId);

        // [추가] Android 12 (API 31, S) 이상에서는 정확한 알람을 예약할 수 있는 권한이 있는지 확인해야 합니다.
        // 이 권한이 없으면 SecurityException이 발생하여 앱이 강제 종료됩니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // 권한이 없는 경우, 사용자에게 알리고 스케줄링을 중단합니다.
                // 실제 상용 앱에서는 사용자를 권한 설정 화면으로 안내하는 로직을 추가하는 것이 좋습니다.
                Log.e(TAG, "정확한 알람을 예약할 수 있는 권한이 없습니다.");
                Toast.makeText(context, "정확한 알람 권한이 필요합니다. 앱 설정에서 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
                return; // 여기서 실행을 중단하여 Exception을 방지합니다.
            }
        }

        // 안드로이드 버전에 따라 정확한 알람을 설정하는 방법이 다릅니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Doze 모드에서도 알람이 울리도록 setExactAndAllowWhileIdle 사용
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }

        Log.d(TAG, "알람 ID " + alarmId + "이(가) " + triggerAtMillis + " 시간에 예약되었습니다.");
        Toast.makeText(context, "알람이 예약되었습니다.", Toast.LENGTH_SHORT).show();
    }

    /**
     * 알람을 취소합니다.
     * @param alarm 취소할 알람 객체
     */
    public void cancel(Alarm alarm) {
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager를 가져올 수 없습니다. 취소를 중단합니다.");
            return;
        }
        PendingIntent pendingIntent = createPendingIntent(alarm.getId());
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "알람 ID " + alarm.getId() + "이(가) 취소되었습니다.");
    }

    /**
     * 알람을 위한 PendingIntent를 생성합니다.
     * @param alarmId 알람의 고유 ID
     * @return 생성된 PendingIntent
     */
    private PendingIntent createPendingIntent(int alarmId) {
        // AlarmReceiver에게 알람 이벤트를 전달할 Intent를 생성합니다.
        Intent intent = new Intent(context, AlarmReceiver.class);
        // 알람 ID를 Intent에 추가하여, 수신 측에서 어떤 알람이 울렸는지 식별할 수 있도록 합니다.
        intent.putExtra(MainActivity.ALARM_ID_EXTRA, alarmId);

        // 동일한 알람 ID에 대해서는 동일한 PendingIntent가 사용되도록 FLAG_UPDATE_CURRENT를 사용합니다.
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 안드로이드 6.0 이상에서는 PendingIntent의 불변성을 명시해주는 것이 좋습니다.
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, alarmId, intent, flags);
    }
}
