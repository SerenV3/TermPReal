package com.example.alarm;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * [새로운 인터페이스] Retrofit을 사용하여 날씨 API와 통신하는 방법을 정의하는 명세서입니다.
 * Retrofit은 이 인터페이스의 구현체를 자동으로 생성하여 네트워크 통신을 처리합니다.
 */
public interface WeatherApiService {

    /**
     * [새로운 주석] @GET 어노테이션은 HTTP GET 요청을 보낼 경로를 지정합니다.
     * 여기서는 베이스 URL에 "weather"가 추가된 주소(예: https://api.openweathermap.org/data/2.5/weather)로 요청을 보냅니다.
     *
     * @param latitude 위도 (API 요청 시 'lat' 쿼리 파라미터로 전달됨)
     * @param longitude 경도 (API 요청 시 'lon' 쿼리 파라미터로 전달됨)
     * @param apiKey API 키 (API 요청 시 'appid' 쿼리 파라미터로 전달됨)
     * @param units 온도 단위 (metric: 섭씨, imperial: 화씨) - 'units' 파라미터로 전달
     * @param lang 언어 (예: "kr"-한국어, "en"-영어) - 'lang' 파라미터로 전달
     * @return 서버의 응답을 WeatherResponse 객체로 변환하여 담고 있는 Call 객체. 이 Call 객체를 실행하면 실제 네트워크 요청이 시작됩니다.
     */
    @GET("weather")
    Call<WeatherResponse> getCurrentWeather(
            @Query("lat") double latitude,
            @Query("lon") double longitude,
            @Query("appid") String apiKey,
            @Query("units") String units,
            @Query("lang") String lang
    );
}
