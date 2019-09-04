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
import java.io.FileOutputStream;
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
    private Integer size = 0;
    private Integer sendingDevice;
    private Integer targetDevice;
    public ArrayList<FileBlock> blocks; // части файла

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
        // обновляем id файла в каждом блоке
        for (FileBlock fileBlock: this.blocks) {
            fileBlock.setIdFile(fileId);
        }
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

    public Integer getTargetDevice() {
        return this.targetDevice;
    }

    public Integer getSendingDevice() {
        return sendingDevice;
    }

    public void setSendingDevice(Integer sendingDevice) {
        this.sendingDevice = sendingDevice;
    }

    // Конструкторы
    public SharedFile(String fileName, int toDevice) { // имя файла и  id устройства назанчения

        blocks = new ArrayList<FileBlock>();
        this.targetDevice = toDevice;
        this.fileId = -1; // не задан

        String [] path = fileName.split("/");
        this.name = path[path.length-1]; //возвращаем только имя файла без папок
        File file = new File(fileName);
        this.data = new byte[(int) file.length()];

        // Читаем содержимое файла
        try {
            DataInputStream stream = new DataInputStream(new FileInputStream(file));
            stream.readFully(this.data);
            stream.close();
            this.size = data.length; // узнаем размер файла
        } catch (Exception e) {
            Log.e("Ошибка: ", e.toString());
        }
    }

    public SharedFile(String fileName) {
        this(fileName, 0);
    }

    public SharedFile() {
        blocks = new ArrayList<FileBlock>();
    }


    // разбиение на блоки
    public void generateBlocks() {

        int blockSize = Constants.BLOCK_SIZE;
        int id = 1;
        for (int i = 0; i < this.data.length; i+= blockSize) {
            if ((i + blockSize) > data.length) blockSize = data.length -i; // урезаем последний блок концом файла
            byte[] blockData = new byte[blockSize];
            System.arraycopy(this.data, i, blockData,0, blockSize); // копируем часть массива в другой
            blocks.add(new FileBlock(id, this.fileId, blockData));
            id++; //TODO найти ошибку
        }
        this.blocksCount = this.blocks.size();
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
        return this.blocksCount;
    }


    public String toXMLDocument() {
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        try {
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "file");
            serializer.attribute("", "fileId", this.fileId.toString());
            serializer.attribute("", "size", this.size.toString());
            serializer.attribute("", "sendingDevice", this.sendingDevice.toString());
            serializer.attribute("", "targetDevice", this.targetDevice.toString());
            serializer.attribute("", "name", this.name);
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
                    this.size = Integer.parseInt(parser.getAttributeValue(null, "size"));
                    this.sendingDevice = Integer.parseInt(parser.getAttributeValue(null, "sendingDevice"));
                    this.targetDevice = Integer.parseInt(parser.getAttributeValue(null, "targetDevice"));
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

    // соединяет два массива байт
    private byte[] concatArrays(byte[] A, byte[] B) {
        int aLen = A.length;
        int bLen = B.length;
        byte[] C= new byte[aLen+bLen];
        System.arraycopy(A, 0, C, 0, aLen);
        System.arraycopy(B, 0, C, aLen, bLen);
        return C;
    }


    // Сохраняем полученный файл
    public void save(String directory) {
        if (this.data == null) { // объединяем блоки
            this.data = new byte[this.size];
            int cursor = 0;
            for(FileBlock block : this.blocks) {
                int blockSize = block.getData().length;
                System.arraycopy(block.getData(), 0, this.data, cursor, blockSize);
                cursor += blockSize;
            }
        }
        try { // Пытаемся записать файл
            File file = new File(directory + this.name); // полное имя файла
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(this.data);
            fos.close();
        } catch (Exception e) {
            Log.e("Ошибка: ", e.toString());
        }

    }
}


// Часть содержимого файла
class FileBlock {
    private Integer blockId;
    private Integer idFile;
    private byte[] data;

    public int getBlockId() {
        return blockId;
    }

    public void setBlockId(int blockId) {
        this.blockId = blockId;
    }

    public int getIdFile() {
        return idFile;
    }

    public void setIdFile(int idFile) {
        this.idFile = idFile;
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

    public FileBlock(int id, int idFile, byte[] blockData) {
        this.blockId = id;
        this.data = blockData;
        this.idFile = idFile;
    }

    public  FileBlock() {}

    public String toBase64() {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }


   /* public String toXMLDocument() {
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        try {
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "block");
            serializer.attribute("", "idFile", this.idFile.toString());
            serializer.attribute("", "blockId", this.blockId.toString());
            serializer.attribute("", "base64", this.toBase64());
            serializer.endTag("", "block");
            serializer.endDocument();
            return writer.toString();
        } catch (Exception e) {
            Log.d("Ошибка: ", e.toString());
        }
        return "null";
    } */

   /* public void parseXML(String xml) {
        try {
            XmlPullParser parser = XMLBuilder.prepareXpp(xml);
            while (parser.getEventType()!= XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG
                        && parser.getName().equals("block")) {
                    this.idFile = Integer.parseInt(parser.getAttributeValue(null, "idFile"));
                    this.data = Base64.decode( parser.getAttributeValue(null, "base64"), Base64.NO_WRAP);
                    this.blockId = Integer.parseInt(parser.getAttributeValue(null, "blockId"));
                }
                parser.next();
            }
        }
        catch (Throwable t) { }
    } */

    // Делаем в виде простой строки
    public String toString() {
        return this.idFile.toString() + " " + this.blockId + " " + this.toBase64();
    }

    // Из строки в блок
    public void parseString(String stringBlock) {
        String[] splitString = stringBlock.split(" ");
        this.idFile = Integer.parseInt(splitString[0]);
        this.blockId = Integer.parseInt(splitString[1]);
        this.data = Base64.decode(splitString[2], Base64.NO_WRAP);
    }

}
