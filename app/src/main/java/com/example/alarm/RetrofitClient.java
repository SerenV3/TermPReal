package com.example.alarm;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * [새로운 클래스] Retrofit 라이브러리의 인스턴스를 생성하고 관리하는 클래스입니다.
 * 앱 전체에서 단 하나의 Retrofit 인스턴스를 사용하도록 하여(싱글톤 패턴), 효율적인 네트워크 통신을 돕습니다.
 */
public class RetrofitClient {

    // [새로운 주석] Retrofit 인스턴스를 저장할 정적(static) 변수입니다. volatile 키워드는 멀티스레드 환경에서 안전하게 사용하기 위함입니다.
    private static volatile Retrofit retrofit = null;
    // [새로운 주석] OpenWeatherMap API의 기본(Base) URL입니다. 모든 API 요청은 이 주소를 기준으로 시작됩니다.
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";

    /**
     * [새로운 주석] WeatherApiService의 구현체를 제공하는 정적 메소드입니다.
     * 이 메소드를 통해 API 통신을 시작할 수 있습니다.
     * @return Retrofit이 자동으로 생성한 WeatherApiService 인터페이스의 구현체
     */
    public static WeatherApiService getApiService() {
        // [새로운 주석] Retrofit 인스턴스가 아직 생성되지 않았을 경우에만 새로 생성합니다. (더블 체크 락킹)
        if (retrofit == null) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null) {
                    // [새로운 주석] Retrofit.Builder를 사용하여 Retrofit 인스턴스를 설정하고 생성합니다.
                    retrofit = new Retrofit.Builder()
                            // 1. 베이스 URL을 설정합니다.
                            .baseUrl(BASE_URL)
                            // 2. JSON 데이터를 자바 객체로 변환해 줄 컨버터(Gson)를 추가합니다.
                            .addConverterFactory(GsonConverterFactory.create())
                            // 3. 설정이 완료된 Retrofit 인스턴스를 빌드합니다.
                            .build();
                }
            }
        }
        // [새로운 주석] 생성된 Retrofit 인스턴스를 사용하여 WeatherApiService 인터페이스의 구현체를 만듭니다.
        return retrofit.create(WeatherApiService.class);
    }
}
