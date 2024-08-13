package com.example.speeddetectorsensorapp;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.widget.TextView;

public class SpeedCalculator implements SensorEventListener {
    private static SpeedCalculator instance;
    private SpeedUnit speedUnit;
    private KalmanFilter kalmanFilter;
    private TextView speedValueTextView;
    private TextView speedUnitTextView;

    private static final float ALPHA = 0.8f;
    private static final float STOP_DETECTION_THRESHOLD = 0.1f;
    private static final float IMMEDIATE_STOP_THRESHOLD = 0.5f;
    private static final float MAX_ACCELERATION = 30.0f;
    private static final float MAX_SPEED = 100.0f;
    private static final int STOP_DETECTION_SAMPLES = 10;
    private static final float FRICTION_COEFFICIENT = 0.98f;
    private static final float SPEED_DECAY_FACTOR = 0.8f;

    private float[] gravity = new float[3];
    private float[] linearAcceleration = new float[3];

    private float currentAcceleration = 0;
    private float currentSpeed = 0;
    private long lastUpdateTime;
    private int lowAccelerationCount = 0;

    private SpeedCalculator() {
        this.speedUnit = new MsSpeedUnit(); // Default unit
        this.kalmanFilter = new KalmanFilter();
        lastUpdateTime = System.nanoTime();
    }
    //Singleton Design Pattern
    public static synchronized SpeedCalculator getInstance() {
        if (instance == null) {
            instance = new SpeedCalculator();
        }
        return instance;
    }

    public void initialize(TextView speedValueTextView, TextView speedUnitTextView) {
        this.speedValueTextView = speedValueTextView;
        this.speedUnitTextView = speedUnitTextView;
    }

    public void setSpeedUnit(SpeedUnit speedUnit) {
        this.speedUnit = speedUnit;
        updateSpeedDisplay();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.nanoTime();
            float deltaTime = (currentTime - lastUpdateTime) / 1e9f;

            updateGravity(event.values);
            updateLinearAcceleration(event.values);

            float instantAcceleration = calculateInstantAcceleration();
            currentAcceleration = kalmanFilter.update(instantAcceleration);
            currentAcceleration = Math.min(currentAcceleration, MAX_ACCELERATION);

            updateSpeed(deltaTime);

            lastUpdateTime = currentTime;
            updateSpeedDisplay();
        }
    }

    private void updateGravity(float[] values) {
        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * values[0];
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * values[1];
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * values[2];
    }

    private void updateLinearAcceleration(float[] values) {
        linearAcceleration[0] = values[0] - gravity[0];
        linearAcceleration[1] = values[1] - gravity[1];
        linearAcceleration[2] = values[2] - gravity[2];
    }

    private float calculateInstantAcceleration() {
        return (float) Math.sqrt(
                linearAcceleration[0] * linearAcceleration[0] +
                        linearAcceleration[1] * linearAcceleration[1] +
                        linearAcceleration[2] * linearAcceleration[2]
        );
    }

    private void updateSpeed(float deltaTime) {
        float speedChange = currentAcceleration * deltaTime;

        if (Math.abs(currentAcceleration) < IMMEDIATE_STOP_THRESHOLD) {
            currentSpeed *= SPEED_DECAY_FACTOR;
            lowAccelerationCount++;
            if (lowAccelerationCount >= STOP_DETECTION_SAMPLES) {
                currentSpeed = 0;
            }
        } else {
            currentSpeed += speedChange;
            lowAccelerationCount = 0;
        }

        currentSpeed *= FRICTION_COEFFICIENT;
        currentSpeed = Math.max(0, Math.min(currentSpeed, MAX_SPEED));
    }

    private void updateSpeedDisplay() {
        if (speedValueTextView != null && speedUnitTextView != null) {
            float convertedSpeed = speedUnit.convert(currentSpeed);
            speedValueTextView.post(() -> {
                speedValueTextView.setText(String.format("%.1f", convertedSpeed));
                speedUnitTextView.setText(speedUnit.getUnitSuffix());
            });
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used in this example
    }
}
