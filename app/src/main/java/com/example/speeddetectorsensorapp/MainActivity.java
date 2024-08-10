package com.example.speeddetectorsensorapp;

import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {

    private SensorManager sensorManager;
    private TextView speedTextView;
    private Button kmhButton, knotsButton, msButton;
    private ToggleButton toggleThemeButton;

    private String currentUnit = "ms"; // Default unit
    private static final String PREFS_NAME = "SpeedDetectorPrefs";

    private KalmanFilter kalmanFilter;

    private static final float ALPHA = 0.8f; // Smoothing factor for low-pass filter
    private static final float STOP_DETECTION_THRESHOLD = 0.1f; // Threshold for stop detection (m/s^2)
    private static final float IMMEDIATE_STOP_THRESHOLD = 0.5f; // Threshold for immediate stop detection (m/s^2)
    private static final float MAX_ACCELERATION = 30.0f; // Maximum acceleration in m/s^2
    private static final float MAX_SPEED = 100.0f; // Maximum speed in m/s
    private static final int STOP_DETECTION_SAMPLES = 10; // Number of samples to consider for stop detection
    private static final float FRICTION_COEFFICIENT = 0.98f; // Friction coefficient to simulate deceleration
    private static final float SPEED_DECAY_FACTOR = 0.8f; // Factor to quickly reduce speed when stopping

    private float[] gravity = new float[3];
    private float[] linearAcceleration = new float[3];

    private float currentAcceleration = 0;
    private float currentSpeed = 0;
    private long lastUpdateTime;
    private int lowAccelerationCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applyTheme();
        setContentView(R.layout.activity_main);

        speedTextView = findViewById(R.id.speedTextView);
        kmhButton = findViewById(R.id.kmhButton);
        knotsButton = findViewById(R.id.knotsButton);
        msButton = findViewById(R.id.msButton);
        toggleThemeButton = findViewById(R.id.toggleThemeButton);
        toggleThemeButton.setChecked(isDarkMode());

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        kalmanFilter = new KalmanFilter();

        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);

        kmhButton.setOnClickListener(view -> {
            currentUnit = "kmh";
            updateSpeedDisplay();
        });

        knotsButton.setOnClickListener(view -> {
            currentUnit = "knots";
            updateSpeedDisplay();
        });

        msButton.setOnClickListener(view -> {
            currentUnit = "ms";
            updateSpeedDisplay();
        });

        toggleThemeButton.setOnClickListener(view -> {
            boolean isChecked = ((ToggleButton) view).isChecked();
            setTheme(isChecked);
        });

        lastUpdateTime = System.nanoTime();
    }

    private void applyTheme() {
        boolean isDarkMode = isDarkMode();
        AppCompatDelegate.setDefaultNightMode(isDarkMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private boolean isDarkMode() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return preferences.getBoolean("isDarkMode", false);
    }

    private void setTheme(boolean isDarkMode) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isDarkMode", isDarkMode);
        editor.apply();

        AppCompatDelegate.setDefaultNightMode(isDarkMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        recreate();
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long currentTime = System.nanoTime();
                float deltaTime = (currentTime - lastUpdateTime) / 1e9f; // Convert to seconds

                // Isolate the force of gravity with the low-pass filter.
                gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * event.values[0];
                gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * event.values[1];
                gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * event.values[2];

                // Remove the gravity contribution with the high-pass filter.
                linearAcceleration[0] = event.values[0] - gravity[0];
                linearAcceleration[1] = event.values[1] - gravity[1];
                linearAcceleration[2] = event.values[2] - gravity[2];

                float instantAcceleration = (float) Math.sqrt(
                        linearAcceleration[0] * linearAcceleration[0] +
                                linearAcceleration[1] * linearAcceleration[1] +
                                linearAcceleration[2] * linearAcceleration[2]
                );

                // Apply Kalman filter to smooth acceleration
                currentAcceleration = kalmanFilter.update(instantAcceleration);

                // Limit acceleration to a reasonable range
                currentAcceleration = Math.min(currentAcceleration, MAX_ACCELERATION);

                // Update speed
                updateSpeed(deltaTime);

                lastUpdateTime = currentTime;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used in this example
        }
    };

    private void updateSpeed(float deltaTime) {
        // Calculate speed change based on acceleration
        float speedChange = currentAcceleration * deltaTime;

        // Check for immediate stop
        if (Math.abs(currentAcceleration) < IMMEDIATE_STOP_THRESHOLD) {
            // Quickly reduce speed
            currentSpeed *= SPEED_DECAY_FACTOR;
            lowAccelerationCount++;
            if (lowAccelerationCount >= STOP_DETECTION_SAMPLES) {
                currentSpeed = 0;
            }
        } else {
            // Apply speed change
            //v=prevSpeed + a*t
            currentSpeed += speedChange;
            lowAccelerationCount = 0;
        }

        // Apply friction to simulate deceleration
        currentSpeed *= FRICTION_COEFFICIENT;

        // Ensure speed is never negative
        currentSpeed = Math.max(0, currentSpeed);

        // Limit speed to a reasonable range
        currentSpeed = Math.min(currentSpeed, MAX_SPEED);

        updateSpeedDisplay();
    }

    private float convertSpeed(float speedMps, String unit) {
        switch (unit) {
            case "kmh":
                return speedMps * 3.6f;
            case "knots":
                return speedMps * 1.94384f;
            case "ms":
            default:
                return speedMps;
        }
    }

    private String getUnitSuffix(String unit) {
        switch (unit) {
            case "kmh":
                return " km/h";
            case "knots":
                return " knots";
            case "ms":
            default:
                return " m/s";
        }
    }

    private void updateSpeedDisplay() {
        float displaySpeed = convertSpeed(currentSpeed, currentUnit);
        runOnUiThread(() -> speedTextView.setText(String.format("Speed: %.2f%s", displaySpeed, getUnitSuffix(currentUnit))));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        lastUpdateTime = System.nanoTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }
}