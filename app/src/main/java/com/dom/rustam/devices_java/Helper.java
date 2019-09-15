package com.dom.rustam.devices_java;

import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class Helper {

    public static boolean beginAs(String text, String subtext) {
        if (text.split(" ")[0].contains(subtext)) return true; else return false;
    }

    // формирование строки из файла эксперимент
    public static String fileToString(String fileName) {
        String [] path = fileName.split("/");
        String shortFileName = path[path.length-1]; //возвращаем только имя файла без папок
        File file = new File(fileName);
        byte[] data = new byte[(int) file.length()];
        try { // Открываем файл
            DataInputStream stream = new DataInputStream(new FileInputStream(file));
            stream.readFully(data);
            stream.close();
        } catch (Exception e) {
            Log.e("Ошибка: ", e.toString());
        }
        String base64 = Base64.encodeToString(data, Base64.NO_WRAP);
        String stringData = shortFileName + Constants.FILE_NAME_SEPARATOR + base64;

        return stringData; // передаем Base64 содержимого файла
    }

    // запись строки в файл
    public static int stringToFile(String fileName, String data) {
        try { // Пытаемся записать в файл
            File file = new File(fileName);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] binaryData = Base64.decode(data, Base64.NO_WRAP);
            fos.write(binaryData);
            fos.close();
        } catch (Exception e) {
            Log.e("Ошибка: ", e.toString());
            return 0;
        }
        return 1;
    }

    public static String fileSize(long fileLength) {
        if (fileLength < 1024) {
            return String.valueOf(fileLength) + " B";
        }
        else if ((fileLength >=1024) && (fileLength < 1024*1024)) {
            double size = fileLength/1024.0;
            return String.format("%.1f KB", size); // выводим 2 знака после запятой
        }
        else if ((fileLength >=1024*1024) && (fileLength < 1024*1024*1024)) {
            double size = fileLength/(1024.0*1024.0);
            return String.format("%.1f MB", size);
        }
        else if (fileLength >= 1024*1024*1024) {
            double size = fileLength/(1024.0*1024.0*1024.0);
            return String.format("%.1f GB", size);
        }
        else return "?";
    }

}
