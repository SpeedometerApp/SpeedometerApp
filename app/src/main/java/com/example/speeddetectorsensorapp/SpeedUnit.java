package com.example.speeddetectorsensorapp;

public interface SpeedUnit {
    float convert(float speedMps);
    String getUnitSuffix();
}
