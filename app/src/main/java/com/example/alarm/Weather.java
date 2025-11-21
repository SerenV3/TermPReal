package com.example.alarm;

import com.google.gson.annotations.SerializedName;

/**
 * [새로운 클래스] OpenWeatherMap API 응답의 'weather' 배열에 포함된 객체를 나타내는 데이터 모델입니다.
 * 날씨에 대한 주요 설명(예: "Clear", "Rain")과 상세 설명(예: "맑은 하늘", "가벼운 비")을 담습니다.
 */
public class Weather {

    /**
     * [새로운 주석] @SerializedName 어노테이션은 JSON 데이터의 키 이름과 자바 필드 변수 이름이 다를 때,
     * Gson 라이브러리가 두 이름을 올바르게 매핑할 수 있도록 돕는 역할을 합니다.
     * 예를 들어, JSON 응답에 "main"이라는 키가 있으면, 그 값을 이 main 필드에 넣어줍니다.
     */

    @SerializedName("main")
    private String main;

    @SerializedName("description")
    private String description;

    @SerializedName("icon")
    private String icon;

    // --- Getter 메소드 --- //

    public String getMain() {
        return main;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }
}
