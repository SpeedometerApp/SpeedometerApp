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
    private float currentSpeed = 0;

    private KalmanFilter kalmanFilter;

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
        sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

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
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                float acceleration = (float) Math.sqrt(x * x + y * y + z * z);
                currentSpeed = calculateSpeed(acceleration);
                updateSpeedDisplay();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used in this example
        }
    };

    private float calculateSpeed(float acceleration) {
        // Subtract Earth's gravity to focus on actual device movement
        float filteredAcceleration = kalmanFilter.update(Math.abs(acceleration - SensorManager.GRAVITY_EARTH));
        return filteredAcceleration;
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
                return " m/s";
            default:
                return "";
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
        sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }
}