package com.example.alarm;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;



// [새로운 내용] 자동으로 생성된 BuildConfig 클래스를 명시적으로 import하여 IDE가 찾을 수 있도록 합니다.
import com.example.alarm.BuildConfig;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * [기존 주석, 내용 확장] 날씨 정보를 표시하고, 날씨 연동 TTS 기능을 최종 설정하는 화면입니다.
 */
public class WeatherActivity extends AppCompatActivity implements View.OnClickListener {

    //뒤로가기 버튼
    ImageButton revertBtn;

    private static final String TAG = "WeatherActivity";

    // --- UI 요소 --- //
    private TextView weatherInfoTextView;
    private SwitchMaterial confirmWeatherTtsSwitch;

    // --- 위치 정보 관련 --- //
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // [새로운 내용] 권한 요청 결과를 처리할 런처를 초기화합니다.
        setupPermissionLauncher();

        // [새로운 내용] 위치 서비스 클라이언트를 초기화합니다.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // [기존 주석] 시스템 UI와 충돌하지 않도록 여백을 설정합니다.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.weatherActivityLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // [기존 주석] UI 뷰들을 찾아와 멤버 변수에 할당합니다.
        weatherInfoTextView = findViewById(R.id.weatherInfoTextView);
        confirmWeatherTtsSwitch = findViewById(R.id.confirmWeatherTtsSwitch);
        revertBtn = findViewById(R.id.weather_revertButton);
        revertBtn.setOnClickListener(this);

        // [새로운 내용] 설정 완료 스위치의 리스너를 설정합니다.
        setupListeners();

        // [새로운 내용] 화면이 생성되자마자 위치 권한을 확인하고 날씨 정보를 가져오는 과정을 시작합니다.
        checkLocationPermissionAndGetWeather();
    }

    /**
     * [새로운 메소드] 권한 요청 및 그 결과를 처리하는 ActivityResultLauncher를 설정합니다.
     */
    private void setupPermissionLauncher() {
        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
            Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

            if (fineLocationGranted != null && fineLocationGranted) {
                // [새로운 주석] 정확한 위치 권한이 승인되면, 다시 날씨 정보를 가져옵니다.
                getLastKnownLocation();
            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                // [새로운 주석] 대략적인 위치 권한만 승인되어도, 날씨 정보를 가져옵니다.
                getLastKnownLocation();
            } else {
                // [새로운 주석] 권한이 거부되면 사용자에게 알리고 화면을 종료합니다.
                Toast.makeText(this, "위치 권한이 거부되어 날씨 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * [새로운 메소드] 설정 완료 스위치의 클릭 이벤트를 처리하는 리스너를 설정합니다.
     */
    private void setupListeners() {
        confirmWeatherTtsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // [새로운 주석] 스위치가 켜지면, SetAlarmActivity에 "성공" 결과를 돌려주고 현재 화면을 종료합니다.
                Intent resultIntent = new Intent();
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                // [새로운 주석] 스위치가 꺼지면, "실패" 또는 "취소" 결과를 돌려줍니다.
                setResult(RESULT_CANCELED);
            }
        });
    }

    /**
     * [새로운 메소드] 위치 정보 접근 권한을 확인하고, 없으면 요청합니다.
     */
    private void checkLocationPermissionAndGetWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // [새로운 주석] 권한이 이미 있으면, 마지막으로 알려진 위치를 가져옵니다.
            getLastKnownLocation();
        } else {
            // [새로운 주석] 권한이 없으면, 사용자에게 권한을 요청하는 다이얼로그를 띄웁니다.
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    /**
     * [새로운 메소드] 마지막으로 알려진 사용자의 위치를 가져옵니다.
     */
    private void getLastKnownLocation() {
        // [새로운 주석] 보안 검사를 통과하지 못하면(권한이 없으면) 메소드를 즉시 종료합니다.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // 이 코드는 checkLocationPermissionAndGetWeather 에서 이미 확인했으므로 거의 실행되지 않습니다.
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                // [새로운 주석] 위치 정보를 성공적으로 가져왔으면, 해당 위치로 날씨 정보를 요청합니다.
                fetchWeatherInfo(location.getLatitude(), location.getLongitude());
            } else {
                // [새로운 주석] 마지막 위치 정보가 없는 경우(예: 기기 재부팅 직후, GPS 비활성화 등)
                weatherInfoTextView.setText("위치 정보를 가져올 수 없습니다. GPS를 활성화하고 잠시 후 다시 시도해주세요.");
            }
        });
    }

    /**
     * [새로운 메소드] Retrofit을 사용하여 OpenWeatherMap API에 날씨 정보를 요청합니다.
     * @param latitude 위도
     * @param longitude 경도
     */
    private void fetchWeatherInfo(double latitude, double longitude) {
        // [새로운 주석] Retrofit 클라이언트를 통해 API 서비스의 구현체를 가져옵니다.
        WeatherApiService apiService = RetrofitClient.getApiService();

        // [새로운 주석] API 키는 BuildConfig 클래스에서 안전하게 가져옵니다.
        String apiKey = BuildConfig.WEATHER_API_KEY;

        // [새로운 주석] API 요청을 생성합니다. 아직 실제 통신이 시작된 것은 아닙니다.
        Call<WeatherResponse> call = apiService.getCurrentWeather(latitude, longitude, apiKey, "metric", "kr");

        // [새로운 주석] 비동기 방식으로 네트워크 요청을 실행합니다.
        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // [새로운 주석] 서버로부터 성공적으로 응답을 받았을 때의 처리
                    WeatherResponse weatherResponse = response.body();
                    updateUiWithWeatherInfo(weatherResponse);
                } else {
                    // [새로운 주석] 서버로부터 오류 응답을 받았을 때 (예: 404 Not Found)
                    weatherInfoTextView.setText("날씨 정보를 가져오는 데 실패했습니다. (오류 코드: " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                // [새로운 주석] 네트워크 통신 자체에 실패했을 때 (예: 인터넷 연결 없음)
                Log.e(TAG, "네트워크 오류", t);
                weatherInfoTextView.setText("네트워크 오류가 발생했습니다. 인터넷 연결을 확인해주세요.");
            }
        });
    }

    /**
     * [새로운 메소드] 성공적으로 받아온 날씨 정보(WeatherResponse)를 바탕으로 UI를 업데이트합니다.
     * @param weatherResponse API로부터 받은 날씨 데이터 객체
     */
    private void updateUiWithWeatherInfo(WeatherResponse weatherResponse) {
        if (weatherResponse != null && weatherResponse.getMainWeatherData() != null && weatherResponse.getWeather() != null && !weatherResponse.getWeather().isEmpty()) {
            String cityName = weatherResponse.getCityName();
            String weatherDescription = weatherResponse.getWeather().get(0).getDescription();
            double temperature = weatherResponse.getMainWeatherData().getTemperature();

            // [새로운 주석] 최종적으로 화면에 보여줄 텍스트를 조합합니다.
            String weatherText = String.format(Locale.KOREAN,
                    "현재 위치(%s)의 날씨는 '%s'이며, 기온은 %.1f℃ 입니다.",
                    cityName, weatherDescription, temperature);

            weatherInfoTextView.setText(weatherText);
        } else {
            weatherInfoTextView.setText("날씨 정보 형식이 올바르지 않습니다.");
        }
    }


    @Override
        public void onClick(View v) {
        if(v.getId()==R.id.weather_revertButton)
            finish();
    }

}
