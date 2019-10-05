package com.dom.rustam.devices_java;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


public class DeviceFragment extends Fragment {

    private Device device;
    TextView deviceTitle;
    TextView devicePosition;
    ImageView callImage;
    ImageView sendFileImage;
    ConstraintLayout deviceLayout;

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
       View rootView = inflater.inflate(R.layout.fragment_device, container, false);

        //final Device device = ((OnlineActivity)getActivity()).getSelectedDevice();

        deviceTitle = rootView.findViewById(R.id.deviceTitleText);
        devicePosition = rootView.findViewById(R.id.positionText);
        deviceLayout = rootView.findViewById(R.id.deviceLayout);
        callImage = rootView.findViewById(R.id.callImage);
        sendFileImage = rootView.findViewById(R.id.sendFileImage);

        buildView(); // заполняем данными об устройства

        return rootView;
    }

    // Строим весь вид фрагмента
    public void buildView() {

        device = ((OnlineActivity)getActivity()).getSelectedDevice();
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


        // Настраиваем события для кнопок
        callImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((OnlineActivity)getActivity()).callAction(device);
            }
        });

        sendFileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((OnlineActivity)getActivity()).sendFileAction();
            }
        });
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
