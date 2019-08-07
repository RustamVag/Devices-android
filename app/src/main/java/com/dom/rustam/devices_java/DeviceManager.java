package com.dom.rustam.devices_java;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class DeviceManager extends Thread {
    private Device device; // экземпляр класса Device, хранящий информацию о пользователе
    private Socket socket; // сокет, созданный при подключении пользователя
    private int ping = 0;
    private PrintWriter bufferSender;
    private boolean running; // флаг для проверки, запущен ли сокет
    private UserManagerDelegate managerDelegate; // экземпляр интерфейса UserManagerDelegate

    public DeviceManager(Socket socket, UserManagerDelegate managerDelegate) {
        this.device = new Device();
        this.socket = socket;
        this.managerDelegate = managerDelegate;
        running = true;
    }


    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) { this.device = device; }

    public Socket getSocket() {
        return socket;
    }

    @Override public void run() {
        super.run();
        try {
            // отправляем сообщение клиенту
            bufferSender =
                    new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
                            true);

            // читаем сообщение от клиента
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // в бесконечном цикле ждём сообщения от клиента и смотрим, что там
            while (running) {
                String message = null;
                message = in.readLine();

                // проверка на команды
                if (hasCommand(message)) {
                    continue;
                }

                if (message != null && managerDelegate != null) {
                    device.setMessage(message); // сохраняем сообщение
                    managerDelegate.messageReceived(device, null); // уведомляем сервер о сообщении
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        running = false;

        if (bufferSender != null) {
            bufferSender.flush();
            bufferSender.close();
            bufferSender = null;
        }

        try {
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        socket = null;
    }

    public void sendMessage(String message) {
        if (bufferSender != null && !bufferSender.checkError()) {
            bufferSender.println(message);
            bufferSender.flush();
        }
    }

    // Обработка команд од клиента
    public boolean hasCommand(String message) {
        if (message != null) {
            if (message.contains(Constants.CLOSED_CONNECTION)) {
                close();
                managerDelegate.userDisconnected(this, device.getName());
                return true;
            } else if (message.split(" ")[0].contains(Constants.DEVICE_CONNECTED)) { // Если сообщение начинается со слова -connect
                message = message.substring(Constants.DEVICE_CONNECTED.length()+1); // Убираем -connect из строки сообщения
                device = new Device(message); // Создаем объект по XML
                device.setId(socket.getPort());
                managerDelegate.userConnected(device); // Устройство подключено
                initPingTimer();
                return true;
            } else if (message.contains(Constants.PING)) {
                ping = 0;
                return true;
            }
        }
        return false;
    }

    // Запускаем таймер проверки пинга
    public void initPingTimer() {
        final Timer myTimer = new Timer(); // Создаем таймер
        myTimer.schedule(new TimerTask() { // Определяем задачу
            @Override
            public void run() { // Останавливаем таймер и кикаем устройство если прошло больше 30 сек
                if (ping > 30) {
                    managerDelegate.userDisconnected(DeviceManager.this, device.getName());
                    close();
                    ping = 0;
                    myTimer.cancel();
                }
                else if (running == false) {
                    myTimer.cancel(); // Если устройство не в сети выключаем таймер
                }
                else ping++;
            }
        }, 0L, 1000);
    }

    // интерфейс, который передает результаты операций в SocketServer
    public interface UserManagerDelegate {
        void userConnected(Device connectedDevice);

        void userDisconnected(DeviceManager userManager, String username);

        void messageReceived(Device fromDevice, Device toDevice);
    }
}