package com.dom.rustam.devices_java

import android.content.Context
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.view.View
import android.widget.*
import com.skydoves.colorpickerpreference.ColorEnvelope
import com.skydoves.colorpickerpreference.ColorListener
import com.skydoves.colorpickerpreference.ColorPickerView
import kotlinx.android.synthetic.main.activity_settrings.*

class SettingsActivity : AppCompatActivity() {

    lateinit var settings:Settings // Класс для работы с настройками
    lateinit var pref:SharedPreferences
    lateinit var colorPicker: ColorPickerView
    lateinit var layout:ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settrings)
        val deviceNameEdit = findViewById<EditText>(R.id.deviceNameEdit)
        val notifySwitch = findViewById<Switch>(R.id.notifySwitch)
        val colorTextView = findViewById<TextView>(R.id.colorTextView)
        val darkThemeSwitch = findViewById<Switch>(R.id.darkTheme)
        layout = findViewById<ConstraintLayout>(R.id.settingsLayout)
        pref = getSharedPreferences(Settings.APP_PREFERENCES, MODE_PRIVATE)
        settings = Settings(deviceNameEdit.text.toString(), notifySwitch.isChecked, pref)
        colorPicker = findViewById<ColorPickerView>(R.id.colorPickerView)
        colorPicker.preferenceName = "ColorPickerView"

        // Выбор цвета
        colorPicker.setColorListener(object:ColorListener {
            override fun onColorSelected(colorEnvelope: ColorEnvelope) {
                settings.color = colorEnvelope.color
                colorTextView.setTextColor(settings.color)
            }
        })

        // Меняем тему
        darkThemeSwitch.setOnCheckedChangeListener(object:CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView:CompoundButton, isChecked:Boolean) {
                if (isChecked == true) {
                    layout.setBackground(resources.getDrawable(R.color.colorDark))
                }
                else {
                    layout.setBackground(resources.getDrawable(R.color.colorLightGray))
                }
            }
        })
    }

    private fun setTheme() {
        if (settings.darkTheme) {
            layout.setBackground(resources.getDrawable(R.color.colorDark))
        }
        else {
            layout.setBackground(resources.getDrawable(R.color.colorLightGray))
        }
    }

    override fun onPause() {
        super.onPause()
        settings.deviceName = deviceNameEdit.text.toString()
        settings.notify = notifySwitch.isChecked
        settings.sound = soundSwitch.isChecked
        settings.darkTheme = darkTheme.isChecked
        settings.saveSettings()
    }

    override fun onResume() {
        super.onResume()
        settings.loadSettings()
        deviceNameEdit.setText(settings.deviceName)
        notifySwitch.isChecked = settings.notify
        soundSwitch.isChecked = settings.sound
        darkTheme.isChecked = settings.darkTheme
        colorTextView.setTextColor(settings.color)
        setTheme()
    }

    override fun onDestroy() {
        super.onDestroy()
        colorPicker.saveData()
    }

}
