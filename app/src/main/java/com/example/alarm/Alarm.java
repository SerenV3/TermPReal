package com.example.alarm;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Locale;

/**
 * 알람 하나의 데이터를 표현하는 클래스 (데이터 모델).
 * 이 클래스는 Room 데이터베이스의 'alarms' 테이블과 1:1로 매핑됩니다.
 */
@Entity(tableName = "alarms")
public class Alarm {

    /**
     * 알람의 고유 ID. Room에 의해 자동으로 생성됩니다.
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "hour")
    private int hour;

    @ColumnInfo(name = "minute")
    private int minute;

    @ColumnInfo(name = "is_enabled")
    public boolean isEnabled;

    @ColumnInfo(name = "is_vibration_enabled")
    private boolean isVibrationEnabled;

    // --- [추가] 알람음 기능 구현을 위한 필드 ---
    // 사용자가 선택한 음악 파일의 경로(URI)를 문자열 형태로 저장합니다.
    // 알람음을 선택하지 않을 수도 있으므로, null 값을 허용해야 합니다. (@Nullable)
    @Nullable
    @ColumnInfo(name = "sound_uri")
    private String soundUri;

    // --- [기존] 요일 반복 기능 구현을 위한 필드들 ---
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

    /**
     * [수정] Room 데이터베이스가 모든 정보를 포함하여 객체를 생성할 때 사용하는 기본 생성자입니다.
     * 알람음 경로를 저장하는 soundUri 필드가 새롭게 추가되었습니다.
     * Room은 데이터베이스의 각 행(row)을 Alarm 객체로 변환할 때 이 생성자를 호출합니다.
     */
    public Alarm(int id, int hour, int minute, boolean isEnabled, boolean isVibrationEnabled, @Nullable String soundUri,
                 boolean isMondayEnabled, boolean isTuesdayEnabled, boolean isWednesdayEnabled,
                 boolean isThursdayEnabled, boolean isFridayEnabled, boolean isSaturdayEnabled,
                 boolean isSundayEnabled) {
        this.id = id;
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
    }

    /**
     * [수정] SetAlarmActivity에서 새로운 알람을 생성할 때 사용할 편의 생성자입니다.
     * soundUri 필드가 추가되었습니다.
     * @Ignore 어노테이션이 있으므로 Room은 데이터베이스 작업에 이 생성자를 사용하지 않습니다.
     */
    @Ignore
    public Alarm(int hour, int minute, boolean isEnabled, boolean isVibrationEnabled, @Nullable String soundUri,
                 boolean isMondayEnabled, boolean isTuesdayEnabled, boolean isWednesdayEnabled,
                 boolean isThursdayEnabled, boolean isFridayEnabled, boolean isSaturdayEnabled,
                 boolean isSundayEnabled) {
        // id는 Room이 자동으로 생성하므로, 여기서는 0으로 초기화하고 기본 생성자를 호출합니다.
        this(0, hour, minute, isEnabled, isVibrationEnabled, soundUri, isMondayEnabled, isTuesdayEnabled, isWednesdayEnabled, isThursdayEnabled, isFridayEnabled, isSaturdayEnabled, isSundayEnabled);
    }

    /**
     * [유지] 반복이 없는 단순 알람을 생성할 때 사용하는 편의 생성자입니다.
     * 하위 호환성을 위해 유지되며, soundUri는 기본값인 null로 설정됩니다.
     */
    @Ignore
    public Alarm(int hour, int minute, boolean isEnabled, boolean isVibrationEnabled) {
        this(0, hour, minute, isEnabled, isVibrationEnabled, null, false, false, false, false, false, false, false);
    }

    // --- Getter 및 Setter 메소드 ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getHour() { return hour; }
    public int getMinute() { return minute; }
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }
    public boolean isVibrationEnabled() { return isVibrationEnabled; }
    public void setVibrationEnabled(boolean vibrationEnabled) { isVibrationEnabled = vibrationEnabled; }

    // --- [추가] 알람음 필드에 대한 Getter 및 Setter ---
    @Nullable
    public String getSoundUri() { return soundUri; }
    public void setSoundUri(@Nullable String soundUri) { this.soundUri = soundUri; }

    // --- [기존] 요일 반복 필드에 대한 Getter 및 Setter ---
    public boolean isMondayEnabled() { return isMondayEnabled; }
    public void setMondayEnabled(boolean mondayEnabled) { isMondayEnabled = mondayEnabled; }
    public boolean isTuesdayEnabled() { return isTuesdayEnabled; }
    public void setTuesdayEnabled(boolean tuesdayEnabled) { isTuesdayEnabled = tuesdayEnabled; }
    public boolean isWednesdayEnabled() { return isWednesdayEnabled; }
    public void setWednesdayEnabled(boolean wednesdayEnabled) { isWednesdayEnabled = wednesdayEnabled; }
    public boolean isThursdayEnabled() { return isThursdayEnabled; }
    public void setThursdayEnabled(boolean thursdayEnabled) { isThursdayEnabled = thursdayEnabled; }
    public boolean isFridayEnabled() { return isFridayEnabled; }
    public void setFridayEnabled(boolean fridayEnabled) { isFridayEnabled = fridayEnabled; }
    public boolean isSaturdayEnabled() { return isSaturdayEnabled; }
    public void setSaturdayEnabled(boolean saturdayEnabled) { isSaturdayEnabled = saturdayEnabled; }
    public boolean isSundayEnabled() { return isSundayEnabled; }
    public void setSundayEnabled(boolean sundayEnabled) { isSundayEnabled = sundayEnabled; }

    /**
     * [기존] 이 알람이 반복 알람인지 여부를 간단히 확인하는 헬퍼(Helper) 메소드입니다.
     */
    @Ignore
    public boolean isRepeating() {
        return isMondayEnabled || isTuesdayEnabled || isWednesdayEnabled || isThursdayEnabled || isFridayEnabled || isSaturdayEnabled || isSundayEnabled;
    }

    // --- 시간 표시 형식을 위한 헬퍼(Helper) 메소드 ---

    @Ignore
    public String getAmPm() {
        if (hour >= 0 && hour < 12) {
            return "오전";
        } else {
            return "오후";
        }
    }

    @Ignore
    private int getHour12() {
        if (hour == 0) {
            return 12;
        } else if (hour > 12) {
            return hour - 12;
        } else {
            return hour;
        }
    }

    @Ignore
    public String getFormattedTime() {
        return String.format(Locale.getDefault(), "%02d:%02d", getHour12(), getMinute());
    }
}
