package com.example.speeddetectorsensorapp;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private SpeedCalculator speedCalculator;
    private SensorManager sensorManager;
    private ThemeManager themeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        themeManager = ThemeManager.getInstance(this);
        themeManager.applyTheme();

        setContentView(R.layout.activity_main);

        TextView speedValueTextView = findViewById(R.id.speedValueTextView);
        TextView speedUnitTextView = findViewById(R.id.speedUnitTextView);
        Button kmhButton = findViewById(R.id.kmhButton);
        Button knotsButton = findViewById(R.id.knotsButton);
        Button msButton = findViewById(R.id.msButton);
        Switch themeSwitch = findViewById(R.id.themeSwitch);

        //Builder Design Pattern
        speedCalculator = new SpeedCalculatorBuilder()
                .setSpeedValueTextView(speedValueTextView)
                .setSpeedUnitTextView(speedUnitTextView)
                .setSpeedUnit(new MsSpeedUnit())
                .build();

        setupUnitButtons(kmhButton, knotsButton, msButton);
        setupThemeSwitch(themeSwitch);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(speedCalculator, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    //Strategy Design Pattern
    //Here we are swapping out different SpeedUnit strategies when buttons are clicked.
    private void setupUnitButtons(Button kmhButton, Button knotsButton, Button msButton) {
        kmhButton.setOnClickListener(v -> speedCalculator.setSpeedUnit(new KmhSpeedUnit()));
        knotsButton.setOnClickListener(v -> speedCalculator.setSpeedUnit(new KnotsSpeedUnit()));
        msButton.setOnClickListener(v -> speedCalculator.setSpeedUnit(new MsSpeedUnit()));
    }

    private void setupThemeSwitch(Switch themeSwitch) {
        boolean isDarkMode = themeManager.isDarkMode();
        updateThemeSwitchText(themeSwitch, isDarkMode);
        themeSwitch.setChecked(isDarkMode);
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themeManager.setTheme(isChecked);
            updateThemeSwitchText(themeSwitch, isChecked);
            recreate();
        });
    }

    private void updateThemeSwitchText(Switch themeSwitch, boolean isDarkMode) {
        themeSwitch.setText(isDarkMode ? "Light Mode" : "Dark Mode");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(speedCalculator, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(speedCalculator);
    }
}