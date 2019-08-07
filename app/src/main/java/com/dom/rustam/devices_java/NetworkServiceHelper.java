package com.dom.rustam.devices_java;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

public class NetworkServiceHelper {
    public DeviceMessage lastMessage;
    public Online online;

    // Конструктор
    public NetworkServiceHelper() { online = new Online(); }

    // Поиск устройства по id
    public Device findDeviceById(int devId) {
        for (Device device : online.devices) {
            if (device.getId() == devId) return device;
        }
        return null;
    }
}

// Сообщение от устройства
class DeviceMessage {
    public Device device;
    public String text;

    public DeviceMessage(Device device, String text) {
        this.device = device;
        this.text = text;
    }
    public DeviceMessage() {}
}


// Список устройств
class Online {
    public ArrayList<Device> devices;

    public  Online() { devices = new ArrayList<Device>();}

    public Online(String xml) {
        devices = new ArrayList<Device>();
        try {
            XmlPullParser parser = prepareXpp(xml);
            while (parser.getEventType()!= XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG
                        && parser.getName().equals("device")) {
                    Device device = new Device();
                    device.setId(Integer.parseInt(parser.getAttributeValue(null, "id")));
                    device.setName(parser.getAttributeValue(null, "name"));
                    device.setColor(Integer.parseInt(parser.getAttributeValue(null, "color")));
                    device.setPosition(Integer.parseInt(parser.getAttributeValue(null, "position")));
                    devices.add(device); // Добавляем запарсенное устройство в список
                }
                parser.next();
            }
        }
        catch (Throwable t) { }
    }


    public String toXML() {
        String xml = XMLBuilder.Header;
        xml += "<online>";
            for (Device device : this.devices) {
                xml += device.toXML();
            }
        xml += "</online>";
        return xml;
    }

    // Создаем парсер
    XmlPullParser prepareXpp(String xml) throws XmlPullParserException {
        // получаем фабрику
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        // включаем поддержку namespace (по умолчанию выключена)
        factory.setNamespaceAware(true);
        // создаем парсер
        XmlPullParser xpp = factory.newPullParser();
        // даем парсеру на вход Reader
        xpp.setInput(new StringReader(xml));
        return xpp;
    }

}


