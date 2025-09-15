package com.example.alarm; // 이 파일이 속한 패키지를 정의합니다.

// 필요한 안드로이드 및 자바 표준 라이브러리들을 가져옵니다.
import android.content.Intent; // 다른 Activity를 시작하기 위한 Intent 클래스입니다.
import android.os.Bundle; // 안드로이드 Activity의 상태 저장 및 복원에 사용됩니다.
import android.os.Handler; // 특정 작업을 지연 실행하거나 다른 스레드에서 UI 스레드로 메시지를 보내는 데 사용됩니다.
import android.os.Looper;  // 스레드별 메시지 루프를 관리합니다. UI 업데이트는 메인 Looper에서 해야 합니다.
import android.view.View;    // UI 요소(예: 버튼)의 클릭 이벤트를 처리하기 위해 필요합니다.
import android.widget.TextView; // 화면에 텍스트를 표시하는 UI 위젯입니다.
import androidx.appcompat.app.AppCompatActivity; // 이전 안드로이드 버전과의 호환성을 유지하면서 현대적인 액티비티 기능을 제공합니다.

import com.google.android.material.floatingactionbutton.FloatingActionButton; // Material Design의 FloatingActionButton 위젯을 사용하기 위해 필요합니다.

import java.text.SimpleDateFormat; // 날짜와 시간을 원하는 형식의 문자열로 변환하는 데 사용됩니다.
import java.util.Calendar;      // 현재 날짜와 시간 정보를 가져오는 데 사용됩니다.
import java.util.Date;          // 특정 시점의 날짜와 시간을 나타내는 객체입니다.
import java.util.Locale;        // 지역화 관련 정보를 다룰 때 사용됩니다 (예: 날짜/시간 형식).
import java.util.TimeZone;      // 시간대 정보를 다룰 때 사용됩니다.

// MainActivity 클래스는 앱의 주 화면을 나타내며, AppCompatActivity를 상속받습니다.
public class MainActivity extends AppCompatActivity {

    // 멤버 변수 선언
    private TextView timeTextView; // XML 레이아웃의 TextView 위젯(시간 표시용)을 가리킬 변수
    private Handler handler;       // UI 업데이트 및 주기적 실행을 위한 핸들러
    private Runnable timeUpdater;  // 현재 시간을 주기적으로 업데이트하는 작업을 정의할 Runnable 객체
    private SimpleDateFormat sdf;  // 날짜/시간을 특정 문자열 형식으로 포맷하기 위한 객체
    private FloatingActionButton addAlarmFab; // 알람 추가 버튼(FloatingActionButton)을 가리킬 변수

    // Activity가 처음 생성될 때 호출되는 메소드입니다.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // 부모 클래스(AppCompatActivity)의 onCreate 메소드 호출 (필수)
        // XML 레이아웃 파일(R.layout.activity_main)을 현재 Activity의 화면으로 설정합니다.
        setContentView(R.layout.activity_main);

        // XML 레이아웃에 정의된 ID (R.id.timeTextView)를 사용하여 TextView 위젯의 참조를 가져옵니다.
        timeTextView = findViewById(R.id.timeTextView);

        // Handler를 메인 스레드(UI 스레드)의 Looper와 연결하여 초기화합니다.
        // UI 업데이트는 반드시 메인 스레드에서 수행해야 합니다.
        handler = new Handler(Looper.getMainLooper());

        // SimpleDateFormat 객체를 초기화합니다.
        // "HH:mm:ss"는 24시간제 시:분:초 형식을 의미합니다.
        // Locale.KOREA를 사용하여 한국 지역 설정에 맞는 형식을 우선적으로 고려합니다.
        sdf = new SimpleDateFormat("HH:mm:ss", Locale.KOREA);
        // 시간대를 "Asia/Seoul" (한국 표준시, KST)로 명시적으로 설정합니다.
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

        // XML 레이아웃에 정의된 ID (R.id.addAlarmFab)를 사용하여 FloatingActionButton 위젯의 참조를 가져옵니다.
        addAlarmFab = findViewById(R.id.addAlarmFab);

        // 알람 추가 버튼(addAlarmFab)에 클릭 리스너를 설정합니다.
        addAlarmFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // 버튼이 클릭되었을 때 이 메소드가 호출됩니다.
                // SetAlarmActivity로 화면을 전환하기 위한 Intent 객체를 생성합니다.
                // 첫 번째 인자는 현재 Context(MainActivity.this)이고,
                // 두 번째 인자는 시작할 Activity의 클래스(SetAlarmActivity.class)입니다.
                Intent intent = new Intent(MainActivity.this, SetAlarmActivity.class);
                // 생성된 Intent를 사용하여 SetAlarmActivity를 시작합니다.
                startActivity(intent);
            }
        });

        // 주기적으로 시간을 업데이트할 Runnable 객체를 정의하고 초기화합니다.
        timeUpdater = new Runnable() {
            @Override
            public void run() { // 이 메소드가 실제로 주기적으로 실행될 코드입니다.
                // 현재 시간을 나타내는 Date 객체를 가져옵니다.
                // Calendar.getInstance()는 현재 시간대의 현재 시간을 기준으로 Calendar 객체를 반환합니다.
                Date currentTime = Calendar.getInstance().getTime();
                // sdf (SimpleDateFormat)를 사용하여 현재 시간을 "HH:mm:ss" 형식의 문자열로 변환합니다.
                String formattedTime = sdf.format(currentTime);

                // timeTextView가 null이 아닌지 확인하고 (안전성 확보), 포맷된 시간 문자열을 TextView에 설정합니다.
                if (timeTextView != null) {
                    timeTextView.setText(formattedTime);
                }

                // Handler를 사용하여 이 Runnable(timeUpdater 자신)을 1000밀리초(1초) 후에 다시 실행하도록 예약합니다.
                // 'this'는 현재 실행 중인 Runnable 객체(timeUpdater)를 가리킵니다.
                handler.postDelayed(this, 1000);
            }
        };
    }

    // Activity가 사용자에게 보여지기 시작할 때 (또는 다시 활성화될 때) 호출되는 메소드입니다.
    @Override
    protected void onResume() {
        super.onResume(); // 부모 클래스의 onResume 메소드 호출
        // Activity가 화면에 나타날 때 시간 업데이트를 시작(또는 재개)합니다.
        // handler와 timeUpdater가 null이 아닌지 확인하여 NullPointerException을 방지합니다.
        if (handler != null && timeUpdater != null) {
            // timeUpdater Runnable을 즉시 실행 대기열에 추가합니다 (실제 실행은 run() 메소드).
            handler.post(timeUpdater);
        }
    }

    // Activity가 화면에서 사라지기 직전 (다른 Activity가 전면에 오거나, 홈 버튼을 누르는 등) 호출되는 메소드입니다.
    @Override
    protected void onPause() {
        super.onPause(); // 부모 클래스의 onPause 메소드 호출
        // Activity가 보이지 않을 때는 불필요한 업데이트를 중지하여 배터리를 절약하고 시스템 리소스를 아낍니다.
        // handler와 timeUpdater가 null이 아닌지 확인합니다.
        if (handler != null && timeUpdater != null) {
            // Handler의 메시지 큐에서 timeUpdater Runnable에 대한 모든 예약된 콜백(postDelayed로 예약된 것)을 제거합니다.
            handler.removeCallbacks(timeUpdater);
        }
    }
}
