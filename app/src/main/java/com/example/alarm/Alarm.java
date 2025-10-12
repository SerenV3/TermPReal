package com.example.alarm;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "alarms") // 데이터베이스 테이블 이름을 "alarms"로 지정
public class Alarm {

    @PrimaryKey(autoGenerate = true) // id를 기본 키로 하고, 자동으로 값을 생성하도록 설정
    public int id;

    @ColumnInfo(name = "hour") // 테이블의 컬럼 이름을 "hour"로 지정
    public int hour;

    @ColumnInfo(name = "minute") // 테이블의 컬럼 이름을 "minute"로 지정
    public int minute;

    @ColumnInfo(name = "is_enabled") // 테이블의 컬럼 이름을 "is_enabled"로 지정
    public boolean isEnabled;

    // 생성자, getter, setter는 필요에 따라 추가할 수 있습니다.
    // ROOM은 public 필드에 직접 접근하거나, getter/setter가 있다면 이를 활용합니다.

    // 간단한 예시를 위해 기본 생성자만 두거나, 필드를 받는 생성자를 추가할 수 있습니다.
    public Alarm(int hour, int minute, boolean isEnabled) {
        this.hour = hour;
        this.minute = minute;
        this.isEnabled = isEnabled;
    }

    // ROOM이 객체를 생성할 때 사용할 수 있도록 빈 생성자가 필요할 수 있습니다.
    // (하지만 위 생성자만 있어도 동작하는 경우가 많습니다. 만약 오류 발생 시 추가 고려)
    // public Alarm() {}

    // 각 필드에 대한 getter 와 setter 를 추가하면 캡슐화에 더 좋습니다.
    // 여기서는 간결함을 위해 public 필드로 두었지만, 실제 프로젝트에서는 getter/setter 사용을 권장합니다.

    // 아이디를 설정하지 않는 생성자 (자동 생성을 위해)
    // 혹은 모든 필드를 받는 생성자 후 id는 ROOM이 채우도록 할 수도 있습니다.

    // 예시: id는 자동 생성되므로 생성자에서 제외, isEnabled는 기본값 설정 가능
    // public Alarm(int hour, int minute) {
    //     this.hour = hour;
    //     this.minute = minute;
    //     this.isEnabled = true; // 기본적으로 활성화된 알람으로 생성
    // }
}
