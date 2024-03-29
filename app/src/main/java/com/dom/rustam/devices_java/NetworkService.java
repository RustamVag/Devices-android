package com.dom.rustam.devices_java;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.NetworkOnMainThreadException;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class NetworkService extends Service {

    SharedPreferences pref;
    Settings settings;
    Device device;

    NsdHelper nsdHelper;
    NotifyHelper notify;
    NetworkServiceHelper helper;
    SensorsHelper sensorsHelper;
    SocketClient mTcpClient;
    SocketServer mServer;
    String address = "";
    MyBinder binder = new MyBinder();
    PendingIntent pi;
    NotificationManager notificationManager;
    Context context;
    // на сервере
    ArrayList<SharedFile> files;
    int currentFileId = Constants.FIRST_FILE_ID; // 1 - начальный номер файла
    // на клиенте
    SharedFile clientFile; // отправляемый файл на клиенте
    SharedFile recivingFile; // принимаемый файл

    public static int STATUS_OFFLINE = 1;
    public static int STATUS_CLIENT = 2;
    public static int STATUS_SERVER = 3;
    public static int STATUS_STARTING = 4;
    public static int STATUS_RECONNECT = 5;
    int status = STATUS_OFFLINE; // Текущее состояние службы
    int currentPosition = 2; // Начальное текущее проложение девайса в руке

    String CHANNEL_ID = "foreground";


    public NetworkService() {
        files = new ArrayList<SharedFile>(); // список файлов
    }

    // Возвращаем хелпер
    public NetworkServiceHelper getHelper() {
        return helper;
    }

    public Device getDevice() { return this.device; }


    // Связываение сервиса с активити
    @Override
    public IBinder onBind(Intent intent) {
        int flag = intent.getIntExtra(Constants.FLAG, 0);

        if (flag == Constants.FLAG_ONLINE) {
            pi = intent.getParcelableExtra(Constants.PARAM_PINTENT); // Нестабильный код
        }

        return binder;
    }

    public void onRebind(Intent intent) {
        super.onRebind(intent);

        int flag = intent.getIntExtra(Constants.FLAG, 0);

        if (flag == Constants.FLAG_ONLINE) {
            pi = intent.getParcelableExtra(Constants.PARAM_PINTENT); // Нестабильный код
        }
    }

    public boolean onUnbind(Intent intent) {
        // кодить здесь
        return true;
    }

    class MyBinder extends Binder {
        NetworkService getService() {
            return NetworkService.this;
        }
    }

    // Создание службы
    @Override
    public void onCreate() {
        super.onCreate();
        // Помещаем службу на передний план
        Intent notificationIntent = new Intent(this, ClientActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        context = getApplicationContext();
        // Создаем уведомление для переднего плана
        notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        // Для Android 8.0 и выше создаем канал
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Сеть", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Служба Devices запущена.");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(false);
            notificationManager.createNotificationChannel(channel);
            Notification notification = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.dev)
                    .setContentTitle("Устройства")
                    .setContentText("Подключено к сети")
                    .setChannelId(CHANNEL_ID)
                    .setContentIntent(pendingIntent)
                    .build();
            startForeground(1, notification);
        }
        else {
            Notification notification = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.dev).setContentTitle("Устройства").setContentText("Подключено к сети").setContentIntent(pendingIntent).build();
            startForeground(1, notification);
        }
    }

    // Запуск службы
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) pi = intent.getParcelableExtra(Constants.PARAM_PINTENT);
        int st = status;
        connectToService(); // Возобновляем работу сервиса или создаем его заново
        return super.onStartCommand(intent, flags, startId);
    }

    // Остановка службы
    @Override
    public void onDestroy() {
        super.onDestroy();
        close();
        // Не вызывается когда надо
    }

    // Останавливаем службу
    public void close() {
        stopForeground(true);
        if (mServer != null) {
            mServer.close();
            mServer = null;
        }
        if (mTcpClient != null) {
            mTcpClient.stopClient();
            mTcpClient = null;
        }
        nsdHelper.tearDown();
        status = STATUS_OFFLINE;
        helper.online.devices.clear(); // Ощищаем список подключенных устройств
    }

    public void updateLogo(String uiMessage) {
        try {
            Intent intent = new Intent().putExtra(Constants.PARAM_RESULT, uiMessage);
            if (pi != null) pi.send(NetworkService.this, Constants.STATUS_LOGO, intent);

        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    private void sendToActivity(String uiMessage) {
        try {
            Intent intent = new Intent().putExtra(Constants.PARAM_RESULT, uiMessage);
            pi.send(NetworkService.this, Constants.STATUS_MESSAGE, intent);

        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    // Сообщение в Activity с помощью Broadcast Reciver
    private void sendBroadcast(String uiMessage) {
        Intent intent = new Intent(Constants.BROADCAST_ONLINE);
        try {
            intent.putExtra(Constants.PARAM_RESULT, uiMessage);
            sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Действие сервиса после ее возобновления работы и при начале работы
    public void connectToService() {
        if (status == STATUS_OFFLINE) { // Запускаем сервис если он не запущен
            // Загружаем настройки
            pref = getSharedPreferences(Settings.Companion.getAPP_PREFERENCES(), Context.MODE_PRIVATE);
            settings = new Settings("", false, pref); //Не по красоте
            settings.loadSettings();
            device = new Device(settings.getDeviceName(), settings.getColor()); // Создаем объект устройства

            // Локальная сеть. Если адресс неизвестен то включаем NSD, иначе подключаемся к серверу
            if (address == "") {
                nsdHelper = new NsdHelper(getApplicationContext(), this);
                nsdHelper.initializeNsd();
                nsdHelper.registerService(nsdHelper.servicePort); // Пытаемся зарегестировать сервер
            } else {
                sendToServer(Constants.PING);
                status = STATUS_CLIENT;
                updateLogo("Клиент");
            }

            // Создаем хелперы
            helper = new NetworkServiceHelper();
            notify = new NotifyHelper(getApplicationContext());
            notify.init();
            initSensors();
            status = STATUS_STARTING;
        }
        if (status == STATUS_STARTING) {

        }
        if (status == STATUS_SERVER) {
            sendToActivity("Режим сервера");
            updateLogo("Сервер");
        }
        if (status == STATUS_CLIENT) {
            sendToActivity("Режим клиента");
            updateLogo("Клиент");
        }
    }

    // Изменяется режим с сервера на клиента
    public void onChangeConnectionMode() {
        close();
        status = STATUS_OFFLINE;
        Toast.makeText(this, "Переподключение", Toast.LENGTH_SHORT).show();
        connectToService(); // тест
    }


    // --------------------------------- Клиент ---------------------------

    // Подключиться к серверу и обработать события клиента
    private void connectToServer() {
        if (address != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mTcpClient = new SocketClient(address, new SocketClient.OnMessageReceived() {
                        // Клиент подключился к серверу
                        @Override
                        public void onConnected() {
                            if (device != null) {
                                sendToServer(Constants.DEVICE_CONNECTED + " " + device.toXMLDocument()); // Команда подключения устройства
                            } // Отправляем данные об устройстве серверу

                            // Сообщаем активити о подключении
                            try {
                                pi.send(Constants.STATUS_CONNECTED);

                            } catch (PendingIntent.CanceledException e) {
                                e.printStackTrace();
                            }

                            // Пингуем раз в 10 сек
                            final Timer pingTimer = new Timer(); // Создаем таймер
                            pingTimer.schedule(new TimerTask() { // Определяем задачу
                                @Override
                                public void run() {
                                    if (status == STATUS_OFFLINE) {
                                        pingTimer.cancel();
                                    } else if (status == STATUS_RECONNECT) {
                                        pingTimer.cancel();
                                        status = STATUS_CLIENT;
                                    } else {
                                        sendToServer(Constants.PING);
                                        if (device.getId() != -1) {
                                            sendToServer(Constants.DEVICE_UPDATED + " " + device.toXML()); // Обновляем данные об устройстве
                                        }
                                    }
                                }
                            }, 0L, 10L * 1000);
                        }

                        @Override
                        public void messageReceivedFromServer(final String message) {
                            // sendToActivity(message);
                            parseResponse(message); // Обрабатываем ответ
                        }
                    });
                    mTcpClient.run("name");
                }
            }).start();
        }
    }

    // Отправить сообщение на сервер в отдельном потоке
    public void sendToServer(final String msg) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if ((mTcpClient != null)) {
                    if (mTcpClient.isRunning()) {
                        String g = address;
                        mTcpClient.sendMessage(msg);
                    } else {
                        connectToServer();
                    }
                } else {
                    connectToServer();
                }
            }
        });
        thread.start();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if ((mTcpClient != null)) {
                    if (mTcpClient.isRunning()) {
                        String g = address;
                        mTcpClient.sendMessage(msg);
                    } else {
                        connectToServer();
                    }
                } else {
                    connectToServer();
                }
            }
        };
        //runnable.run();
    }

    public void sendSaredFile(SharedFile file) {
        clientFile = file;
        sendToServer(Constants.SEND_FILE_INFO + " " + file.toXMLDocument());
    }

    // отправка части файла
    public void sendFileBlock(FileBlock block, Device device) {

    }

    // Обрабатываем ответ от сервера
    private void parseResponse(String message) {
        // Обрабатываем ответы сервера
        int i = 5;
        if (beginAs(message, Constants.ONLINE)) { // Запрос онлайна
            message = message.substring(Constants.ONLINE.length() + 1); // Убираем -online из строки сообщения
            Online online = new Online(message);
            int oldOnline = helper.online.devices.size(); // записываем старый онлайн
            helper.online = online;
            if (oldOnline != 0) { // если мы не только что подключились к сети
                if (oldOnline < helper.online.devices.size())
                    notify.newDevice(); // звук нового устройства в сети
                if (oldOnline > helper.online.devices.size())
                    notify.deviceLeave(); // другое устройство вышло из сети
            }
            sendBroadcast(Constants.BROADCAST_UPDATE_ONLINE);
        } else if (beginAs(message, Constants.DEVICE_CALL)) {
            notify.call();
        }
        else if (beginAs(message, Constants.DEVICE_ID)) {
            message = message.substring(Constants.DEVICE_ID.length() + 1);
            device.setId(Integer.parseInt(message));
        }
        else if (beginAs(message, Constants.RECIVE_FILE)) {
            message = message.substring(Constants.RECIVE_FILE.length() + 1);
            String shortFileName = message.split("\\" + Constants.FILE_NAME_SEPARATOR)[0]; // извлекаем имя файла
            message = message.substring(shortFileName.length() + 1);
            String fileName = Environment.getExternalStorageDirectory().getPath() + Constants.PATH_DOWNLOADS + shortFileName; // получаем полное имя файла
            if (Helper.stringToFile(fileName, message) == 1) { // сохранаем в файл пришедший ответ
                sendBroadcast(Constants.RECIVE_FILE);
            }
        }
        else if (beginAs(message, Constants.FILE_ID)) { // Id файла получен
            message = message.substring(Constants.FILE_ID.length() + 1);
            clientFile.setFileId(Integer.parseInt(message)); // получаем id отправляемого файла
            onReciveFileId();
        }
        else if (beginAs(message, Constants.SEND_FILE_INFO)) { // Id файла получен
            message = message.substring(Constants.SEND_FILE_INFO.length() + 1);
            recivingFile = new SharedFile();
            recivingFile.parseXML(message);
            int j = 5;
        }
        else if (beginAs(message, Constants.SEND_FILE_BLOCK)) { // Новый блок отправителю
            String data = message.substring(Constants.SEND_FILE_BLOCK.length() +1); // Убираем тэг
            FileBlock recivedBlock = new FileBlock();
            recivedBlock.parseString(data);
            if (recivingFile != null) {
                recivingFile.blocks.add(recivedBlock);
                if (recivingFile.blocks.size() < recivingFile.blocksCount) {  // если не все получили запрашиваем новый блок
                    sendToServer(Constants.GET_NEW_BLOCK + " " + recivingFile.getFileId());
                }
                else { // если все блоки полкчены
                    recivingFile.save(Environment.getExternalStorageDirectory().getPath() + Constants.PATH_DOWNLOADS); // сохраняем файл в указанной папке
                    sendBroadcast(Constants.RECIVE_FILE); // сообщение о принятии файла
                    recivingFile = null; // освобождаем память эксперимент
                }
            }
        }
        else if (beginAs(message, Constants.GET_NEW_BLOCK)) {
            String data = message.substring(Constants.GET_NEW_BLOCK.length() +1);
            int fileId = Integer.parseInt(data);
            if (clientFile != null) {
                FileBlock sendingBlock = clientFile.readBlock();
                sendToServer(Constants.SEND_FILE_BLOCK + " " + sendingBlock.toString()); // отправляем следующий блок
                Integer progress = Math.round((100*sendingBlock.getBlockId()/clientFile.getBlocksCount()));
                sendBroadcast(Constants.BROADCAST_MESSAGE + " Отправка: " + progress.toString() + "%");
                if (sendingBlock.getBlockId() == clientFile.blocksCount) clientFile = null; // освобождаем панять эксперимент
            }

        }
        else if (beginAs(message, Constants.CLOSED_CONNECTION)) {
            stopSelf(); // вырубаем службу если сервер закрыл соединение
        }

    }

    // Id файла получен
    private void onReciveFileId() {
        FileBlock sendingBlock = clientFile.readBlock();
        sendToServer(Constants.SEND_FILE_BLOCK + " " + sendingBlock.toString()); // отправляем первый блок
    }


    //   ------------------------ Сервер ---------------------------------

    // Инициализация сервера и обработка его событий
    public void startServer() {
        if (mServer == null) {
            mServer = new SocketServer(new SocketServer.OnMessageReceived() {
                // Пришло сообщение от клиента
                @Override
                public void messageReceived(String message, Device from) {
                    parseMessage(message, from);
                }

                @Override
                public void updateDeviceList(final ArrayList connectedDevices) {
                    helper.online.devices.clear();
                    for (Object dm : connectedDevices) {
                        Device dev = ((DeviceManager) dm).getDevice();
                        helper.online.devices.add(dev);
                    }

                    // Отправляем каждому клиенту данные об онлайне
                    for (Device device : helper.online.devices) {
                        mServer.sendMessageTo(device.getId(), Constants.ONLINE + " " + helper.online.toXML());
                    }
                }
            });
            mServer.start();
        }
        status = STATUS_SERVER;
    }

    // Прием сообщения от клиента
    private void parseMessage(final String message, final Device device) {
        helper.lastMessage = new DeviceMessage(device, message);

        // Обрабатываем команды
        if (beginAs(message, Constants.ONLINE)) { // Запрос онлайна
            mServer.sendMessageTo(device.getId(), Constants.ONLINE + " " + helper.online.toXML()); // Отправляем онлайн сервера клиенту
        } else if (beginAs(message, Constants.DEVICE_CALL)) {
            Integer devId = Integer.parseInt(message.split(" ")[1]); // Извлекаем второе слово из сообщения, это id устройства
            mServer.sendMessageTo(devId, Constants.DEVICE_CALL);
        }
        else if (beginAs(message, Constants.DEVICE_UPDATED)) {
            String xmlMessage = message.substring(Constants.DEVICE_UPDATED.length() +1); // Убираем тэг DEVICE_UPDATED
            Device newDevice = new Device(xmlMessage);
            mServer.userUpdated(newDevice);
        }
        else if (beginAs(message, Constants.SEND_FILE)) { // Обмен файлом
            String data = message.substring(Constants.SEND_FILE.length() +1); // Убираем тэг
            Integer devId = Integer.parseInt(data.substring(0, 10).split(" ")[0]); // получаем первое слово - id устройства  эксперимент
            data = data.substring(devId.toString().length() +1);
            mServer.sendMessageTo(devId, Constants.RECIVE_FILE + " " + data);
        }
        else if (beginAs(message, Constants.SEND_FILE_INFO)) { // Новый файл
            String data = message.substring(Constants.SEND_FILE_INFO.length() +1); // Убираем тэг
            // регаем новый файл их xml
            SharedFile sharedFile = new SharedFile();
            sharedFile.parseXML(data);
            sharedFile.setFileId(getCurrentFileId()); // присваиваем id файлу, осторожно - инкремент
            files.add(sharedFile);
            mServer.sendMessageTo(device.getId(), Constants.FILE_ID + " " + sharedFile.getFileId()); // Отправляем id клиенту
            mServer.sendMessageTo(sharedFile.getTargetDevice(), Constants.SEND_FILE_INFO + " " + sharedFile.toXMLDocument()); // отправляем данные о файле получателю

        }
        else if (beginAs(message, Constants.SEND_FILE_BLOCK)) { // Новый файл
            String data = message.substring(Constants.SEND_FILE_BLOCK.length() +1); // Убираем тэг
            // формируем блок из xml
            FileBlock recivedBlock = new FileBlock();
            recivedBlock.parseString(data);
            SharedFile file = findFileById(recivedBlock.getIdFile());
            mServer.sendMessageTo(file.getTargetDevice(), Constants.SEND_FILE_BLOCK + " " + recivedBlock.toString());
        }
        else if (beginAs(message, Constants.GET_NEW_BLOCK)) {
            String data = message.substring(Constants.GET_NEW_BLOCK.length() +1);
            int fileId = Integer.parseInt(data);
            SharedFile file = findFileById(fileId);
            mServer.sendMessageTo(file.getSendingDevice(), message);
        }


    }

    // Строка начинается со слова..
    public boolean beginAs(String text, String subtext) {
        if (text.split(" ")[0].contains(subtext)) return true;
        else return false;
    }


    // ------------------------ Сенсоры ------------------------------

    private void initSensors() {
        sensorsHelper = new SensorsHelper(getApplicationContext());

        // Слушаем данные от акселерометра
        sensorsHelper.initGravitySensor(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                int position = sensorsHelper.getPosition(event.values[0], event.values[1], event.values[2]);

                if (position != currentPosition) {
                    if (currentPosition != 0) {
                        onChangeDevicePosition(position);
                    }
                    currentPosition = position;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        });

    }

    // Изменилось положение устройства
    private void onChangeDevicePosition(int position) {
        device.setPosition(position);
    }


    // ---------------------------------- Файлы --------------------------

    private int getCurrentFileId() {
        return currentFileId++;
    }


    private SharedFile findFileById(int fileId) {
        for (SharedFile file : this.files) {
            if (file.getFileId() == fileId) return file;
        }
        return null;
    }

}



