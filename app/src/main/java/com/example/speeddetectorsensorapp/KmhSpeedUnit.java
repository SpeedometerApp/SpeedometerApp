package com.example.speeddetectorsensorapp;

public class KmhSpeedUnit implements SpeedUnit {
    @Override
    public float convert(float speedMps) {
        return speedMps * 3.6f;
    }

    @Override
    public String getUnitSuffix() {
        return "Km/h";
    }
}