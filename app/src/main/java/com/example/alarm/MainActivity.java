package com.example.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

// OnAlarmInteractionListener 인터페이스 구현
public class MainActivity extends AppCompatActivity implements AlarmAdapter.OnAlarmInteractionListener {

    private static final String TAG = "MainActivity";

    private TextView timeTextView;
    private Handler handler;
    private Runnable timeUpdater;
    private SimpleDateFormat sdf;

    private AlarmViewModel alarmViewModel;
    private AlarmAdapter alarmAdapter;
    private RecyclerView recyclerView;
    private FloatingActionButton addAlarmFab;

    // --- 하단 메뉴 관련 UI 요소 ---
    private LinearLayout bottomActionMenu;
    private Button buttonTurnOff;
    private Button buttonDelete;

    public static final String ALARM_ID_EXTRA = "com.example.alarm.ALARM_ID_EXTRA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 기본 UI 요소 초기화
        timeTextView = findViewById(R.id.timeTextView);
        recyclerView = findViewById(R.id.alarmRecyclerView);
        addAlarmFab = findViewById(R.id.addAlarmFab);

        // 하단 메뉴 UI 요소 초기화
        bottomActionMenu = findViewById(R.id.bottom_action_menu);
        buttonTurnOff = findViewById(R.id.button_turn_off);
        buttonDelete = findViewById(R.id.button_delete);

        handler = new Handler(Looper.getMainLooper());
        sdf = new SimpleDateFormat("HH:mm:ss", Locale.KOREA);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

        // ViewModel 및 Adapter 설정
        setupViewModelAndAdapter();

        // 버튼 리스너 설정
        setupButtonListeners();

        // 뒤로가기 콜백 설정
        setupOnBackPressedCallback();

        // 시간 업데이트 시작
        timeUpdater = new Runnable() {
            @Override
            public void run() {
                Date currentTime = Calendar.getInstance().getTime();
                String formattedTime = sdf.format(currentTime);
                if (timeTextView != null) {
                    timeTextView.setText(formattedTime);
                }
                handler.postDelayed(this, 1000);
            }
        };
    }

    private void setupViewModelAndAdapter() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
        alarmAdapter = new AlarmAdapter(new AlarmAdapter.AlarmDiff(), this);
        recyclerView.setAdapter(alarmAdapter);
        alarmViewModel.getAllAlarms().observe(this, alarms -> alarmAdapter.submitList(alarms));
    }

    private void setupButtonListeners() {
        addAlarmFab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SetAlarmActivity.class);
            startActivity(intent);
        });

        buttonDelete.setOnClickListener(v -> {
            List<Alarm> selectedAlarms = alarmAdapter.getSelectedAlarms();
            for (Alarm alarm : selectedAlarms) {
                cancelAlarm(alarm); // 시스템 알람 취소
                alarmViewModel.delete(alarm); // DB에서 삭제
            }
            Toast.makeText(MainActivity.this, selectedAlarms.size() + "개의 알람이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
            exitSelectionMode();
        });

        buttonTurnOff.setOnClickListener(v -> {
            List<Alarm> selectedAlarms = alarmAdapter.getSelectedAlarms();
            for (Alarm alarm : selectedAlarms) {
                if (alarm.isEnabled) {
                    alarm.isEnabled = false;
                    cancelAlarm(alarm); // 시스템 알람 취소
                    alarmViewModel.update(alarm); // DB 업데이트
                }
            }
            Toast.makeText(MainActivity.this, "선택된 알람이 꺼졌습니다.", Toast.LENGTH_SHORT).show();
            exitSelectionMode();
        });
    }

    private void enterSelectionMode() {
        alarmAdapter.setSelectionMode(true);
        bottomActionMenu.setVisibility(View.VISIBLE);
        addAlarmFab.setVisibility(View.GONE);
    }

    private void exitSelectionMode() {
        alarmAdapter.setSelectionMode(false);
        bottomActionMenu.setVisibility(View.GONE);
        addAlarmFab.setVisibility(View.VISIBLE);
    }

    private void setupOnBackPressedCallback() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (alarmAdapter.isSelectionMode()) {
                    exitSelectionMode();
                } else {
                    // 기본 뒤로가기 동작을 수행하기 위해 콜백을 비활성화하고 다시 호출
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }


    // --- OnAlarmInteractionListener 구현부 ---

    @Override
    public void onAlarmToggled(Alarm alarm, boolean isEnabled) {
        if (isEnabled) {
            checkPermissionsAndSchedule(alarm);
        } else {
            alarm.isEnabled = false;
            alarmViewModel.update(alarm);
            cancelAlarm(alarm);
            Toast.makeText(MainActivity.this, String.format(Locale.getDefault(), "%02d:%02d 알람이 해제되었습니다.", alarm.hour, alarm.minute), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemClick(int position) {
        if (alarmAdapter.isSelectionMode()) {
            // 선택 모드일 때 아이템 클릭
            alarmAdapter.toggleSelection(position);
            if (alarmAdapter.getSelectedItemCount() == 0) {
                exitSelectionMode();
            }
        } else {
            // 일반 모드일 때 아이템 클릭 (알람 수정 등)
            // TODO: 알람 수정 화면으로 이동하는 로직 구현
            Toast.makeText(this, "알람 수정 화면으로 이동 (구현 필요)", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemLongClick(int position) {
        if (!alarmAdapter.isSelectionMode()) {
            enterSelectionMode();
        }
        onItemClick(position); // 롱클릭 시에도 선택 상태가 토글되도록
    }


    private void checkPermissionsAndSchedule(Alarm alarm) {
        // 이 메소드의 내용은 기존과 동일
        AlarmManager systemAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && systemAlarmManager != null && !systemAlarmManager.canScheduleExactAlarms()) {
            new AlertDialog.Builder(MainActivity.this)
                .setTitle("정확한 알람 권한 필요")
                .setMessage("알람을 설정하려면 '정확한 알람 예약' 권한이 필요합니다. 앱 설정에서 권한을 허용해 주세요.")
                .setPositiveButton("설정으로 이동", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(intent);
                    alarm.isEnabled = false;
                    alarmViewModel.update(alarm);
                })
                .setNegativeButton("취소", (dialog, which) -> {
                    alarm.isEnabled = false;
                    alarmViewModel.update(alarm);
                    Toast.makeText(MainActivity.this, "정확한 알람 권한이 없어 알람을 설정할 수 없습니다.", Toast.LENGTH_LONG).show();
                })
                .setOnDismissListener(dialogInterface -> {
                    AlarmManager amCheck = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && amCheck != null && !amCheck.canScheduleExactAlarms()) {
                        alarm.isEnabled = false;
                        alarmViewModel.update(alarm);
                    }
                })
                .setCancelable(false)
                .show();
        } else {
            alarm.isEnabled = true;
            alarmViewModel.update(alarm);
            scheduleAlarm(alarm);
            Toast.makeText(MainActivity.this, String.format(Locale.getDefault(), "%02d:%02d 알람이 설정되었습니다.", alarm.hour, alarm.minute), Toast.LENGTH_SHORT).show();
        }
    }

    // ... onResume, scheduleAlarm, cancelAlarm, onPause 메소드는 기존과 동일 ...
    @Override
    protected void onResume() {
        super.onResume();
        if (handler != null && timeUpdater != null) {
            handler.post(timeUpdater);
        }
        // 선택 모드 중에 설정 화면 등으로 나갔다 돌아왔을 때,
        // LiveData가 UI를 갱신하면서 어댑터의 선택 상태가 초기화될 수 있습니다.
        // 이를 방지하려면 onSaveInstanceState를 사용하거나, ViewModel에서 선택 상태를 관리해야 합니다.
        // 현재는 단순성을 위해 이 부분은 생략합니다.
    }

    private void scheduleAlarm(Alarm alarm) {
        Log.d(TAG, "scheduleAlarm called for ID: " + alarm.id + " at " + alarm.hour + ":" + alarm.minute);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra(ALARM_ID_EXTRA, alarm.id);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hour);
        calendar.set(Calendar.MINUTE, alarm.minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (alarmManager != null) {
            try {
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingIntent), pendingIntent);
                Log.d(TAG, "Alarm scheduled successfully for ID: " + alarm.id);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException in scheduleAlarm for ID: " + alarm.id + ". This should have been caught earlier.", e);
                Toast.makeText(this, "알람 설정 실패: 권한 문제. (오류 코드: S1)", Toast.LENGTH_LONG).show();
                alarm.isEnabled = false;
                alarmViewModel.update(alarm);
            }
        } else {
            Log.e(TAG, "AlarmManager is null in scheduleAlarm for ID: " + alarm.id);
        }
    }

    private void cancelAlarm(Alarm alarm) {
        Log.d(TAG, "cancelAlarm called for ID: " + alarm.id);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Alarm cancelled successfully for ID: " + alarm.id);
        } else {
            Log.e(TAG, "AlarmManager is null in cancelAlarm for ID: " + alarm.id);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null && timeUpdater != null) {
            handler.removeCallbacks(timeUpdater);
        }
    }
}
