package com.dom.rustam.devices_java;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class SocketServer extends Thread implements DeviceManager.UserManagerDelegate {
    private boolean running = false; // флаг, определяющий, запущен ли сервер
    private ServerSocket serverSocket;
    private ArrayList<DeviceManager> connectedDevices; // список подключенных игроков

    public interface OnMessageReceived {
        void messageReceived(String message, Device from); // отправка сообщения в UI-поток

        void updateDeviceList(ArrayList<DeviceManager> connectedDevices); // обновление списка при подключении\отключении
    }
    private OnMessageReceived messageListener;

    public SocketServer(OnMessageReceived messageListener) {
        this.messageListener = messageListener;
        connectedDevices = new ArrayList<>();
    }

    private void runServer() {
        running = true;

        try {
            serverSocket = new ServerSocket(Constants.SERVER_PORT);

            while (running) {
                Socket client = serverSocket.accept();

                DeviceManager userManager = new DeviceManager(client, this);
                connectedDevices.add(userManager);
                userManager.start();
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    @Override public void run() {
        super.run();
        runServer();
    }

    // Методы сервера
    public void close() {
        if (connectedDevices != null) { // закрытие всех соединений с клиентами
            for (DeviceManager userManager : connectedDevices) {
                userManager.close();
            }
        }

        running = false;
        // закрытие сервера
        try {
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        serverSocket = null;
        connectedDevices = null; // эксперимент
    }

    // Отправить сообщение клиенту
    public void sendMessage(Device device) {
        if (connectedDevices != null) {
            for (DeviceManager userManager : connectedDevices) {
                if (userManager.getDevice().getId() != device.getId()) {
                    userManager.sendMessage(device.getMessage()); // если идентификатор пользователя не равен идентификатору отправившего сообщение - отправляем ответ
                }
            }
        }
    }

    public void sendMessageTo(int id, String msg) {
        if (connectedDevices != null) {
            for (DeviceManager deviceManager : connectedDevices) {
                if (deviceManager.getDevice().getId() == id) {
                    msg = Constants.CLIENT + " " + msg; // добавляем тег для клиента
                    deviceManager.sendMessage(msg); // если идентификатор пользователя равен заданному - отправляем ответ
                }
            }
        }
    }

    public void sendToAll(String msg) {
        if (connectedDevices != null) {
            for (DeviceManager userManager : connectedDevices) {
                userManager.sendMessage(msg); // ищем всех пользователей в списке и отправляем ответ
            }
        }
    }

    // Возвращаем устройствол по id
    public Device findDeviceById(int devId) {
        for (DeviceManager deviceManager : connectedDevices) {
            if (deviceManager.getDevice().getId() == devId) return deviceManager.getDevice();
        }
        return null;
    }

    // Возвращаем менеджер устройства по id устройства
    public DeviceManager findDeviceManagerById(int devId) {
        for (DeviceManager deviceManager : connectedDevices) {
            if (deviceManager.getDevice().getId() == devId) return deviceManager;
        }
        return null;
    }


    // Возвращаем индекс устройства по id
    public int findDeviceIndexById(int devId) {
        for (DeviceManager deviceManager : connectedDevices) {
            if (deviceManager.getDevice().getId() == devId) return connectedDevices.indexOf(deviceManager);
        }
        return -1;
    }


    // Обновление данных об устройстве
    public void userUpdated(Device updatedDevice) {
        DeviceManager deviceManager = findDeviceManagerById(updatedDevice.getId());
        deviceManager.setDevice(updatedDevice);
        messageListener.updateDeviceList(connectedDevices);
    }

    @Override public void userConnected(Device connectedDevice) {
        messageListener.updateDeviceList(connectedDevices);
        sendMessageTo(connectedDevice.getId(), Constants.DEVICE_ID + " " + String.valueOf(connectedDevice.getId()));
    }

    @Override public void userDisconnected(DeviceManager userManager, String username) {
        sendMessageTo(userManager.getDevice().getId(), Constants.SERVER_CLOSED_THE_CONNECTION);
        connectedDevices.remove(userManager);
        messageListener.updateDeviceList(connectedDevices);
    }

    @Override public void messageReceived(Device fromDevice, Device toDevice) {
        messageListener.messageReceived(fromDevice.getMessage(), fromDevice);
        sendMessage(fromDevice);
    }
}

