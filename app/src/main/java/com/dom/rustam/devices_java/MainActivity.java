package com.dom.rustam.devices_java;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.dom.rustam.devices_java.R;

public class MainActivity extends AppCompatActivity implements Theme{

    SensorManager sensorManager;
    List<Sensor> sensors;
    SharedPreferences pref;
    Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // создаем папку загрузок если ее нет
        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + "Devices-downloads");
        if (!folder.exists()) {
            folder.mkdir();
        }
        pref = getSharedPreferences(Settings.Companion.getAPP_PREFERENCES(), Context.MODE_PRIVATE);
        settings = new Settings("", false, pref); //Не по красоте
        settings.loadSettings();
        // Если нет имени устройства переходим в настройки
        if (settings.getDeviceName() == "") {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTheme();
    }

    // Кнопка Подключиться
    public void onClickConnect(View v) {
        Intent intent = new Intent(MainActivity.this, ClientActivity.class);
        startActivity(intent);
    }

    public void onClickSettings(View v) {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public void setTheme() {
        SharedPreferences pref = getSharedPreferences(Settings.Companion.getAPP_PREFERENCES(), Context.MODE_PRIVATE);
        Settings settings = new Settings("", false, pref); //Не по красоте
        settings.loadSettings();
        ConstraintLayout layout = findViewById(R.id.mainLayout);

        if (settings.getDarkTheme() == false) {
            layout.setBackground(getResources().getDrawable(R.color.colorLightGray)); // меняем цвет фона
        }
        else {
            layout.setBackground(getResources().getDrawable(R.color.colorDark));
        }
    }

}