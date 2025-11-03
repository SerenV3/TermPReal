package com.example.alarm;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * 앱의 메인 화면(Activity).
 * 이 클래스는 앱의 주된 UI를 담당하며, 다음과 같은 역할을 수행합니다.
 * - 데이터베이스의 알람 목록을 RecyclerView를 통해 화면에 표시
 * - 사용자의 상호작용(알람 추가, 토글, 다중 선택 등)을 처리
 * - 시스템 권한을 확인하고 알람을 예약하거나 취소하는 작업 조율
 */
public class MainActivity extends AppCompatActivity implements AlarmAdapter.OnAlarmInteractionListener {

    // 로그 출력을 위한 태그
    private static final String TAG = "MainActivity";

    // --- UI 요소 --- //
    /** 알람 목록을 보여주는 RecyclerView */
    private RecyclerView recyclerView;
    /** 새 알람을 추가하는 플로팅 액션 버튼 */
    private FloatingActionButton addAlarmFab;
    /** 다중 선택 모드에서 나타나는 하단 메뉴 (끄기, 삭제) */
    private LinearLayout bottomActionMenu;
    /** 하단 메뉴의 '끄기' 버튼 */
    private Button buttonTurnOff;
    /** 하단 메뉴의 '삭제' 버튼 */
    private Button buttonDelete;

    // --- 비즈니스 로직 및 데이터 관련 --- //
    /** UI 관련 데이터를 관리하고 데이터베이스와 통신하는 ViewModel */
    private AlarmViewModel alarmViewModel;
    /** RecyclerView에 알람 데이터를 연결해주는 어댑터 */
    private AlarmAdapter alarmAdapter;
    /** [추가] 알람 예약/취소 로직을 담당하는 스케줄러 클래스 */
    private AlarmScheduler alarmScheduler;

    // --- 권한 요청 관련 --- //
    /** 권한 요청 결과를 처리하는 ActivityResultLauncher (AndroidX의 새로운 방식) */
    private ActivityResultLauncher<String> requestPermissionLauncher;
    /** 권한 요청이 진행되는 동안 임시로 알람 객체를 저장하는 변수 */
    private Alarm pendingAlarm;

    /** 다른 컴포넌트(Receiver, Activity) 간에 알람 ID를 전달하기 위한 키 값 */
    public static final String ALARM_ID_EXTRA = "com.example.alarm.ALARM_ID_EXTRA";

    /**
     * Activity가 생성될 때 가장 먼저 호출되는 메소드.
     * 앱의 초기 설정을 여기서 수행합니다.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 모든 뷰(View)들을 초기화하고, 시스템 UI와 상호작용을 설정합니다.
        setupViews();
        // 2. [추가] 알람 스케줄러를 초기화합니다. (메소드 분리로 코드 구조 개선)
        alarmScheduler = new AlarmScheduler(this);
        // 3. ViewModel을 설정하고 데이터(알람 목록)의 변경을 감지합니다.
        setupViewModel();
        // 4. RecyclerView를 설정합니다.
        setupRecyclerView();
        // 5. 버튼 클릭 등 사용자의 입력을 받을 리스너들을 설정합니다.
        setupListeners();
        // 6. 뒤로가기 버튼의 동작을 재정의합니다 (선택 모드 해제 등).
        setupOnBackPressedCallback();
        // 7. 권한 요청 결과를 처리할 런처를 준비합니다.
        setupPermissionLauncher();
    }

    /** XML 레이아웃의 UI 요소들을 코드와 연결(바인딩)합니다. */
    private void setupViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.alarmRecyclerView);
        addAlarmFab = findViewById(R.id.addAlarmFab);
        bottomActionMenu = findViewById(R.id.bottom_action_menu);
        buttonTurnOff = findViewById(R.id.button_turn_off);
        buttonDelete = findViewById(R.id.button_delete);
    }

    /** ViewModel을 설정하고, 데이터베이스의 알람 목록이 변경될 때마다 UI를 자동으로 업데이트하도록 설정합니다. */
    private void setupViewModel() {
        alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
        // `getAllAlarms()`가 반환하는 LiveData를 관찰(observe)합니다.
        // 데이터베이스의 `alarms` 테이블에 변경이 생기면, 이 람다 표현식이 자동으로 실행됩니다.
        alarmViewModel.getAllAlarms().observe(this, alarms -> {
            Log.d(TAG, "LiveData가 변경됨. " + (alarms != null ? alarms.size() : 0) + "개의 알람을 어댑터에 전달합니다.");
            // `submitList`는 ListAdapter의 메소드로, DiffUtil을 사용해 효율적으로 목록을 업데이트합니다.
            alarmAdapter.submitList(alarms);
        });
    }

    /** RecyclerView와 Adapter를 설정합니다. */
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // 어댑터를 생성할 때, 'this'를 전달하여 Activity가 리스너 역할을 하도록 합니다.
        alarmAdapter = new AlarmAdapter(new AlarmAdapter.AlarmDiff(), this);
        recyclerView.setAdapter(alarmAdapter);
    }

    /** 각종 버튼의 클릭 이벤트를 처리하는 리스너를 설정합니다. */
    private void setupListeners() {
        // '알람 추가' 버튼 클릭 시, SetAlarmActivity를 엽니다.
        addAlarmFab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SetAlarmActivity.class);
            startActivity(intent);
        });

        // '삭제' 버튼 클릭 시
        buttonDelete.setOnClickListener(v -> {
            List<Alarm> selectedAlarms = alarmAdapter.getSelectedAlarms();
            for (Alarm alarm : selectedAlarms) {
                cancelAlarm(alarm); // 먼저 시스템에 예약된 알람을 취소하고
                alarmViewModel.delete(alarm); // 그 다음 데이터베이스에서 삭제합니다.
            }
            Toast.makeText(this, selectedAlarms.size() + "개의 알람이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
            exitSelectionMode(); // 선택 모드를 종료합니다.
        });

        // '끄기' 버튼 클릭 시
        buttonTurnOff.setOnClickListener(v -> {
            List<Alarm> selectedAlarms = alarmAdapter.getSelectedAlarms();
            for (Alarm alarm : selectedAlarms) {
                if (alarm.isEnabled()) {
                    alarm.setEnabled(false); // 알람 상태를 '비활성'으로 변경하고
                    cancelAlarm(alarm); // 시스템 알람을 취소한 뒤
                    alarmViewModel.update(alarm); // 변경된 상태를 데이터베이스에 업데이트합니다.
                }
            }
            Toast.makeText(this, "선택된 알람이 꺼졌습니다.", Toast.LENGTH_SHORT).show();
            exitSelectionMode(); // 선택 모드를 종료합니다.
        });
    }

    /** 뒤로가기 버튼의 기본 동작을 재정의합니다. */
    private void setupOnBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 만약 현재 다중 선택 모드라면, 뒤로가기 버튼은 앱을 종료하는 대신 선택 모드를 해제합니다.
                if (alarmAdapter.isSelectionMode()) {
                    exitSelectionMode();
                } else {
                    // 선택 모드가 아니라면, 원래의 뒤로가기 동작을 수행합니다.
                    setEnabled(false); // 콜백을 비활성화하고
                    getOnBackPressedDispatcher().onBackPressed(); // 기본 동작을 호출
                }
            }
        });
    }

    /** 권한 요청의 결과를 처리하는 런처를 설정합니다. */
    private void setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // 권한이 허용되었다면, 임시로 저장해둔 'pendingAlarm'에 대해 알람 예약 절차를 계속 진행합니다.
                if (pendingAlarm != null) {
                    checkExactAlarmPermissionAndSchedule(pendingAlarm);
                    pendingAlarm = null; // 처리 후 변수 초기화
                }
            } else {
                // 권한이 거부되었다면, 사용자에게 알리고 알람을 비활성화 상태로 되돌립니다.
                Toast.makeText(this, "알림 권한이 없어 알람을 설정할 수 없습니다.", Toast.LENGTH_LONG).show();
                if (pendingAlarm != null) {
                    pendingAlarm.setEnabled(false);
                    alarmViewModel.update(pendingAlarm);
                    pendingAlarm = null; // 처리 후 변수 초기화
                }
            }
        });
    }

    /** 다중 선택 모드로 진입하는 UI 처리 */
    private void enterSelectionMode() {
        alarmAdapter.setSelectionMode(true);
        bottomActionMenu.setVisibility(View.VISIBLE); // 하단 메뉴를 보여주고
        addAlarmFab.hide(); // '알람 추가' 버튼을 숨깁니다.
    }

    /** 다중 선택 모드에서 나가는 UI 처리 */
    private void exitSelectionMode() {
        alarmAdapter.setSelectionMode(false);
        bottomActionMenu.setVisibility(View.GONE); // 하단 메뉴를 숨기고
        addAlarmFab.show(); // '알람 추가' 버튼을 다시 보여줍니다.
    }

    // --- AlarmAdapter.OnAlarmInteractionListener 인터페이스 구현부 --- //

    /** 알람 목록 아이템의 스위치가 토글될 때 어댑터에 의해 호출됩니다. */
    @Override
    public void onAlarmToggled(Alarm alarm, boolean isEnabled) {
        if (isEnabled) {
            // 스위치가 켜졌다면, 권한을 확인하고 알람을 예약하는 절차를 시작합니다.
            checkPermissionsAndSchedule(alarm);
        } else {
            // 스위치가 꺼졌다면, 알람을 비활성화하고 시스템 예약을 취소합니다.
            alarm.setEnabled(false);
            alarmViewModel.update(alarm);
            cancelAlarm(alarm);
            Toast.makeText(this, formatTime(alarm.getHour(), alarm.getMinute()) + " 알람이 해제되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    /** 알람 아이템이 클릭되었을 때 어댑터에 의해 호출됩니다. */
    @Override
    public void onItemClick(int position) {
        if (alarmAdapter.isSelectionMode()) {
            // 선택 모드에서는 아이템 선택/해제 상태를 토글합니다.
            alarmAdapter.toggleSelection(position);
            // 만약 선택된 아이템이 하나도 없다면, 자동으로 선택 모드를 종료합니다.
            if (alarmAdapter.getSelectedItemCount() == 0) {
                exitSelectionMode();
            }
        } else {
            // [새로운 내용] 일반 모드에서 아이템을 클릭하면, 해당 알람을 수정하기 위해 SetAlarmActivity로 이동합니다.
            Alarm alarmToEdit = alarmAdapter.getCurrentList().get(position);
            Intent intent = new Intent(MainActivity.this, SetAlarmActivity.class);

            // [새로운 내용] 수정할 알람의 ID를 Intent에 담아 전달합니다.
            // SetAlarmActivity에서는 이 ID를 받아 기존 알람 정보를 불러오게 됩니다.
            intent.putExtra(ALARM_ID_EXTRA, alarmToEdit.getId());
            startActivity(intent);
        }
    }

    /** 알람 아이템이 길게 클릭되었을 때 어댑터에 의해 호출됩니다. */
    @Override
    public void onItemLongClick(int position) {
        if (!alarmAdapter.isSelectionMode()) {
            // 일반 모드에서 길게 클릭하면, 다중 선택 모드로 진입합니다.
            enterSelectionMode();
        }
        // 길게 클릭한 아이템이 바로 선택되도록 일반 클릭 로직을 이어서 호출합니다.
        onItemClick(position);
    }

    /**
     * [권한 플로우 1] 알람 예약을 위한 권한 확인 절차를 시작하는 메소드.
     * Android 13 (TIRAMISU) 이상에서는 POST_NOTIFICATIONS 권한을 먼저 확인해야 합니다.
     */
    private void checkPermissionsAndSchedule(Alarm alarm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 알림 권한이 이미 있는지 확인합니다.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // 권한이 있다면, 다음 단계인 '정확한 알람' 권한 확인으로 넘어갑니다.
                checkExactAlarmPermissionAndSchedule(alarm);
            } else {
                // 권한이 없다면, 알람 객체를 `pendingAlarm`에 임시 저장하고 권한 요청을 시작합니다.
                // 결과는 `setupPermissionLauncher`에서 처리됩니다.
                pendingAlarm = alarm;
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Android 13 미만에서는 알림 권한이 필요 없으므로 바로 다음 단계로 넘어갑니다.
            checkExactAlarmPermissionAndSchedule(alarm);
        }
    }

    /**
     * [권한 플로우 2] 정확한 알람 예약 권한을 확인하고, 최종적으로 알람을 예약하는 메소드.
     * Android 12 (S) 이상에서는 사용자가 직접 '알람 및 리마인더' 권한을 허용해야 합니다.
     */
    private void checkExactAlarmPermissionAndSchedule(Alarm alarm) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Android 12 이상이고, '정확한 알람' 예약 권한이 없는 경우
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // 사용자에게 왜 권한이 필요한지 설명하고 설정 화면으로 안내하는 대화상자를 띄웁니다.
            new AlertDialog.Builder(this)
                .setTitle("정확한 알람 권한 필요")
                .setMessage("알람을 정확한 시간에 울리게 하려면 \'알람 및 리마인더\' 권한이 필요합니다. 설정 화면으로 이동하여 권한을 허용해 주세요.")
                .setPositiveButton("설정으로 이동", (dialog, which) -> {
                    // 사용자가 '설정'을 누르면, 권한 설정 화면으로 가는 Intent를 실행합니다.
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(intent);
                    // 사용자가 설정을 하고 돌아올 때까지 알람은 비활성화 상태로 둡니다.
                    alarm.setEnabled(false);
                    alarmViewModel.update(alarm);
                })
                .setNegativeButton("취소", (dialog, which) -> {
                    // 사용자가 '취소'를 누르면, 권한이 없어 알람을 켤 수 없음을 알립니다.
                    Toast.makeText(this, "권한이 없어 알람을 설정할 수 없습니다.", Toast.LENGTH_LONG).show();
                    alarm.setEnabled(false);
                    alarmViewModel.update(alarm);
                })
                .setOnCancelListener(dialog -> { // 대화상자 바깥을 눌러 닫았을 때도 동일하게 처리합니다.
                    Toast.makeText(this, "권한이 없어 알람을 설정할 수 없습니다.", Toast.LENGTH_LONG).show();
                    alarm.setEnabled(false);
                    alarmViewModel.update(alarm);
                })
                .show();
        } else {
            // 모든 권한이 확인되었다면, 알람 상태를 '활성'으로 변경하고 데이터베이스에 업데이트한 뒤,
            alarm.setEnabled(true);
            alarmViewModel.update(alarm);
            // 최종적으로 스케줄러를 통해 알람을 예약합니다.
            scheduleAlarm(alarm);
            Toast.makeText(this, formatTime(alarm.getHour(), alarm.getMinute()) + " 알람이 설정되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * [수정] 특정 알람을 시스템의 AlarmManager에 예약(등록)합니다.
     * 실제 로직은 AlarmScheduler 클래스로 이전되었으며, 이 메소드는 스케줄러를 호출하는 역할만 합니다. (관심사 분리)
     * @param alarm 예약할 알람 객체
     */
    private void scheduleAlarm(Alarm alarm) {
        alarmScheduler.schedule(alarm);
    }

    /**
     * [수정] 시스템에 예약된 알람을 취소합니다.
     * 실제 로직은 AlarmScheduler 클래스로 이전되었으며, 이 메소드는 스케줄러를 호출하는 역할만 합니다. (관심사 분리)
     * @param alarm 취소할 알람 객체
     */
    private void cancelAlarm(Alarm alarm) {
        alarmScheduler.cancel(alarm);
    }

    // Activity 생명주기 관련 메소드 (현재는 특별한 동작 없음)
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /** 시간을 UI에 표시할 형식(예: 오전 07:30)의 문자열로 변환하는 헬퍼 메소드 */
    private String formatTime(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        SimpleDateFormat sdf = new SimpleDateFormat("a hh:mm", Locale.KOREA);
        return sdf.format(calendar.getTime());
    }
}
