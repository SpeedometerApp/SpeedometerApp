package com.example.speeddetectorsensorapp;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private SensorManager sensorManager;
    private TextView speedTextView;
    private Button kmhButton, knotsButton, msButton;
    private ToggleButton toggleThemeButton;

    private String currentUnit = "knots"; // Default unit
    private static final String PREFS_NAME = "MyPrefsFile";

    private long previousTime;
    private float currentSpeed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply the current theme before setting the content view
        applyTheme();
        setContentView(R.layout.activity_main);

        speedTextView = findViewById(R.id.speedTextView);
        kmhButton = findViewById(R.id.kmhButton);
        knotsButton = findViewById(R.id.knotsButton);
        msButton = findViewById(R.id.msButton);
        toggleThemeButton = findViewById(R.id.toggleThemeButton);
        toggleThemeButton.setChecked(isDarkMode()); // Set initial state based on saved preference

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Register accelerometer sensor
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);

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
    }

    public void onToggleThemeClick(View view) {
        boolean isChecked = ((ToggleButton) view).isChecked();

        if (isChecked) {
            // Dark mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            saveThemeState(true);
        } else {
            // Light mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            saveThemeState(false);
        }

        // Update UI to reflect theme change
        applyTheme();
    }

    private void applyTheme() {
        // Check saved theme preference and apply it
        boolean isDarkMode = isDarkMode();
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        private static final float NS2S = 1.0f / 1000000000.0f; // nanoseconds to seconds

        private float[] lastAcc = new float[3];
        private long lastTime = 0;

        private static final float MAX_SPEED_THRESHOLD = 100; // Maximum allowable speed in m/s, adjust as needed
        private static final long SPEED_RESET_INTERVAL = 10000; // Interval to reset speed calculation in milliseconds, adjust as needed

        private float currentSpeed = 0;
        private long lastSpeedResetTime = 0;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                if (lastTime != 0) {
                    float dt = (event.timestamp - lastTime) * NS2S; // time difference in seconds
                    if(dt < 3f)
                        return;
                    float[] acc = event.values.clone();
                    float[] deltaAcc = new float[3];
                    for (int i = 0; i < 3; ++i) {
                        deltaAcc[i] = acc[i] - lastAcc[i];
                    }

                    float acceleration = (float) Math.sqrt(deltaAcc[0] * deltaAcc[0] + deltaAcc[1] * deltaAcc[1] + deltaAcc[2] * deltaAcc[2]); // magnitude of acceleration

                    // Apply low-pass filter to smooth out acceleration data
                    float alpha = 0.2f; // Adjust alpha as needed (0.0f - 1.0f)
                    acceleration = alpha * acceleration + (1 - alpha) * currentSpeed / dt;

                    currentSpeed += acceleration * dt;

                    // Reset speed if exceeds maximum threshold or periodically
                    if (currentSpeed > MAX_SPEED_THRESHOLD || (System.currentTimeMillis() - lastSpeedResetTime) > SPEED_RESET_INTERVAL) {
                        currentSpeed = 0;
                        lastSpeedResetTime = System.currentTimeMillis();
                    }

                    speedTextView.setText("Speed: " + Math.round(currentSpeed) + getUnitSuffix(currentUnit));
                }

                System.arraycopy(event.values, 0, lastAcc, 0, 3); // Corrected to use event.values instead of acc
                lastTime = event.timestamp;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used in this example
        }
    };



    @Override
    protected void onResume() {
        super.onResume();
        // Register the sensor listener again when the activity resumes
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the sensor listener when the activity pauses
        sensorManager.unregisterListener(sensorEventListener);
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
        // Update the speed display based on the current speed unit
        speedTextView.setText("Speed: " + Math.round(currentSpeed) + getUnitSuffix(currentUnit));
    }

    private boolean isDarkMode() {
        // Retrieve the current theme mode from SharedPreferences
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return preferences.getBoolean("isDarkMode", false); // Default is false (light mode)
    }

    private void saveThemeState(boolean isDarkMode) {
        // Save the current theme mode to SharedPreferences
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isDarkMode", isDarkMode);
        editor.apply();
    }
}
