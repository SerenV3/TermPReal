package com.example.alarm;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

/**
 * [기존 주석 개선] 새로운 알람을 설정하거나 '기존 알람을 수정'하는 화면(Activity)입니다.
 * MainActivity로부터 알람 ID를 전달받았는지 여부에 따라 '생성 모드' 또는 '수정 모드'로 동작합니다.
 */
public class SetAlarmActivity extends AppCompatActivity {

    private static final String TAG = "SetAlarmActivity";

    // --- UI 요소 --- //
    private TimePicker timePicker;
    private Button saveAlarmButton;
    private Button cancelButton;
    private SwitchMaterial vibrationSwitch;
    private ToggleButton mondayButton, tuesdayButton, wednesdayButton, thursdayButton, fridayButton, saturdayButton, sundayButton;

    // --- [기존 주석] 알람음 설정을 위한 UI 요소 및 변수 ---
    private SwitchMaterial alarmSoundSwitch;
    private TextView selectedSoundTextView; // 선택된 음악 파일의 이름을 보여줄 TextView

    // --- 비즈니스 로직 및 데이터 관련 --- //
    private AlarmViewModel alarmViewModel;
    private AlarmScheduler alarmScheduler;
    private Vibrator vibrator;

    // --- [기존 주석] 외부 Activity 결과 처리를 위한 런처 및 데이터 변수 ---
    private ActivityResultLauncher<Intent> pickSoundLauncher; // 파일 선택기 결과를 처리할 런처
    private Uri selectedSoundUri; // 사용자가 선택한 음악 파일의 URI를 임시로 저장하는 변수

    // --- [새로운 내용] 알람 수정 모드를 위한 변수 --- //
    /** 현재 '수정 모드'인지 여부를 나타내는 플래그. true이면 수정 모드, false이면 생성 모드입니다. */
    private boolean isEditMode = false;
    /** '수정 모드'일 경우, 수정 대상 알람의 ID를 저장합니다. 생성 모드일 경우 -1로 유지됩니다. */
    private int editingAlarmId = -1;


    /**
     * Activity가 생성될 때 가장 먼저 호출되는 메소드.
     * 앱의 초기 설정을 여기서 수행합니다.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_alarm);

        // [기존 주석 개선] 파일 선택기(SAF)의 결과를 처리할 런처를 초기화합니다.
        // 이 코드는 Activity의 생명주기상 UI가 생성되기 전, onCreate의 가장 앞부분에 위치하는 것이 안정적입니다.
        setupSoundPickerLauncher();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setAlarm), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
        alarmScheduler = new AlarmScheduler(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // 1. 모든 뷰(View)들을 초기화합니다.
        setupViews();
        // 2. 버튼 클릭 등 사용자의 입력을 받을 리스너들을 설정합니다.
        setupListeners();
        // 3. [새로운 내용] Intent를 확인하여 '수정 모드' 또는 '생성 모드'를 결정하고 그에 맞는 초기화를 수행합니다.
        handleIntent();
    }

    /**
     * [기존 주석] XML 레이아웃에 정의된 UI 뷰들을 찾아와 멤버 변수에 할당합니다.
     */
    private void setupViews() {
        timePicker = findViewById(R.id.timePicker);
        saveAlarmButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        vibrationSwitch = findViewById(R.id.vibrationSwitch);
        timePicker.setIs24HourView(false);

        mondayButton = findViewById(R.id.mondayToggle);
        tuesdayButton = findViewById(R.id.tuesdayToggle);
        wednesdayButton = findViewById(R.id.wednesdayToggle);
        thursdayButton = findViewById(R.id.thursdayToggle);
        fridayButton = findViewById(R.id.fridayToggle);
        saturdayButton = findViewById(R.id.saturdayToggle);
        sundayButton = findViewById(R.id.sundayToggle);

        // [기존 주석] 알람음 관련 UI 요소를 찾아옵니다.
        alarmSoundSwitch = findViewById(R.id.alarmSoundSwitch);
        selectedSoundTextView = findViewById(R.id.selectedSoundText);
    }

    /**
     * [기존 주석] 각종 버튼의 클릭 이벤트를 처리하는 리스너를 설정합니다.
     */
    private void setupListeners() {
        saveAlarmButton.setOnClickListener(v -> saveAlarm());
        cancelButton.setOnClickListener(v -> finish());

        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                playDefaultVibration();
            }
        });

        // [기존 주석 개선] 알람음 스위치의 상태 변경 리스너를 설정합니다.
        alarmSoundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // [새로운 내용] isChecked가 true이고, 사용자가 스위치를 직접 눌렀을 때만 파일 선택기를 엽니다.
            // 수정 모드에서 loadAlarmData()에 의해 프로그램이 스위치를 켤 때는 파일 선택기가 열리면 안되기 때문입니다.
            // isPressed()는 사용자의 터치에 의해 상태가 변경될 때 true를 반환합니다.
            if (isChecked && buttonView.isPressed()) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("audio/*");
                pickSoundLauncher.launch(intent);
            } else if (!isChecked) {
                // 스위치가 꺼지면, 선택했던 알람음 정보를 모두 초기화합니다.
                selectedSoundUri = null;
                updateSelectedSoundUI(null);
            }
        });
    }

    /**
     * [새로운 메소드] Activity 시작 시 전달받은 Intent를 처리하여 '수정 모드' 또는 '생성 모드'를 결정합니다.
     */
    private void handleIntent() {
        Intent intent = getIntent();
        // MainActivity로부터 ALARM_ID_EXTRA 키로 값이 전달되었는지 확인합니다.
        if (intent != null && intent.hasExtra(MainActivity.ALARM_ID_EXTRA)) {
            editingAlarmId = intent.getIntExtra(MainActivity.ALARM_ID_EXTRA, -1);
            if (editingAlarmId != -1) {
                // 유효한 ID가 있다면, '수정 모드'로 설정합니다.
                isEditMode = true;
                setTitle("알람 수정"); // Activity의 타이틀을 변경하여 사용자에게 현재 모드를 명확히 알려줍니다.
                Log.d(TAG, "알람 수정 모드로 시작. ID: " + editingAlarmId);
                // 데이터베이스에서 해당 알람 정보를 불러와 UI에 채웁니다.
                loadAlarmData(editingAlarmId);
            }
        } else {
            // 전달된 ID가 없다면, '알람 추가' 모드입니다.
            isEditMode = false;
            setTitle("알람 추가");
            Log.d(TAG, "알람 추가 모드로 시작.");
            // '알람 추가' 모드에서는, 알람이 DB에 저장된 후 생성된 ID를 관찰해야 합니다.
            observeNewAlarmId();
        }
    }

    /**
     * [새로운 메소드] '수정 모드'일 때, 데이터베이스에서 알람 정보를 가져와 UI에 표시합니다.
     * @param alarmId 수정할 알람의 ID
     */
    private void loadAlarmData(int alarmId) {
        // ViewModel을 통해 특정 ID의 알람 정보를 LiveData로 가져옵니다.
        // LiveData를 사용하면 데이터베이스 조회가 비동기적으로 처리되어 UI 멈춤 현상을 막을 수 있습니다.
        alarmViewModel.getAlarmById(alarmId).observe(this, alarm -> {
            if (alarm != null) {
                // 데이터베이스에서 알람 정보를 성공적으로 가져왔을 때 UI를 채웁니다.
                Log.d(TAG, "DB에서 알람(" + alarmId + ") 정보 로드 완료. UI를 채웁니다.");
                populateUiWithAlarmData(alarm);

                // [새로운 내용] 매우 중요! 데이터를 한 번 성공적으로 불러온 후에는, 더 이상 관찰(observe)을 계속할 필요가 없습니다.
                // 만약 이 코드가 없으면, 사용자가 화면에서 요일 버튼을 누르는 등 UI를 조작할 때마다
                // (내부적으로는 DB가 업데이트되고 LiveData가 변경되어) 이 부분이 다시 호출되면서
                // 사용자의 조작이 초기 상태로 다시 덮어쓰여지는 문제가 발생할 수 있습니다.
                alarmViewModel.getAlarmById(alarmId).removeObservers(this);
            }
        });
    }

    /**
     * [새로운 메소드] 전달받은 Alarm 객체의 데이터로 모든 UI 요소(시간, 요일, 스위치 등)를 설정합니다.
     * @param alarm UI에 표시할 데이터가 담긴 Alarm 객체
     */
    private void populateUiWithAlarmData(Alarm alarm) {
        timePicker.setHour(alarm.getHour());
        timePicker.setMinute(alarm.getMinute());

        vibrationSwitch.setChecked(alarm.isVibrationEnabled());

        // [핵심 수정] isMonday() -> isMondayEnabled() 와 같이 올바른 getter 메소드 이름을 사용합니다.
        mondayButton.setChecked(alarm.isMondayEnabled());
        tuesdayButton.setChecked(alarm.isTuesdayEnabled());
        wednesdayButton.setChecked(alarm.isWednesdayEnabled());
        thursdayButton.setChecked(alarm.isThursdayEnabled());
        fridayButton.setChecked(alarm.isFridayEnabled());
        saturdayButton.setChecked(alarm.isSaturdayEnabled());
        sundayButton.setChecked(alarm.isSundayEnabled());

        if (alarm.getSoundUri() != null && !alarm.getSoundUri().isEmpty()) {
            selectedSoundUri = Uri.parse(alarm.getSoundUri());
            alarmSoundSwitch.setChecked(true);
            updateSelectedSoundUI(selectedSoundUri);
        } else {
            selectedSoundUri = null;
            alarmSoundSwitch.setChecked(false);
            updateSelectedSoundUI(null);
        }
    }

    /**
     * [기존 주석 개선] 파일 선택기(SAF)의 결과를 처리하는 ActivityResultLauncher를 설정합니다.
     * 사용자가 음악 파일을 선택하고 돌아오면, 이 런처의 콜백(람다식)이 실행됩니다.
     */
    private void setupSoundPickerLauncher() {
        pickSoundLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // 파일 선택이 성공적으로 완료된 경우
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            // [기존 주석] 앱이 재시작되어도 파일에 접근할 수 있도록 영구적인 읽기 권한을 얻습니다.
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            // 선택된 URI를 멤버 변수에 저장하고, 파일 이름을 화면에 표시합니다.
                            this.selectedSoundUri = uri;
                            updateSelectedSoundUI(uri);
                        }
                    } else {
                        // 파일 선택을 취소했거나 실패한 경우
                        // [새로운 내용] 사용자가 파일 선택을 취소해도, 이전에 이미 설정된 알람음이 있었다면 스위치가 꺼지면 안됩니다.
                        // selectedSoundUri가 null일 때만 (즉, 처음 설정 시도 중 취소했을 때만) 스위치를 끕니다.
                        if (selectedSoundUri == null) {
                            alarmSoundSwitch.setChecked(false);
                        }
                    }
                }
        );
    }

    /**
     * [핵심 수정] '저장' 버튼을 눌렀을 때의 동작입니다.
     * '수정 모드'와 '생성 모드'를 구분하여 각각 다른 로직을 수행합니다.
     */
    private void saveAlarm() {
        // 현재 UI 상태로부터 알람 정보를 읽어옵니다.
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        boolean isVibrationEnabled = vibrationSwitch.isChecked();
        String soundUriString = (selectedSoundUri != null) ? selectedSoundUri.toString() : null;

        if (isEditMode) {
            // --- 수정 모드일 경우 ---
            Log.d(TAG, "수정 모드에서 저장 버튼 클릭됨. 알람 ID: " + editingAlarmId);
            // 기존 알람 ID(editingAlarmId)와 현재 UI의 설정값으로 Alarm 객체를 생성합니다.
            // 수정된 알람은 항상 활성화(true) 상태로 저장됩니다.
            Alarm updatedAlarm = new Alarm(
                    editingAlarmId, hour, minute, true, isVibrationEnabled, soundUriString,
                    mondayButton.isChecked(), tuesdayButton.isChecked(), wednesdayButton.isChecked(),
                    thursdayButton.isChecked(), fridayButton.isChecked(), saturdayButton.isChecked(), sundayButton.isChecked()
            );

            // 1. 데이터베이스에 알람 정보를 업데이트(UPDATE)합니다.
            alarmViewModel.update(updatedAlarm);
            // 2. 시스템에 예약된 기존 알람을 취소합니다. (ID가 동일하므로 PendingIntent가 같아 덮어쓰기 전에 취소하는 것이 안전합니다.)
            alarmScheduler.cancel(updatedAlarm);
            // 3. 수정된 정보로 시스템 알람을 다시 예약합니다.
            alarmScheduler.schedule(updatedAlarm);

            Toast.makeText(this, "알람이 수정되었습니다.", Toast.LENGTH_SHORT).show();
            finish(); // 수정을 완료하고 화면을 닫습니다.

        } else {
            // --- 생성 모드일 경우 (기존 로직과 거의 동일) ---
            Log.d(TAG, "생성 모드에서 저장 버튼 클릭됨.");
            // ID 없이 Alarm 객체를 생성합니다. ID는 데이터베이스에 삽입될 때 Room 라이브러리에 의해 자동 생성됩니다.
            Alarm newAlarm = new Alarm(
                    hour, minute, true, isVibrationEnabled, soundUriString,
                    mondayButton.isChecked(), tuesdayButton.isChecked(), wednesdayButton.isChecked(),
                    thursdayButton.isChecked(), fridayButton.isChecked(), saturdayButton.isChecked(), sundayButton.isChecked()
            );
            // ViewModel에게 이 새로운 알람 객체의 삽입(INSERT)을 요청합니다.
            // 이 요청 후에는 observeNewAlarmId()가 호출되어 시스템 알람 예약 등 후속 처리를 하게 됩니다.
            alarmViewModel.insert(newAlarm);
        }
    }

    /**
     * [기존 주석 개선] ViewModel의 newAlarmId LiveData를 관찰하여, ID가 생성되면 실제 시스템 알람을 예약합니다.
     * 이 메소드는 '생성 모드'에서만 호출되도록 handleIntent()에서 분기 처리되었습니다.
     */
    private void observeNewAlarmId() {
        alarmViewModel.getNewAlarmId().observe(this, newAlarmId -> {
            if (newAlarmId != null) {
                int alarmId = newAlarmId.intValue();
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();
                boolean isVibrationEnabled = vibrationSwitch.isChecked();
                String soundUriString = (selectedSoundUri != null) ? selectedSoundUri.toString() : null;

                // [기존 주석] ID와 알람음 URI까지 포함된 '완성된' Alarm 객체를 생성합니다.
                // [핵심 수정] isMonday() -> isMondayEnabled() 와 같이 올바른 getter 메소드 이름을 사용합니다.
                Alarm alarmToSchedule = new Alarm(
                        alarmId,
                        hour, minute, true, isVibrationEnabled, soundUriString,
                        mondayButton.isChecked(), tuesdayButton.isChecked(), wednesdayButton.isChecked(),
                        thursdayButton.isChecked(), fridayButton.isChecked(), saturdayButton.isChecked(), sundayButton.isChecked()
                );

                alarmScheduler.schedule(alarmToSchedule);

                Toast.makeText(this, String.format(Locale.getDefault(), "%s %02d:%02d 알람이 저장되었습니다.", (hour < 12 ? "오전" : "오후"), (hour == 0 || hour == 12) ? 12 : hour % 12, minute), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void playDefaultVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(500);
            }
        }
    }

    /**
     * [기존 주석] 선택된 알람음 파일의 이름을 UI에 표시하는 헬퍼 메소드입니다.
     * @param uri 음악 파일의 Uri. null일 경우 텍스트를 숨깁니다.
     */
    private void updateSelectedSoundUI(Uri uri) {
        if (uri != null) {
            String fileName = getFileNameFromUri(uri);
            selectedSoundTextView.setText(fileName);
            selectedSoundTextView.setVisibility(View.VISIBLE);
        } else {
            selectedSoundTextView.setText(null);
            selectedSoundTextView.setVisibility(View.GONE);
        }
    }

    /**
     * [기존 주석 개선] 파일의 Uri로부터 실제 파일 이름을 가져오는 헬퍼 메소드입니다.
     * 안드로이드의 ContentResolver를 통해 파일의 메타데이터(예: 표시 이름)를 조회합니다.
     * @param uri 파일의 Uri
     * @return 파일 이름 또는 이름을 찾지 못할 경우 null
     */
    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
            }
        }
        // [기존 주석 개선] ContentResolver로 이름을 못찾는 경우, URI의 마지막 부분을 이름으로 사용하려 시도할 수 있습니다.
        // 다만 이 방식은 항상 정확한 파일 이름을 보장하지는 않습니다.
        return fileName != null ? fileName : uri.getLastPathSegment();
    }
}
