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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
 * [기존 주석] 새로운 알람을 설정하거나 '기존 알람을 수정'하는 화면(Activity)입니다.
 */
public class SetAlarmActivity extends AppCompatActivity {

    private static final String TAG = "SetAlarmActivity";

    // --- UI 요소 --- //
    private TimePicker timePicker;
    private EditText alarmNameEditText;
    private ToggleButton mondayButton, tuesdayButton, wednesdayButton, thursdayButton, fridayButton, saturdayButton, sundayButton;
    private SwitchMaterial alarmSoundSwitch;
    private TextView selectedSoundTextView;
    // [새로운 내용] 날씨 TTS 스위치를 위한 UI 변수 선언
    private SwitchMaterial weatherTtsSwitch;
    private SwitchMaterial vibrationSwitch;
    private Button saveAlarmButton;
    private Button cancelButton;

    // --- 비즈니스 로직 및 데이터 관련 --- //
    private AlarmViewModel alarmViewModel;
    private AlarmScheduler alarmScheduler;
    private Vibrator vibrator;

    // --- 외부 Activity 결과 처리를 위한 런처 --- //
    private ActivityResultLauncher<Intent> pickSoundLauncher;
    // [새로운 내용] WeatherActivity의 결과를 처리할 런처를 선언합니다.
    private ActivityResultLauncher<Intent> weatherActivityLauncher;
    private Uri selectedSoundUri;

    // --- [기존 주석] 알람 수정 모드를 위한 변수 --- //
    private boolean isEditMode = false;
    private int editingAlarmId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_alarm);

        // [기존 주석] Activity가 생성될 때 런처들을 미리 초기화합니다.
        setupLaunchers();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setAlarm), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        alarmViewModel = new ViewModelProvider(this).get(AlarmViewModel.class);
        alarmScheduler = new AlarmScheduler(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        setupViews();
        setupListeners();
        handleIntent();
    }

    /**
     * [새로운 메소드] 화면 시작 시, 다른 Activity를 호출하고 그 결과를 받아 처리할 런처들을 설정합니다.
     */
    private void setupLaunchers() {
        // [기존 주석] 알람음 선택 결과를 처리하는 런처
        pickSoundLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            this.selectedSoundUri = uri;
                            updateSelectedSoundUI(uri);
                        }
                    } else {
                        if (selectedSoundUri == null) {
                            alarmSoundSwitch.setChecked(false);
                        }
                    }
                }
        );

        // [새로운 내용] 날씨 설정 화면(WeatherActivity)의 결과를 처리하는 런처를 등록합니다.
        weatherActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // WeatherActivity에서 설정이 완료되었는지(RESULT_OK) 여부를 확인합니다.
                    if (result.getResultCode() == RESULT_OK) {
                        // 완료되었다면, 날씨 TTS 기능이 활성화된 것으로 간주하고 스위치를 켠 상태로 유지합니다.
                        weatherTtsSwitch.setChecked(true);
                    } else {
                        // 완료되지 않았거나 사용자가 뒤로가기로 나왔다면, 스위치를 끈 상태로 되돌립니다.
                        weatherTtsSwitch.setChecked(false);
                    }
                }
        );
    }

    /**
     * [기존 주석, 내용 추가] XML 레이아웃의 모든 UI 뷰들을 찾아와 멤버 변수에 할당합니다.
     */
    private void setupViews() {
        timePicker = findViewById(R.id.timePicker);
        timePicker.setIs24HourView(false);

        alarmNameEditText = findViewById(R.id.alarmNameEditText);

        mondayButton = findViewById(R.id.mondayToggle);
        tuesdayButton = findViewById(R.id.tuesdayToggle);
        wednesdayButton = findViewById(R.id.wednesdayToggle);
        thursdayButton = findViewById(R.id.thursdayToggle);
        fridayButton = findViewById(R.id.fridayToggle);
        saturdayButton = findViewById(R.id.saturdayToggle);
        sundayButton = findViewById(R.id.sundayToggle);

        alarmSoundSwitch = findViewById(R.id.alarmSoundSwitch);
        selectedSoundTextView = findViewById(R.id.selectedSoundText);

        // [새로운 내용] 날씨 TTS 스위치 뷰를 코드와 연결합니다.
        weatherTtsSwitch = findViewById(R.id.weatherTtsSwitch);

        vibrationSwitch = findViewById(R.id.vibrationSwitch);
        saveAlarmButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
    }

    /**
     * [기존 주석, 내용 추가] 각종 UI 요소의 이벤트를 처리하는 리스너를 설정합니다.
     */
    private void setupListeners() {
        saveAlarmButton.setOnClickListener(v -> saveAlarm());
        cancelButton.setOnClickListener(v -> finish());

        alarmSoundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && buttonView.isPressed()) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("audio/*");
                pickSoundLauncher.launch(intent);
            } else if (!isChecked) {
                selectedSoundUri = null;
                updateSelectedSoundUI(null);
            }
        });

        // [새로운 내용] 날씨 TTS 스위치의 상태 변경 리스너를 설정합니다.
        weatherTtsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // [새로운 주석] isChecked: 스위치가 켜졌는지(true) 꺼졌는지(false) 나타냅니다.
            // buttonView.isPressed(): 사용자가 직접 터치하여 상태를 변경했을 때만 true가 됩니다.
            // (수정모드에서 DB값으로 스위치를 켤 때 원치 않게 화면이 넘어가는 것을 방지합니다.)
            if (isChecked && buttonView.isPressed()) {
                // [새로운 주석] 스위치가 켜지면, WeatherActivity를 시작합니다.
                Intent intent = new Intent(this, WeatherActivity.class);
                // [새로운 주석] 위에서 등록한 런처를 사용해 Activity를 시작합니다. 결과를 받아오기 위함입니다.
                weatherActivityLauncher.launch(intent);
            }
        });

        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                playDefaultVibration();
            }
        });
    }

    /**
     * [기존 주석] Activity 시작 시 전달받은 Intent를 처리하여 모드를 결정합니다.
     */
    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(MainActivity.ALARM_ID_EXTRA)) {
            editingAlarmId = intent.getIntExtra(MainActivity.ALARM_ID_EXTRA, -1);
            if (editingAlarmId != -1) {
                isEditMode = true;
                setTitle("알람 수정");
                loadAlarmData(editingAlarmId);
            }
        } else {
            isEditMode = false;
            setTitle("알람 추가");
            observeNewAlarmId();
        }
    }

    /**
     * [기존 주석] '수정 모드'일 때, DB에서 알람 정보를 가져와 UI에 표시합니다.
     */
    private void loadAlarmData(int alarmId) {
        alarmViewModel.getAlarmById(alarmId).observe(this, alarm -> {
            if (alarm != null) {
                populateUiWithAlarmData(alarm);
                alarmViewModel.getAlarmById(alarmId).removeObservers(this);
            }
        });
    }

    /**
     * [기존 주석, 내용 추가] Alarm 객체 데이터로 모든 UI 요소를 설정합니다.
     */
    private void populateUiWithAlarmData(Alarm alarm) {
        timePicker.setHour(alarm.getHour());
        timePicker.setMinute(alarm.getMinute());

        if (alarm.getName() != null) {
            alarmNameEditText.setText(alarm.getName());
        }

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

        // [새로운 내용] DB에서 가져온 날씨 TTS 설정값으로 스위치의 초기 상태를 설정합니다.
        weatherTtsSwitch.setChecked(alarm.isWeatherTtsEnabled());

        vibrationSwitch.setChecked(alarm.isVibrationEnabled());
    }

    /**
     * [기존 주석] '저장' 버튼을 눌렀을 때의 동작입니다.
     */
    private void saveAlarm() {
        String alarmName = alarmNameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(alarmName)) {
            alarmName = null;
        }

        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        boolean isVibrationEnabled = vibrationSwitch.isChecked();
        String soundUriString = (selectedSoundUri != null) ? selectedSoundUri.toString() : null;
        // [새로운 내용] 날씨 TTS 스위치의 현재 상태를 변수에 저장합니다.
        boolean isWeatherTtsEnabled = weatherTtsSwitch.isChecked();

        if (isEditMode) {
            Log.d(TAG, "수정 모드에서 저장 버튼 클릭됨. 알람 ID: " + editingAlarmId);
            // [새로운 내용] Alarm 객체 생성자에 isWeatherTtsEnabled 값을 추가하여 전달합니다.
            Alarm updatedAlarm = new Alarm(
                    editingAlarmId, alarmName, hour, minute, true, isVibrationEnabled, soundUriString,
                    mondayButton.isChecked(), tuesdayButton.isChecked(), wednesdayButton.isChecked(),
                    thursdayButton.isChecked(), fridayButton.isChecked(), saturdayButton.isChecked(), sundayButton.isChecked(),
                    isWeatherTtsEnabled // 날씨 TTS 설정값 추가
            );

            alarmViewModel.update(updatedAlarm);
            alarmScheduler.cancel(updatedAlarm);
            alarmScheduler.schedule(updatedAlarm);

            Toast.makeText(this, "알람이 수정되었습니다.", Toast.LENGTH_SHORT).show();
            finish();

        } else {
            Log.d(TAG, "생성 모드에서 저장 버튼 클릭됨.");
            // [새로운 내용] Alarm 객체 생성자에 isWeatherTtsEnabled 값을 추가하여 전달합니다.
            Alarm newAlarm = new Alarm(
                    alarmName, hour, minute, true, isVibrationEnabled, soundUriString,
                    mondayButton.isChecked(), tuesdayButton.isChecked(), wednesdayButton.isChecked(),
                    thursdayButton.isChecked(), fridayButton.isChecked(), saturdayButton.isChecked(), sundayButton.isChecked(),
                    isWeatherTtsEnabled // 날씨 TTS 설정값 추가
            );
            alarmViewModel.insert(newAlarm);
        }
    }

    /**
     * [기존 주석] 새 알람 ID를 관찰하여 시스템 알람을 예약합니다.
     */
    private void observeNewAlarmId() {
        alarmViewModel.getNewAlarmId().observe(this, newAlarmId -> {
            if (newAlarmId != null) {
                String alarmName = alarmNameEditText.getText().toString().trim();
                if (TextUtils.isEmpty(alarmName)) {
                    alarmName = null;
                }

                int alarmId = newAlarmId.intValue();
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();
                boolean isVibrationEnabled = vibrationSwitch.isChecked();
                String soundUriString = (selectedSoundUri != null) ? selectedSoundUri.toString() : null;
                // [새로운 내용] 날씨 TTS 스위치의 현재 상태를 변수에 저장합니다.
                boolean isWeatherTtsEnabled = weatherTtsSwitch.isChecked();

                // [새로운 내용] Alarm 객체 생성 시 isWeatherTtsEnabled 값을 추가하여 완전한 객체를 만듭니다.
                Alarm alarmToSchedule = new Alarm(
                        alarmId, alarmName,
                        hour, minute, true, isVibrationEnabled, soundUriString,
                        mondayButton.isChecked(), tuesdayButton.isChecked(), wednesdayButton.isChecked(),
                        thursdayButton.isChecked(), fridayButton.isChecked(), saturdayButton.isChecked(), sundayButton.isChecked(),
                        isWeatherTtsEnabled // 날씨 TTS 설정값 추가
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
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(500);
            }
        }
    }

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
        return fileName != null ? fileName : uri.getLastPathSegment();
    }
}
