package com.dom.rustam.devices_java;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;

public class ClientActivity extends AppCompatActivity {

    TextView logoText;
    ServiceConnection sConn;
    NetworkService service;
    NetworkServiceHelper networkServiceHelper;
    BroadcastReceiver br;
    Boolean bound;
    Context context;

    // Константы
    final int TASK_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        context = this;

        logoText = findViewById(R.id.logoText);

        // Создаем приемник сообщений
        br = new BroadcastReceiver() {
            // Принимаем сообщения от сервиса
            @Override
            public void onReceive(Context context, Intent intent) {
                String result = intent.getStringExtra(Constants.PARAM_RESULT);

                // Обрабатываем разные варианты сообщений
                if (result.contains(Constants.BROADCAST_UPDATE_ONLINE)) {
                     // Пришли свежие данные об онлайне
                } else if (result.contains(Constants.RECIVE_FILE)){
                    Toast.makeText(getApplicationContext(), "Принят файл", Toast.LENGTH_SHORT).show();
                }
            }
        };

        // Регаем приемник
        IntentFilter intFilt = new IntentFilter(Constants.BROADCAST_ONLINE);
        registerReceiver(br, intFilt);

        // Настраиваем подключение к сервису
        PendingIntent pi = createPendingResult(TASK_CODE, new Intent(), 0);
        final Intent intent = new Intent(this, NetworkService.class).putExtra(Constants.PARAM_PINTENT, pi);
        sConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder binder) {
                service = ((NetworkService.MyBinder) binder).getService();
                networkServiceHelper = service.getHelper();
                bound = true;
                service.sendToServer(Constants.ONLINE); // Запрашиваем онлайн
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                bound = false;
            }
        };

        // Запускаем сервис
        startService(intent);
    }

    // Принимаем сообщения от сервиса
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Constants.STATUS_CONNECTED) {
            Toast.makeText(getApplicationContext(), "Подключено", Toast.LENGTH_SHORT).show();
        }
        if (resultCode == Constants.STATUS_LOGO) { // Обновляем лого
            String result = data.getStringExtra(Constants.PARAM_RESULT);
            logoText.setText(result);
        }
        if (resultCode == Constants.STATUS_MESSAGE) { // отправка сообщений отключена в коде сервиса
            String result = data.getStringExtra(Constants.PARAM_RESULT);
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
        }
    }


    // -------------------- UI ------------------

    // Клик по логотипу
    public void onClickLogo(View v) {
        if (logoText.getText().equals("Клиент")) { // лучше так не делать
            //sendToServer("Привет от клиента " + settings.getDeviceName());
            //sendToServer(device.toXML());
        }
    }

    // Создание меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.device_menu, menu);
        return true;
    }

    // Выбор пункта меню
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // получим идентификатор выбранного пункта меню
        int id = item.getItemId();
        TextView infoTextView = (TextView) findViewById(R.id.textView);
        switch (id) {
            case R.id.settings:
                Intent intent = new Intent(ClientActivity.this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.devices:
                Intent intent2 = new Intent(ClientActivity.this, OnlineActivity.class);
                // intent2.putExtra("address", this.address);
                startActivity(intent2);
                break;
            case R.id.downloads:
                String path = Environment.getExternalStorageDirectory().toString();
                Intent filesIntent = new Intent(Intent.ACTION_PICK, Uri.parse(path));
                startActivity(filesIntent);
            case R.id.close:
                Intent serviceIntent = new Intent(this, NetworkService.class);
                stopService(serviceIntent);
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    @Override protected void onPause() {
        // кодить здесь
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        // кодить здесь
        super.onDestroy();
        //unregisterReceiver(br);
    }

}
