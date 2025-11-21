package com.example.alarm;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * [새로운 클래스] OpenWeatherMap API의 전체 응답을 나타내는 최상위 데이터 모델입니다.
 * 이 클래스는 다른 데이터 모델들(Weather, MainWeatherData)을 멤버로 포함합니다.
 */
public class WeatherResponse {

    /**
     * [새로운 주석] @SerializedName 어노테이션은 JSON의 키와 자바 필드를 매핑합니다.
     * 예를 들어, JSON 응답에 "weather": [...] 부분이 있으면, 그 배열의 내용을 이 List<Weather> weather 필드에 넣어줍니다.
     */

    // JSON의 "weather": [...] 배열에 해당합니다.
    @SerializedName("weather")
    private List<Weather> weather;

    // JSON의 "main": {...} 객체에 해당합니다.
    @SerializedName("main")
    private MainWeatherData mainWeatherData;

    // JSON의 "name": "Seoul" 과 같은 도/시 이름에 해당합니다.
    @SerializedName("name")
    private String cityName;

    // --- Getter 메소드 --- //

    public List<Weather> getWeather() {
        return weather;
    }

    public MainWeatherData getMainWeatherData() {
        return mainWeatherData;
    }

    public String getCityName() {
        return cityName;
    }
}
