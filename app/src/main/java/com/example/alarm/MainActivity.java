package com.example.alarm;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
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
 * - 현재 시간 표시
 * - 데이터베이스의 알람 목록을 RecyclerView에 표시
 * - 사용자의 상호작용(알람 추가, 토글, 다중 선택 등)을 처리
 * - 시스템 알람을 예약하거나 취소
 */
public class MainActivity extends AppCompatActivity implements AlarmAdapter.OnAlarmInteractionListener {

    private static final String TAG = "MainActivity";

    // --- UI 요소 ---
    // [수정] TextClock으로 대체되었으므로 timeTextView 제거
    private RecyclerView recyclerView;
    private FloatingActionButton addAlarmFab;
    private LinearLayout bottomActionMenu;
    private Button buttonTurnOff;
    private Button buttonDelete;

    // --- 비즈니스 로직 및 데이터 관련 ---
    private AlarmViewModel alarmViewModel;
    private AlarmAdapter alarmAdapter;
    // [수정] TextClock으로 대체되었으므로 Handler, Runnable 제거

    // --- 권한 요청 관련 ---
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Alarm pendingAlarm;

    // --- 상수 ---
    public static final String ALARM_ID_EXTRA = "com.example.alarm.ALARM_ID_EXTRA";

    /**
     * Activity가 처음 생성될 때 호출되는 생명주기 메소드.
     * 모든 초기화 작업은 여기서 시작됩니다.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 코드 구조화를 위해 초기화 메소드들을 호출
        setupViews();
        setupViewModel();
        setupRecyclerView();
        setupListeners();
        // [수정] setupTimeUpdater() 호출 제거
        setupOnBackPressedCallback();
        setupPermissionLauncher();
    }

    // --- onCreate에서 호출되는 초기화 메소드들 (구조 개선) ---

    /**
     * XML 레이아웃의 뷰들을 찾아와 멤버 변수에 할당합니다.
     */
    private void setupViews() {
        // 시스템 바(상태 바, 네비게이션 바)와 앱 컨텐츠가 겹치지 않도록 패딩을 조정합니다.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // UI 뷰 초기화
        // [수정] timeTextView 초기화 코드 제거
        recyclerView = findViewById(R.id.alarmRecyclerView);
        addAlarmFab = findViewById(R.id.addAlarmFab);
        bottomActionMenu = findViewById(R.id.bottom_action_menu);
        buttonTurnOff = findViewById(R.id.button_turn_off);
        buttonDelete = findViewById(R.id.button_delete);
    }

    /**
     * ViewModel을 초기화하고, 데이터 변경을 감지하여 UI를 업데이트하도록 설정합니다.
     */
    private void setupViewModel() {
        alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
        // getAllAlarms()가 반환하는 LiveData를 구독(observe)합니다.
        // 데이터베이스의 'alarms' 테이블에 변경이 생길 때마다, 람다식 내부의 코드가 자동으로 실행됩니다.
        alarmViewModel.getAllAlarms().observe(this, alarms -> {
            Log.d(TAG, "LiveData가 변경됨. " + (alarms != null ? alarms.size() : 0) + "개의 알람을 어댑터에 전달합니다.");
            // ListAdapter의 submitList() 메소드에 새로운 리스트를 전달합니다.
            // DiffUtil이 백그라운드에서 변경사항을 계산하여 RecyclerView를 효율적으로 업데이트합니다.
            alarmAdapter.submitList(alarms);
        });
    }

    /**
     * RecyclerView와 AlarmAdapter를 초기화하고 연결합니다.
     */
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // MainActivity가 리스너 인터페이스를 구현했으므로, this를 전달합니다.
        alarmAdapter = new AlarmAdapter(new AlarmAdapter.AlarmDiff(), this);
        recyclerView.setAdapter(alarmAdapter);
    }

    /**
     * 각종 버튼들의 클릭 리스너를 설정합니다.
     */
    private void setupListeners() {
        // 알람 추가 버튼
        addAlarmFab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SetAlarmActivity.class);
            startActivity(intent);
        });

        // (다중 선택 모드) 삭제 버튼
        buttonDelete.setOnClickListener(v -> {
            List<Alarm> selectedAlarms = alarmAdapter.getSelectedAlarms();
            for (Alarm alarm : selectedAlarms) {
                cancelAlarm(alarm); // 시스템에 예약된 알람 취소
                alarmViewModel.delete(alarm); // 데이터베이스에서 삭제
            }
            Toast.makeText(this, selectedAlarms.size() + "개의 알람이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
            exitSelectionMode(); // 선택 모드 종료
        });

        // (다중 선택 모드) 끄기 버튼
        buttonTurnOff.setOnClickListener(v -> {
            List<Alarm> selectedAlarms = alarmAdapter.getSelectedAlarms();
            for (Alarm alarm : selectedAlarms) {
                if (alarm.isEnabled()) {
                    alarm.setEnabled(false);
                    cancelAlarm(alarm); // 시스템에 예약된 알람 취소
                    alarmViewModel.update(alarm); // 데이터베이스 업데이트
                }
            }
            Toast.makeText(this, "선택된 알람이 꺼졌습니다.", Toast.LENGTH_SHORT).show();
            exitSelectionMode(); // 선택 모드 종료
        });
    }

    // [수정] setupTimeUpdater() 메소드 전체 제거

    /**
     * 뒤로가기 버튼의 동작을 제어하기 위한 콜백을 설정합니다.
     */
    private void setupOnBackPressedCallback() {
        // OnBackPressedDispatcher를 통해 콜백을 등록합니다.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 만약 다중 선택 모드라면, 앱을 종료하는 대신 선택 모드만 종료시킵니다.
                if (alarmAdapter.isSelectionMode()) {
                    exitSelectionMode();
                } else {
                    // 선택 모드가 아니라면, 콜백을 비활성화하고 기본 뒤로가기 동작을 수행합니다.
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    /**
     * 권한 요청 결과를 처리하는 ActivityResultLauncher를 초기화합니다.
     */
    private void setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // 알림 권한이 허용된 경우, 보류 중이던 알람에 대해 다음 권한 확인/예약 절차를 계속 진행합니다.
                if (pendingAlarm != null) {
                    checkExactAlarmPermissionAndSchedule(pendingAlarm);
                    pendingAlarm = null; // 처리했으므로 보류 중인 알람을 비웁니다.
                }
            } else {
                // 알림 권한이 거부된 경우, 사용자에게 알리고 알람 상태를 DB에 '꺼짐'으로 업데이트합니다.
                Toast.makeText(this, "알림 권한이 없어 알람을 설정할 수 없습니다.", Toast.LENGTH_LONG).show();
                if (pendingAlarm != null) {
                    pendingAlarm.setEnabled(false);
                    alarmViewModel.update(pendingAlarm);
                    pendingAlarm = null;
                }
            }
        });
    }


    // --- 다중 선택 모드 관리 ---

    /**
     * 다중 선택 모드로 진입합니다.
     */
    private void enterSelectionMode() {
        alarmAdapter.setSelectionMode(true);
        bottomActionMenu.setVisibility(View.VISIBLE); // 하단 메뉴를 보여줍니다.
        addAlarmFab.hide(); // 플로팅 액션 버튼을 숨깁니다.
    }

    /**
     * 다중 선택 모드를 종료합니다.
     */
    private void exitSelectionMode() {
        alarmAdapter.setSelectionMode(false);
        bottomActionMenu.setVisibility(View.GONE); // 하단 메뉴를 숨깁니다.
        addAlarmFab.show(); // 플로팅 액션 버튼을 다시 보여줍니다.
    }


    // --- Adapter의 OnAlarmInteractionListener 구현부 ---

    /**
     * 알람 아이템의 스위치가 토글될 때 어댑터로부터 호출됩니다.
     * @param alarm 토글된 알람 객체
     * @param isEnabled 스위치의 새로운 상태 (true: 켜짐, false: 꺼짐)
     */
    @Override
    public void onAlarmToggled(Alarm alarm, boolean isEnabled) {
        if (isEnabled) {
            checkPermissionsAndSchedule(alarm);
        } else {
            alarm.setEnabled(false);
            alarmViewModel.update(alarm);
            cancelAlarm(alarm);
            Toast.makeText(this, formatTime(alarm.getHour(), alarm.getMinute()) + " 알람이 해제되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemClick(int position) {
        if (alarmAdapter.isSelectionMode()) {
            alarmAdapter.toggleSelection(position);
            if (alarmAdapter.getSelectedItemCount() == 0) {
                exitSelectionMode();
            }
        } else {
            Toast.makeText(this, "알람 수정 기능은 아직 구현되지 않았습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemLongClick(int position) {
        if (!alarmAdapter.isSelectionMode()) {
            enterSelectionMode();
        }
        onItemClick(position);
    }


    // --- 시스템 알람(AlarmManager) 및 권한 관련 메소드 ---

    private void checkPermissionsAndSchedule(Alarm alarm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                checkExactAlarmPermissionAndSchedule(alarm);
            } else {
                pendingAlarm = alarm;
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            checkExactAlarmPermissionAndSchedule(alarm);
        }
    }

    private void checkExactAlarmPermissionAndSchedule(Alarm alarm) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            new AlertDialog.Builder(this)
                .setTitle("정확한 알람 권한 필요")
                .setMessage("알람을 정확한 시간에 울리게 하려면 \'알람 및 리마인더\' 권한이 필요합니다. 설정 화면으로 이동하여 권한을 허용해 주세요.")
                .setPositiveButton("설정으로 이동", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(intent);
                    alarm.setEnabled(false);
                    alarmViewModel.update(alarm);
                })
                .setNegativeButton("취소", (dialog, which) -> {
                    Toast.makeText(this, "권한이 없어 알람을 설정할 수 없습니다.", Toast.LENGTH_LONG).show();
                    alarm.setEnabled(false);
                    alarmViewModel.update(alarm);
                })
                .setOnCancelListener(dialog -> {
                    Toast.makeText(this, "권한이 없어 알람을 설정할 수 없습니다.", Toast.LENGTH_LONG).show();
                    alarm.setEnabled(false);
                    alarmViewModel.update(alarm);
                })
                .show();
        } else {
            alarm.setEnabled(true);
            alarmViewModel.update(alarm);
            scheduleAlarm(alarm);
            Toast.makeText(this, formatTime(alarm.getHour(), alarm.getMinute()) + " 알람이 설정되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleAlarm(Alarm alarm) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra(ALARM_ID_EXTRA, alarm.getId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarm.getId(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        calendar.set(Calendar.MINUTE, alarm.getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingIntent), pendingIntent);
        Log.d(TAG, "알람 ID " + alarm.getId() + "이 " + formatTime(alarm.getHour(), alarm.getMinute()) + "에 예약되었습니다.");
    }

    private void cancelAlarm(Alarm alarm) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarm.getId(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "알람 ID " + alarm.getId() + "이 취소되었습니다.");
    }


    // --- Activity 생명주기 관련 메소드 ---

    // [수정] onResume에서 Handler 관련 코드 제거
    @Override
    protected void onResume() {
        super.onResume();
    }

    // [수정] onPause에서 Handler 관련 코드 제거
    @Override
    protected void onPause() {
        super.onPause();
    }

    // --- 유틸리티 메소드 ---
    private String formatTime(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        SimpleDateFormat sdf = new SimpleDateFormat("a hh:mm", Locale.KOREA);
        return sdf.format(calendar.getTime());
    }
}
