package com.dom.rustam.devices_java

import android.content.Context
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import com.skydoves.colorpickerpreference.ColorEnvelope
import com.skydoves.colorpickerpreference.ColorListener
import com.skydoves.colorpickerpreference.ColorPickerView
import kotlinx.android.synthetic.main.activity_settrings.*

class SettingsActivity : AppCompatActivity() {

    lateinit var settings:Settings // Класс для работы с настройками
    lateinit var pref:SharedPreferences
    lateinit var colorPicker:ColorPickerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settrings)
        val deviceNameEdit = findViewById<EditText>(R.id.deviceNameEdit)
        val notifySwitch = findViewById<Switch>(R.id.notifySwitch)
        val colorTextView = findViewById<TextView>(R.id.colorTextView)
        pref = getSharedPreferences(Settings.APP_PREFERENCES, MODE_PRIVATE)
        settings = Settings(deviceNameEdit.text.toString(), notifySwitch.isChecked, pref)
        colorPicker = findViewById<ColorPickerView>(R.id.colorPickerView)
        colorPicker.preferenceName = "ColorPickerView"

        // Выбор цвета
        colorPickerView.setColorListener(object:ColorListener {
            override fun onColorSelected(colorEnvelope: ColorEnvelope) {
                settings.color = colorEnvelope.color
                colorTextView.setTextColor(settings.color)
            }
        })
    }

    override fun onPause() {
        super.onPause()
        settings.deviceName = deviceNameEdit.text.toString()
        settings.notify = notifySwitch.isChecked
        settings.sound = soundSwitch.isChecked
        settings.saveSettings()
    }

    override fun onResume() {
        super.onResume()
        settings.loadSettings()
        deviceNameEdit.setText(settings.deviceName)
        notifySwitch.isChecked = settings.notify
        soundSwitch.isChecked = settings.sound
        colorTextView.setTextColor(settings.color)
    }

    override fun onDestroy() {
        super.onDestroy()
        colorPicker.saveData()
    }

}
