package com.dom.rustam.devices_java;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OnlineActivity extends AppCompatActivity implements Theme{

    private static List<Device> devices = new ArrayList<Device>();
    String address;
    Intent serviceIntent;
    ServiceConnection sConn;
    NetworkService service;
    NetworkServiceHelper networkServiceHelper;
    Boolean bound;
    BroadcastReceiver br;
    ArrayAdapter<Device> adapter;
    ListView lv;
    Integer imgId;
    Context context;
    Intent openFileIntent;
    Device selectedDevice;
    boolean darkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online);
        //setTheme();
        context = this;

        openFileIntent = new Intent(this, OpenFileActivity.class);

        // Создаем список устройств и настраиваем его
        lv = findViewById(R.id.onlineList);
        // Долгое нажатие
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                showPopupMenu(view, position); // всплывающее меню
                return true;
            }
        });
        // Обычное нажатие
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id) {
                Device device = devices.get(position);
                Intent intent = new Intent(OnlineActivity.this, DeviceActivity.class)
                        .putExtra(Constants.DEVICE_NAME, device.getName())
                        .putExtra(Constants.DEVICE_COLOR, device.getColor())
                        .putExtra(Constants.DEVICE_POSITION, device.getPosition());
                startActivity(intent);
            }
        });

        imgId = R.drawable.dev; // Id картинки в ресурсах для ListView


        // Принимаем сообщения от сервиса
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String result = intent.getStringExtra(Constants.PARAM_RESULT);

                // Пришли свежие данные об онлайне
                if (result.contains(Constants.BROADCAST_UPDATE_ONLINE)) {
                    devices = networkServiceHelper.online.devices;
                    adapter = new DeviceAdapter(getApplicationContext());
                    adapter.notifyDataSetChanged();
                    lv.setAdapter(adapter);
                }
            }
        };

        // Регаем приемник
        IntentFilter intFilt = new IntentFilter(Constants.BROADCAST_ONLINE);
        registerReceiver(br, intFilt);


        // Настраиваем подключение к службе
        serviceIntent = new Intent(this, NetworkService.class);
        sConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder binder) {
                service = ((NetworkService.MyBinder) binder).getService();
                networkServiceHelper = service.getHelper();
                bound = true;

                // Обновляем данные об онлайне в списке
                devices = networkServiceHelper.online.devices;
                adapter = new DeviceAdapter(getApplicationContext());
                lv.setAdapter(adapter);
            }
            @Override
            public void onServiceDisconnected(ComponentName componentName) { bound = false; }
        };
        bindService(serviceIntent, sConn, BIND_AUTO_CREATE); // Пока что
    }

    @Override protected void onPause() {
        // кодить здесь
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTheme(); // устанавливаем тему здесь
    }

    @Override
    protected void onDestroy() {
        // кодить здесь
        unbindService(sConn);
        unregisterReceiver(br);
        super.onDestroy();
    }

    @Override // Пришло имя выбранного файла
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {return;}
        String fileName = data.getStringExtra("fileName");
        Device device = selectedDevice;
        SharedFile sharedFile = new SharedFile(fileName, device.getId());
        sharedFile.setSendingDevice(service.getDevice().getId()); // задаем id текущего устройства
        sharedFile.generateBlocks();
        //service.sendToServer(Constants.SEND_FILE + " " + device.getId() + " " + Helper.fileToString(fileName));
        service.sendSaredFile(sharedFile);
    }

    // Устанавливаем тему
    @Override
    public void setTheme() {
        SharedPreferences pref = getSharedPreferences(Settings.Companion.getAPP_PREFERENCES(), Context.MODE_PRIVATE);
        Settings settings = new Settings("", false, pref); //Не по красоте
        settings.loadSettings();
        ConstraintLayout layout = findViewById(R.id.onlineLayout);

        if (settings.getDarkTheme() == false) {
            layout.setBackground(getResources().getDrawable(R.color.colorLightGray)); // меняем цвет фона
        }
        else {
            layout.setBackground(getResources().getDrawable(R.color.colorDark));
        }
        this.darkTheme = settings.getDarkTheme();
    }


    // Создаем класс адаптер для ListView
    private class DeviceAdapter extends ArrayAdapter<Device>{

        View currentView;

        public DeviceAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_2, devices);
            setTheme();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext())
                        .inflate(R.layout.list_item, null);
            }
            if (darkTheme) {
                view.setBackground(getResources().getDrawable(R.color.colorDark)); // меняем цвет фона
            }
            else {
                view.setBackground(getResources().getDrawable(R.color.colorLightGray));
            }
            Device device = getItem(position);
            TextView tvName = (TextView) view.findViewById(R.id.list_item_name);
            ImageView dev = (ImageView) view.findViewById(R.id.devImage);
            tvName.setText(device.getName());
            dev.setImageResource(imgId); // Устанавливаем картинку

            return view;
        }

    }


    // ------------ Всплывающее меню для устройства ---------------------------

    private void showPopupMenu(View v, final int position) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.popup_menu); // Для Android 4.0

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Toast.makeText(PopupMenuDemoActivity.this,
                // item.toString(), Toast.LENGTH_LONG).show();
                // return true;
                selectedDevice = devices.get(position);
                switch (item.getItemId()) {

                    case R.id.action_call:
                        final Device device = devices.get(position);
                        service.sendToServer(Constants.DEVICE_CALL + " " + Integer.toString(device.getId()));
                        return true;
                    case R.id.action_file:
                        openFileIntent.putExtra("status", Constants.STATUS_CHOOSE_FILE);
                        startActivityForResult(openFileIntent, 5);
                        return true;
                    case R.id.action_kick:
                        if ((service.status == service.STATUS_SERVER) && (selectedDevice.getId() != service.getDevice().getId())) {
                            service.mServer.kickDevice(devices.get(position)); // отключаем другое выбранное устройство если мы сервер

                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
        popupMenu.show();
    }
}


