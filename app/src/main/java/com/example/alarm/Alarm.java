package com.example.alarm;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Locale;

/**
 * 알람 하나의 데이터를 표현하는 클래스 (데이터 모델).
 * 이 클래스는 Room 데이터베이스의 'alarms' 테이블과 1:1로 매핑됩니다.
 *
 * @Entity 어노테이션은 이 클래스가 데이터베이스의 테이블임을 나타냅니다.
 * tableName 속성은 테이블의 이름을 'alarms'로 지정합니다.
 */
@Entity(tableName = "alarms")
public class Alarm {

    /**
     * 알람의 고유 ID.
     * @PrimaryKey 어노테이션은 이 필드가 테이블의 기본 키(Primary Key)임을 나타냅니다.
     * autoGenerate = true 속성은 Room이 새로운 알람 데이터를 추가할 때마다
     * 이 ID를 자동으로 1씩 증가시켜 할당하도록 설정합니다.
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * 알람이 울릴 시간 (0-23).
     * @ColumnInfo 어노테이션은 이 필드가 테이블의 특정 컬럼에 해당함을 나타냅니다.
     * name 속성은 컬럼의 이름을 'hour'로 지정합니다.
     */
    @ColumnInfo(name = "hour")
    private int hour;

    /**
     * 알람이 울릴 분 (0-59).
     * 컬럼 이름은 'minute'로 지정됩니다.
     */
    @ColumnInfo(name = "minute")
    private int minute;

    /**
     * 알람의 활성화 여부.
     * true이면 알람이 설정된 상태, false이면 해제된 상태입니다.
     * 컬럼 이름은 'is_enabled'로 지정됩니다.
     */
    @ColumnInfo(name = "is_enabled")
    public boolean isEnabled; // isEnabled는 상태 변경이 잦으므로 public으로 유지하거나 setter를 추가하는 것이 편리할 수 있습니다.

    /**
     * Room이 데이터베이스에서 Alarm 객체를 생성할 때 사용하는 생성자입니다.
     * Room은 이 생성자를 통해 각 컬럼의 값을 객체의 필드에 채워 넣습니다.
     * @param id 알람의 고유 ID
     * @param hour 알람 시간
     * @param minute 알람 분
     * @param isEnabled 알람 활성화 여부
     */
    public Alarm(int id, int hour, int minute, boolean isEnabled) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.isEnabled = isEnabled;
    }

    /**
     * 새로운 알람을 생성할 때 주로 사용하는 생성자입니다.
     * id는 Room에 의해 자동으로 생성되므로, 이 생성자에서는 id를 받지 않습니다.
     * @Ignore 어노테이션은 Room이 이 생성자를 데이터베이스 작업에 사용하지 않도록 지시합니다.
     * @param hour 알람 시간
     * @param minute 알람 분
     * @param isEnabled 알람 활성화 여부
     */
    @Ignore
    public Alarm(int hour, int minute, boolean isEnabled) {
        this.hour = hour;
        this.minute = minute;
        this.isEnabled = isEnabled;
    }


    // --- Getter 및 Setter 메소드 ---
    // 필드를 private으로 변경했으므로, 외부에서 필드 값에 접근하려면 public getter 메소드가 필요합니다.
    // Setter는 필요에 따라 추가합니다. (예: id는 Room이 설정하므로 setter가 필요할 수 있음)

    public int getId() {
        return id;
    }

    // Room이 id를 설정할 수 있도록 setter를 제공합니다.
    public void setId(int id) {
        this.id = id;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    // --- [추가] 시간 표시 형식을 위한 헬퍼(Helper) 메소드 ---

    /**
     * 24시간 형식의 시간을 기반으로 "오전" 또는 "오후" 문자열을 반환합니다.
     * 이 메소드는 데이터베이스에 저장되지 않으며, 오직 화면 표시용으로만 사용됩니다.
     * @return "오전" 또는 "오후"
     */
    @Ignore
    public String getAmPm() {
        if (hour >= 0 && hour < 12) {
            return "오전";
        } else {
            return "오후";
        }
    }

    /**
     * 24시간 형식의 시간을 12시간 형식으로 변환하여 반환합니다.
     * (예: 0시는 12시, 13시는 1시로 변환)
     * @return 12시간 형식의 시간 (1-12)
     */
    @Ignore
    private int getHour12() {
        if (hour == 0) {
            return 12; // 자정은 12시로 표시
        } else if (hour > 12) {
            return hour - 12; // 오후 시간 변환
        } else {
            return hour; // 오전 시간은 그대로
        }
    }

    /**
     * 12시간 형식에 맞는 시간과 분을 "hh:mm" 형태의 문자열로 만들어 반환합니다.
     * @return "03:15" 와 같은 형식의 시간 문자열
     */
    @Ignore
    public String getFormattedTime() {
        return String.format(Locale.getDefault(), "%02d:%02d", getHour12(), getMinute());
    }
}
