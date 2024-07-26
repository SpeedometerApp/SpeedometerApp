package com.example.speeddetectorsensorapp;


public class KalmanFilter {
    private float estimate;
    private float errorEstimate;
    private float q = 0.1f; // Process noise
    private float r = 0.1f; // Measurement noise

    public KalmanFilter() {
        estimate = 0;
        errorEstimate = 1;
    }

    public float update(float measurement) {
        // Prediction
        float predictedEstimate = estimate;
        float predictedErrorEstimate = errorEstimate + q;

        // Update
        float kalmanGain = predictedErrorEstimate / (predictedErrorEstimate + r);
        estimate = predictedEstimate + kalmanGain * (measurement - predictedEstimate);
        errorEstimate = (1 - kalmanGain) * predictedErrorEstimate;

        return estimate;
    }
}
