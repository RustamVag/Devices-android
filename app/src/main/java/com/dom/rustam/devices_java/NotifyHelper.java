package com.dom.rustam.devices_java;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.widget.Toast;

import java.io.IOException;

public class NotifyHelper {

    private SoundPool mSoundPool;
    private AssetManager mAssetManager;
    private  int callSound; // Id звука звонка
    private  int mStreamID;
    private Context context;
    NotificationManager notificationManager;
    private int inQueue = -1;

    public NotifyHelper(Context context) {
        this.context = context;
    }

    // Инициализация
    public void init() {

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // Для устройств до Android 5
            createOldSoundPool();
        } else {
            // Для новых устройств
            createNewSoundPool();
        }

        mAssetManager = context.getAssets();
        callSound = loadSound("call.ogg");
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int i, int i1) {
                if (inQueue == callSound) playSound(callSound);
            }
        });

        notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
    }

    void sendServiceNotif() {
        // 1-я часть
        Notification notif = new Notification(R.drawable.dev, "Служба Devices запущена.",
                System.currentTimeMillis());
        // отправляем
        notificationManager.notify(1, notif);
    }

    // Вызов звонка
    public void call() {
        // inQueue = callSound;
        playSound(callSound);
    }

    // Инициализация SoundPool
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void createNewSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        mSoundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();
    }
    @SuppressWarnings("deprecation")
    private void createOldSoundPool() {
        mSoundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
    }



    // Загружаем звук из файла
    private int loadSound(String fileName) {
        AssetFileDescriptor afd;
        try {
            afd = mAssetManager.openFd(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        return mSoundPool.load(afd, 1);
    }

    // Проигрываем загруженный звук
    private int playSound(int sound) {
        if (sound > 0) {
            mStreamID = mSoundPool.play(sound, 1, 1, 1, 0, 1);
        }
        return mStreamID;
    }
}
