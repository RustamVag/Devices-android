package com.dom.rustam.devices_java;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.IBinder;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class DeviceActivity extends AppCompatActivity {

    Device device;
    TextView deviceTitle;
    TextView devicePosition;
    ConstraintLayout deviceLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        Intent intent = getIntent();

        deviceTitle = findViewById(R.id.deviceTitleText);
        deviceLayout = findViewById(R.id.deviceLayout);
        devicePosition = findViewById(R.id.positionText);

        // Читаем данные, полученные из предыдущего активити
        device = new Device();
        device.setName(intent.getStringExtra(Constants.DEVICE_NAME));
        device.setColor(intent.getIntExtra(Constants.DEVICE_COLOR, -1));
        device.setPosition(intent.getIntExtra(Constants.DEVICE_POSITION, 0));

        // Устанавливаем фон с градиентом
        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {device.getColor(), lighten(device.getColor(), 0.25)});
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        deviceLayout.setBackground(gradientDrawable);

        // Меняем цвет текста для темного фона
        if (colorIsDark(device.getColor())) {
            deviceTitle.setTextColor(getResources().getColor(R.color.colorWhite));
            devicePosition.setTextColor(getResources().getColor(R.color.colorWhite));
        }
        deviceTitle.setText(device.getName());
        if (device.getPosition() == Constants.POSITION_IS_LYING) devicePosition.setText("Лежит");
        if (device.getPosition() == Constants.POSITION_IN_HAND) devicePosition.setText("В руке");
    }

    // Определяет темный ли цвет
    public boolean colorIsDark(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        if (1 - (0.299 * red + 0.587 * green + 0.114 * blue) / 255 < 0.5) return false; else return true;
    }

    // Осветлить цвет
    public int lighten(int color, double intensity) { // цвет и интенсивность 0..1
        double red = (double) Color.red(color);
        double green = (double) Color.green(color);
        double blue = (double) Color.blue(color);
        double value = 255*intensity;
        red += value; blue += value; green += value;
        if (red > 255) red = 255; if (green > 255) green = 255; if (blue > 255) blue = 255;
        int redInt = (int) Math.round(red);
        int greenInt = (int) Math.round(green);
        int blueInt = (int) Math.round(blue);
        return getIntFromColor(redInt, greenInt, blueInt);
    }

    public int lighten(int color) {
        return lighten(color, 0.3);
    }

    // Преобразует цвет из rgb в int
    public int getIntFromColor(int Red, int Green, int Blue) {
        Red = (Red << 16) & 0x00FF0000;
        Green = (Green << 8) & 0x0000FF00;
        Blue = Blue & 0x000000FF;
        return 0xFF000000 | Red | Green | Blue;
    }
}
