package com.example.testalarmreal02;

import java.util.Locale;

public class Alarm {
    private int hour;
    private int minute;
    private boolean onOff;

    public Alarm(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
        this.onOff = true; // 기본값 on으로 설정
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public boolean isOnOff() {
        return onOff;
    }

    public String getAmPm() {
        return hour < 12 ? "오전" : "오후";
    }

    public String getFormattedTime() {
        int displayHour = (hour == 0 || hour == 12) ? 12 : hour % 12;
        return String.format(Locale.getDefault(), "%02d:%02d", displayHour, minute);
    }

}
