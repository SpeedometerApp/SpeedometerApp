package com.example.speeddetectorsensorapp;

public class MsSpeedUnit implements SpeedUnit {
    @Override
    public float convert(float speedMps) {
        return speedMps;
    }

    @Override
    public String getUnitSuffix() {
        return "m/s";
    }
}