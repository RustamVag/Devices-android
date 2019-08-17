package com.dom.rustam.devices_java;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

// Файл отправляемый по сети
public class SharedFile {

    private int fileId;
    private String name;
    private byte[] data;
    private ArrayList<FileBlock> blocks; // части файла

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    // Конструктор
    public SharedFile(String fileName) {

        blocks = new ArrayList<FileBlock>();

        String [] path = fileName.split("/");
        this.name = path[path.length-1]; //возвращаем только имя файла без папок
        File file = new File(fileName);
        this.data = new byte[(int) file.length()];

        // Читаем содержимое файла
        try {
            DataInputStream stream = new DataInputStream(new FileInputStream(file));
            stream.readFully(this.data);
            stream.close();
        } catch (Exception e) {
            Log.e("Ошибка: ", e.toString());
        }
    }

    // разбиение на блоки
    public void generateBlocks() {

        int blockSize = Constants.BLOCK_SIZE;
        int id = 1;
        for (int i = 0; i < this.data.length; i+= blockSize) {
            if ((i + blockSize) > data.length) blockSize = data.length -i; // урезаем последний блок концом файла
            byte[] blockData = new byte[blockSize];
            System.arraycopy(this.data, i, blockData,0, blockSize); // копируем часть массива в другой
            blocks.add(new FileBlock(id, blockData));
            id++;
        }

        //TODO Доработать передачу файла частями
    }



}

class FileBlock {
    private int blockId;
    private byte[] data;

    public FileBlock(int id, byte[] blockData) {
        this.blockId = id;
        this.data = blockData;
    }
}
