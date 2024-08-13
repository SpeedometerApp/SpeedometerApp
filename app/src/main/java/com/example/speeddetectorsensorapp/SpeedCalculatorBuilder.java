package com.example.speeddetectorsensorapp;

import android.widget.TextView;

public class SpeedCalculatorBuilder {
    private TextView speedValueTextView;
    private TextView speedUnitTextView;
    private SpeedUnit speedUnit;

    public SpeedCalculatorBuilder setSpeedValueTextView(TextView speedValueTextView) {
        this.speedValueTextView = speedValueTextView;
        return this;
    }

    public SpeedCalculatorBuilder setSpeedUnitTextView(TextView speedUnitTextView) {
        this.speedUnitTextView = speedUnitTextView;
        return this;
    }

    public SpeedCalculatorBuilder setSpeedUnit(SpeedUnit speedUnit) {
        this.speedUnit = speedUnit;
        return this;
    }

    public SpeedCalculator build() {
        SpeedCalculator calculator = SpeedCalculator.getInstance();
        calculator.initialize(speedValueTextView, speedUnitTextView);
        if (speedUnit != null) {
            calculator.setSpeedUnit(speedUnit);
        }
        return calculator;
    }
}
