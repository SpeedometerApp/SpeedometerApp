package com.example.speeddetectorsensorapp;

public class KnotsSpeedUnit implements SpeedUnit {
    @Override
    public float convert(float speedMps) {
        return speedMps * 1.94384f;
    }

    @Override
    public String getUnitSuffix() {
        return "Knots";
    }
}