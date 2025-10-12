package com.example.testalarmreal02;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Locale;

/**
 * 데이터베이스의 'alarms' 테이블과 매핑될 데이터 클래스 (Entity).
 * 이 클래스의 각 인스턴스는 테이블의 하나의 행(row)을 나타냅니다.
 */
@Entity(tableName = "alarms") // 이 클래스가 'alarms'라는 테이블임을 Room에 알립니다.
public class Alarm {

    /**
     * 알람의 고유 ID.
     * @PrimaryKey 어노테이션은 이 필드가 테이블의 기본 키(Primary Key)임을 나타냅니다.
     * autoGenerate = true는 Room이 ID를 자동으로 생성하고 1씩 증가시키도록 합니다.
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * 알람 시간 (0-23)
     */
    private int hour;

    /**
     * 알람 분 (0-59)
     */
    private int minute;

    /**
     * 알람 활성화 여부.
     * 데이터베이스에는 'is_enabled'라는 이름의 컬럼으로 저장됩니다.
     */
    @ColumnInfo(name = "is_enabled")
    private boolean isEnabled;

    /**
     * Room이 데이터베이스에서 객체를 다시 만들 때 사용하는 생성자.
     * 모든 필드를 인자로 받습니다.
     */
    public Alarm(int id, int hour, int minute, boolean isEnabled) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.isEnabled = isEnabled;
    }

    /**
     * 사용자가 새로운 알람을 만들 때 사용하는 생성자.
     * @Ignore 어노테이션을 붙여 Room이 이 생성자를 데이터베이스 작업에 사용하지 않도록 합니다.
     * id는 자동으로 생성되므로 인자로 받지 않습니다.
     */
    @Ignore
    public Alarm(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
        this.isEnabled = true; // 새로 만들 때는 기본적으로 활성화 상태로 설정
    }

    // --- Getter와 Setter ---
    // private 필드에 접근하고 값을 설정하기 위해 필요합니다.

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    // --- 데이터베이스에 저장되지 않는 헬퍼(Helper) 메소드 ---
    // Room은 필드와 직접적으로 연결되지 않은 메소드들은 자동으로 무시하므로,
    // 이 메소드들은 데이터베이스 스키마에 영향을 주지 않습니다.

    /**
     * @return '오전' 또는 '오후' 문자열을 반환
     */
    @Ignore
    public String getAmPm() {
        return hour < 12 ? "오전" : "오후";
    }

    /**
     * @return 12시간제 형식의 시간 문자열 (예: "03:30")
     */
    @Ignore
    public String getFormattedTime() {
        int displayHour = (hour == 0 || hour == 12) ? 12 : hour % 12;
        return String.format(Locale.getDefault(), "%02d:%02d", displayHour, minute);
    }
}
