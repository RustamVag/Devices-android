package com.dom.rustam.devices_java;

import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

// Файл отправляемый по сети
public class SharedFile {

    private Integer fileId;
    private String name;
    private byte[] data;
    private ArrayList<FileBlock> blocks; // части файла

    public Integer blocksCount;

    public int status;
    public static int STATUS_CREATED = 0;
    public static int STATUS_SENDING = 1;
    public static int STATUS_RECIVING = 2;

    public Integer currentBlockId = 1;

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
    }

    public FileBlock readBlock() {
        if (blocks != null) {
            this.status = STATUS_SENDING; // меняем статус на отправку файла
            return getBlockById(this.currentBlockId++); // Возвращаем текущий блок и переводим курсор на следующий
        }
        return null;
    }


    public FileBlock getBlockById(int blockId) {
        for (FileBlock block : this.blocks) {
            if (block.getBlockId() == blockId) {
                return block;
            }
        }
        return null;
    }

    public Integer getBlocksCount() {
        return this.blocks.size();
    }


    public String toXMLDocument() {
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        try {
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "file");
            serializer.attribute("", "fileId", this.fileId.toString());
            serializer.attribute("", "name", this.name);
            //serializer.attribute("", "blockId", this.currentBlockId.toString());
            //serializer.attribute("", "currentBlock", this.getBlockById(currentBlockId).toBase64());
            serializer.attribute("", "blocksCount", this.getBlocksCount().toString());
            serializer.endTag("", "file");
            serializer.endDocument();
            return writer.toString();
        } catch (Exception e) {
            Log.d("Ошибка: ", e.toString());
        }
        return "null";
    }

    public void parseXML(String xml) {
        try {
            XmlPullParser parser = XMLBuilder.prepareXpp(xml);
            while (parser.getEventType()!= XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG
                        && parser.getName().equals("file")) {
                    this.fileId = Integer.parseInt(parser.getAttributeValue(null, "fileId"));
                    this.name = parser.getAttributeValue(null, "name");
                    this.blocksCount = Integer.parseInt(parser.getAttributeValue(null, "blocksCount"));
                    //this.currentBlockId = Integer.parseInt(parser.getAttributeValue(null, "blockId"));
                    //this.addBlock(parser.getAttributeValue(null, "currentBlock"));
                }
                parser.next();
            }
        }
        catch (Throwable t) { }
    }

}

class FileBlock {
    private int blockId;
    private byte[] data;

    public int getBlockId() {
        return blockId;
    }

    public void setBlockId(int blockId) {
        this.blockId = blockId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public FileBlock(int id, byte[] blockData) {
        this.blockId = id;
        this.data = blockData;
    }
    public String toBase64() {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }
}
