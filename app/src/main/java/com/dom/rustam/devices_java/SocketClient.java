package com.dom.rustam.devices_java;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class SocketClient {
    private String mServerMessage;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false; // флаг, определяющий, запущен ли сервер
    private PrintWriter mBufferOut;
    private BufferedReader mBufferIn;
    private Socket socket;
    private String address;

    public SocketClient(String address, OnMessageReceived listener) {
        this.address = address;
        mMessageListener = listener;
    }

    public void sendMessage(String message) {
        if (mBufferOut != null && !mBufferOut.checkError()) {
            mBufferOut.println(message);
            mBufferOut.flush();
        }
    }

    public void stopClient() {
        sendMessage(Constants.CLOSED_CONNECTION);

        mRun = false;

        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }

        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
    }

    public void run(String player) {
        try {
            InetAddress serverAddr = InetAddress.getByName(address);
            Log.d("Server address: ", serverAddr.getHostAddress());

            try {
                socket = new Socket(serverAddr, Constants.SERVER_PORT);
                mRun = true;
                mBufferOut =
                        new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),
                                true);

                mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // sendMessage(Constants.LOGIN_NAME + player);
                mMessageListener.onConnected();

                // ждем ответа
                while (mRun) {
                    if (mBufferOut.checkError()) {
                        mRun = false;
                    }

                    mServerMessage = mBufferIn.readLine();


                    if (mServerMessage != null && mMessageListener != null && Helper.beginAs(mServerMessage, Constants.CLIENT)) {
                        mServerMessage = mServerMessage.substring(Constants.CLIENT.length() + 1); // Вырезаем начало строки
                        mMessageListener.messageReceivedFromServer(mServerMessage);
                    }
                }
            } catch (Exception e) {
                Log.d("Ошибка: ", e.toString());
            } finally {
                if (socket != null && socket.isConnected()) {
                    socket.close();
                }
            }
        } catch (Exception e) {
            Log.d("Ошибка: ", e.toString());
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    public boolean isRunning() {
        return mRun;
    }

    public interface OnMessageReceived {
        void messageReceivedFromServer(String message);

        void onConnected();
    }
}
