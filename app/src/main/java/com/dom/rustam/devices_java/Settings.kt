package com.dom.rustam.devices_java

import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_settrings.*

class Settings (var name:String, var _notify:Boolean, var pref:SharedPreferences){

    // Статические поля
    companion object{

        val APP_PREFERENCES:String = "deviceSettings"
        val APP_PREFERENCES_NAME:String = "deviceName"
        val APP_PREFERENCES_NOTIFY:String = "notify"
        val APP_PREFERENCES_COLOR:String = "color"
        val APP_PREFERENCES_SOUND:String = "sound"
    }

    lateinit var mSettings: SharedPreferences

    var deviceName:String
    var notify:Boolean
    var color:Int
    var sound:Boolean

    init {
        deviceName = name
        notify = _notify
        mSettings = pref
        color = 0
        sound = true
    }

    // Сохраняем настройки
    fun saveSettings() {
        val editor = mSettings.edit()
        editor.putString(APP_PREFERENCES_NAME, deviceName)
        editor.putBoolean(APP_PREFERENCES_NOTIFY, notify)
        editor.putBoolean(APP_PREFERENCES_SOUND, sound)
        editor.putInt(APP_PREFERENCES_COLOR, color)
        editor.apply()
    }

    fun loadSettings() {
        if (mSettings.contains(APP_PREFERENCES_NAME)) {
            deviceName = mSettings.getString(APP_PREFERENCES_NAME, "Имя устройства") // получаем имя девайса
        }
        if (mSettings.contains(APP_PREFERENCES_NOTIFY)) {
            notify = mSettings.getBoolean(APP_PREFERENCES_NOTIFY, false) // уведомления
        }
        if (mSettings.contains(APP_PREFERENCES_SOUND)) {
            sound = mSettings.getBoolean(APP_PREFERENCES_SOUND, false) // звуки
        }
        if (mSettings.contains(APP_PREFERENCES_COLOR)) {
            color = mSettings.getInt(APP_PREFERENCES_COLOR, 0) // получаем цвет
        }
    }


}