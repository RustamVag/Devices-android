package com.dom.rustam.devices_java;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorsHelper {
    SensorManager sensorManager;
    Sensor sensorGravity;
    Context context;

    public int IS_LYING = 1; // телефон лежит
    public int IN_HAND = 2; // телефон в руках

    private int currentPosition = 0;

    // Конструктор
    public SensorsHelper(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
    }

    // Акселерометр
    public void initGravitySensor(SensorEventListener listenerGravity) {
        sensorGravity = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(listenerGravity, sensorGravity,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    // Простое грубое определение положения телефона
    public int getPosition(double x, double y, double z) {
        int position;
        if ((z - x - y) > 8.0) {
            position = IS_LYING;
        }
        else {
            position = IN_HAND;
        }

        return position;
    }

}

class GravitySensor {

}
