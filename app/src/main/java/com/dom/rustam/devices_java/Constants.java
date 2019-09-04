package com.dom.rustam.devices_java;

public class Constants {
    // Общение между клиентом и сервером
    public static int SERVER_PORT = 7777;
    public static String LOGIN_NAME = "Dev";
    public static String CLOSED_CONNECTION = "-close";
    public  static String SERVER_CLOSED_THE_CONNECTION = "-closed_connection";
    public static String PING = "-ping";
    public static String XML = "<?xml"; // Начало xml файла
    public static String DEVICE_CONNECTED = "-connect";
    public static String ONLINE = "-online";
    public  static  String DEVICE_CALL = "-call";
    public  static  String CLIENT = "-client";
    public static String DEVICE_UPDATED = "-deviceUpdated";
    public static String DEVICE_ID = "-deviceId";

    // Обмен файлами
    public static String SEND_FILE = "-sendFile";
    public static String RECIVE_FILE = "-reciveFile";
    public static String FILE_ID = "-fileId";
    public static String SEND_FILE_INFO = "-sendInfoFile";
    public static String SEND_FILE_BLOCK = "-sendBlockFile";
    public static String GET_NEW_BLOCK = "-getNewBlock";

    // Общение между сервисом и активностями
    public final static String PARAM_PINTENT = "pendingIntent";
    public final static String PARAM_RESULT = "result";
    public final static int STATUS_CONNECTED = 100;
    public final static int STATUS_LOGO = 200;
    public  final static int STATUS_MESSAGE = 300;

    // Флаки для Pending Intent
    public  final static String FLAG = "flag";
    public  final static int FLAG_ONLINE = 1;
    public  final static int FLAG_CLIENT = 2;

    // Broadcast Reciver
    public static String BROADCAST_ONLINE = "com.dom.rustam.devices_java:broadcast_online";
    public  static  String BROADCAST_UPDATE_ONLINE = "-updateOnline";
    public static  String BROADCAST_CALL = "-call";
    public static String BROADCAST_MESSAGE = "-brodcastMessage";

    // Прочтие константы
    public static String DEVICE_POSITION = "devicePosition";
    public  static String DEVICE_NAME = "deviceName";
    public  static String DEVICE_COLOR = "deviceColor";

    public static int POSITION_IS_LYING = 1; // телефон лежит
    public static int POSITION_IN_HAND = 2; // телефон в руках

    public static String PATH_DOWNLOADS = "/Devices-downloads/";
    public static String DEFAULT_FILE_NAME = "devices-file.txt";
    public static String FILE_NAME_SEPARATOR = "|";

    public static String LOGO_SERVER = "Сервер";
    public static String LOGO_CLIENT = "Клиент";

    public static int BLOCK_SIZE = 65536*8; // 512кб

    //public static int BLOCK_SIZE = 128; // тест

    public static int FIRST_FILE_ID = 1; // Номер первого отправляемого файла
}
