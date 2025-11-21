package com.example.alarm;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Locale;

/**
 * [기존 주석, 내용 추가] 데이터베이스의 'alarms' 테이블과 직접 매핑되는 클래스입니다.
 */
@Entity(tableName = "alarms")
public class Alarm {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "hour")
    private int hour;

    @ColumnInfo(name = "minute")
    private int minute;

    @ColumnInfo(name = "is_enabled")
    private boolean isEnabled;

    @ColumnInfo(name = "is_vibration_enabled")
    private boolean isVibrationEnabled;

    @ColumnInfo(name = "sound_uri")
    private String soundUri;

    @ColumnInfo(name = "is_monday_enabled")
    private boolean isMondayEnabled;
    @ColumnInfo(name = "is_tuesday_enabled")
    private boolean isTuesdayEnabled;
    @ColumnInfo(name = "is_wednesday_enabled")
    private boolean isWednesdayEnabled;
    @ColumnInfo(name = "is_thursday_enabled")
    private boolean isThursdayEnabled;
    @ColumnInfo(name = "is_friday_enabled")
    private boolean isFridayEnabled;
    @ColumnInfo(name = "is_saturday_enabled")
    private boolean isSaturdayEnabled;
    @ColumnInfo(name = "is_sunday_enabled")
    private boolean isSundayEnabled;

    @ColumnInfo(name = "is_weather_tts_enabled")
    private boolean isWeatherTtsEnabled;


    // [새로운 내용] 수정 모드에서 DB 데이터를 불러올 때 Room 라이브러리가 사용하는 생성자입니다.
    // isWeatherTtsEnabled 필드를 포함하도록 수정되었습니다.
    public Alarm(int id, String name, int hour, int minute, boolean isEnabled, boolean isVibrationEnabled, String soundUri,
                 boolean isMondayEnabled, boolean isTuesdayEnabled, boolean isWednesdayEnabled, boolean isThursdayEnabled,
                 boolean isFridayEnabled, boolean isSaturdayEnabled, boolean isSundayEnabled, boolean isWeatherTtsEnabled) {
        this.id = id;
        this.name = name;
        this.hour = hour;
        this.minute = minute;
        this.isEnabled = isEnabled;
        this.isVibrationEnabled = isVibrationEnabled;
        this.soundUri = soundUri;
        this.isMondayEnabled = isMondayEnabled;
        this.isTuesdayEnabled = isTuesdayEnabled;
        this.isWednesdayEnabled = isWednesdayEnabled;
        this.isThursdayEnabled = isThursdayEnabled;
        this.isFridayEnabled = isFridayEnabled;
        this.isSaturdayEnabled = isSaturdayEnabled;
        this.isSundayEnabled = isSundayEnabled;
        this.isWeatherTtsEnabled = isWeatherTtsEnabled;
    }

    // [새로운 내용] 새 알람을 생성할 때 사용하는 생성자입니다. id는 자동으로 생성되므로 포함하지 않습니다.
    @Ignore
    public Alarm(String name, int hour, int minute, boolean isEnabled, boolean isVibrationEnabled, String soundUri,
                 boolean isMondayEnabled, boolean isTuesdayEnabled, boolean isWednesdayEnabled, boolean isThursdayEnabled,
                 boolean isFridayEnabled, boolean isSaturdayEnabled, boolean isSundayEnabled, boolean isWeatherTtsEnabled) {
        this.name = name;
        this.hour = hour;
        this.minute = minute;
        this.isEnabled = isEnabled;
        this.isVibrationEnabled = isVibrationEnabled;
        this.soundUri = soundUri;
        this.isMondayEnabled = isMondayEnabled;
        this.isTuesdayEnabled = isTuesdayEnabled;
        this.isWednesdayEnabled = isWednesdayEnabled;
        this.isThursdayEnabled = isThursdayEnabled;
        this.isFridayEnabled = isFridayEnabled;
        this.isSaturdayEnabled = isSaturdayEnabled;
        this.isSundayEnabled = isSundayEnabled;
        this.isWeatherTtsEnabled = isWeatherTtsEnabled;
    }

    // --- Getter 및 Setter --- //

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getHour() { return hour; }
    public int getMinute() { return minute; }
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }
    public boolean isVibrationEnabled() { return isVibrationEnabled; }
    public String getSoundUri() { return soundUri; }
    public boolean isMondayEnabled() { return isMondayEnabled; }
    public boolean isTuesdayEnabled() { return isTuesdayEnabled; }
    public boolean isWednesdayEnabled() { return isWednesdayEnabled; }
    public boolean isThursdayEnabled() { return isThursdayEnabled; }
    public boolean isFridayEnabled() { return isFridayEnabled; }
    public boolean isSaturdayEnabled() { return isSaturdayEnabled; }
    public boolean isSundayEnabled() { return isSundayEnabled; }
    public boolean isWeatherTtsEnabled() { return isWeatherTtsEnabled; }

    // --- [새로운 헬퍼 메소드] UI 표시를 위한 데이터 가공 --- //

    /**
     * [새로운 메소드] 시간을 기준으로 "오전" 또는 "오후"를 반환합니다.
     * @return "오전" (hour < 12) 또는 "오후" (hour >= 12)
     */
    public String getAmPm() {
        return (hour < 12) ? "오전" : "오후";
    }

    /**
     * [새로운 메소드] 24시간 형식의 시간을 UI에 표시하기 좋은 12시간 형식(예: 1:23)으로 변환합니다.
     * @return 12시간 형식으로 변환된 시간 문자열
     */
    public String getFormattedTime() {
        int displayHour = (hour == 0 || hour == 12) ? 12 : hour % 12;
        return String.format(Locale.getDefault(), "%d:%02d", displayHour, minute);
    }

    /**
     * [새로운 메소드] 반복 요일이 하나라도 설정되어 있는지 확인합니다.
     * @return 요일 반복이 하나라도 켜져 있으면 true, 아니면 false
     */
    public boolean isRepeating() {
        return isSundayEnabled || isMondayEnabled || isTuesdayEnabled || isWednesdayEnabled || isThursdayEnabled || isFridayEnabled || isSaturdayEnabled;
    }
}
