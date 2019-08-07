package com.dom.rustam.devices_java;


import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

public class Device {
    private String name; // имя игрока
    private String message; // последнее сообщение
    private Integer id; // идентификатор игрока (в данном случае это порт сокета)
    private Integer color; // цвет устройства, отображаемый в интерфейсе пользователя
    private Integer position;

    public Device() {
        this("", 0);
    }

    public Device(String name, int color) {
        this.name = name;
        this.color = color;
        this.id = -1;
        this.position = 0;
    }

    public  Device(String xml) {
        try {
            XmlPullParser parser = prepareXpp(xml);
            while (parser.getEventType()!= XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG
                        && parser.getName().equals("device")) {
                    this.id = Integer.parseInt(parser.getAttributeValue(null, "id"));
                    this.name = parser.getAttributeValue(null, "name");
                    this.color = Integer.parseInt(parser.getAttributeValue(null, "color"));
                    this.position = Integer.parseInt(parser.getAttributeValue(null, "position"));
                }
                parser.next();
            }
        }
        catch (Throwable t) { }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public Integer getPosition() { return position; }

    public void setPosition(Integer position) { this.position = position; }


    // Преобразуем в готовый XML документ
    public String toXMLDocument() {
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        try {
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "device");
            serializer.attribute("", "id", this.id.toString());
            serializer.attribute("", "color", this.color.toString());
            serializer.attribute("", "name", this.name);
            serializer.attribute("", "position", this.position.toString());
            serializer.endTag("", "device");
            serializer.endDocument();
            return writer.toString();
        } catch (Exception e) {
            Log.d("Ошибка: ", e.toString());
        }
       return "null";
    }

    // XML элемент без шапки
    public String toXML() {
        String xml = this.toXMLDocument();
        if ( (!xml.equals("null")) && (!xml.equals("")) ) {
            return xml.substring(56); // Обрезаем ненужную шапку после serializer.startDocument("UTF-8", true); - костыль
        }
        return "null";
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
