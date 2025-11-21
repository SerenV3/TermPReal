package com.example.alarm;

import com.google.gson.annotations.SerializedName;

/**
 * [새로운 클래스] OpenWeatherMap API 응답의 'main' 객체를 나타내는 데이터 모델입니다.
 * 현재 온도, 체감 온도, 습도 등의 주요 수치 정보를 담습니다.
 */
public class MainWeatherData {

    /**
     * [새로운 주석] @SerializedName 어노테이션은 JSON 데이터의 키 이름(예: "temp")과
     * 자바 필드 변수 이름(예: temperature)이 다를 때 둘을 매핑해주는 역할을 합니다.
     * 이를 통해 더 명확하고 이해하기 쉬운 변수 이름을 사용할 수 있습니다.
     */

    @SerializedName("temp")
    private double temperature;

    @SerializedName("feels_like")
    private double feelsLike;

    @SerializedName("temp_min")
    private double tempMin;

    @SerializedName("temp_max")
    private double tempMax;

    @SerializedName("pressure")
    private int pressure;

    @SerializedName("humidity")
    private int humidity;

    // --- Getter 메소드 --- //

    public double getTemperature() {
        return temperature;
    }

    public double getFeelsLike() {
        return feelsLike;
    }

    public double getTempMin() {
        return tempMin;
    }

    public double getTempMax() {
        return tempMax;
    }

    public int getPressure() {
        return pressure;
    }

    public int getHumidity() {
        return humidity;
    }
}
