package com.dom.rustam.devices_java;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OpenFileActivity extends AppCompatActivity {

    private String currentPath = Environment.getExternalStorageDirectory().getPath();
    private List<File> files = new ArrayList<File>();
    private List<File> rootFolder = new ArrayList<File>(); // в списке внутренняя память и флеш карта если есть
    ArrayList<StorageHelper.MountDevice> storages; // хранилища
    private TextView title;
    private ListView listView;
    ImageView backItem;
    private int selectedIndex = -1;
    private int status;


    // Пути
    public static String PATH_DOWNLOADS = Environment.getExternalStorageDirectory().getPath() + "/Devices-downloads"; // загрузки
    public static String PATH_DEFAULT = Environment.getExternalStorageDirectory().getPath();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_file);
        status = getIntent().getIntExtra("status", 0);
        if (status == Constants.STATUS_CHOOSE_FILE) {
            currentPath = PATH_DEFAULT;
            setTitle("Выберите файл");
        }
        if (status == Constants.STATUS_BROWSE) {
            currentPath = PATH_DOWNLOADS;
            setTitle("Загрузки");
        }
        title = findViewById(R.id.directoryText);
        changeTitle(); // прописываем начальный путь
        rootFolder = getStorageDevices(); // список накопителей
        files.addAll(getFiles(currentPath));
        createListView(this); // создаем список файлов и его обработчик
        OpenFileActivity.FileAdapter adapter = new OpenFileActivity.FileAdapter(this, files);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        createBackItem(this); // кнопка вверх

        //TODO в корень добавить внутренню память и SD карту



    }

    // ---------------------------- Основные методы ---------------------

    // Получаем список файлов папки
    private List<File> getFiles(String directoryPath) {
        File directory = new File(directoryPath);
        List<File> fileList = Arrays.asList(directory.listFiles());
        int i = 5;
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File file, File file2) {
                if (file.isDirectory() && file2.isFile())
                    return -1;
                else if (file.isFile() && file2.isDirectory())
                    return 1;
                else
                    return file.getPath().compareTo(file2.getPath());
            }
        });
        return fileList;
    }

    // Обновляем список файлов
    private void RebuildFiles(ArrayAdapter<File> adapter) {
        files.clear();
        files.addAll(getFiles(currentPath));
        adapter.notifyDataSetChanged();
        //title.setText(currentPath);
        changeTitle();
    }

    // Создание списка и его обработчиков
    private ListView createListView(Context context) {
        listView = findViewById(R.id.files_list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
                final ArrayAdapter<File> adapter = (OpenFileActivity.FileAdapter) adapterView.getAdapter();
                File file = adapter.getItem(index);
                if (file.isDirectory()) {
                    currentPath = file.getPath();
                    RebuildFiles(adapter);
                } else {
                    if (status == Constants.STATUS_CHOOSE_FILE) {
                        // возвращаем полное имя выбранного файла
                        Intent intent = new Intent();
                        intent.putExtra("fileName", currentPath + "/" + file.getName());
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }
            }
        });
        return listView;
    }


    // ---------------------------- Вспомогательные методы ---------------------

    // получаем размеры экрана
    private static Display getDefaultDisplay(Context context) {
        return ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    }

    private static Point getScreenSize(Context context) {
        Point screeSize = new Point();
        getDefaultDisplay(context).getSize(screeSize);
        return screeSize;
    }

    private static int getLinearLayoutMinHeight(Context context) {
        return getScreenSize(context).y;
    }

    private int getItemHeight(Context context) {
        TypedValue value = new TypedValue();
        DisplayMetrics metrics = new DisplayMetrics();
        context.getTheme().resolveAttribute(android.R.attr.rowHeight, value, true);
        getDefaultDisplay(context).getMetrics(metrics);
        return (int) TypedValue.complexToDimension(value.data, metrics);
    }

    public int getTextWidth(String text, Paint paint) {
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return bounds.left + bounds.width() + 80;
    }

    // Адаптируем заголовок под размеры экрана
    private void changeTitle() {
        String titleText = currentPath;
        int screenWidth = getScreenSize(this).x;
        int maxWidth = (int) (screenWidth * 0.99);
        if (getTextWidth(titleText, title.getPaint()) > maxWidth) {
            while (getTextWidth("..." + titleText, title.getPaint()) > maxWidth) {
                int start = titleText.indexOf("/", 2);
                if (start > 0)
                    titleText = titleText.substring(start);
                else
                    titleText = titleText.substring(2);
            }
            title.setText("..." + titleText);
        } else {
            title.setText(titleText);
        }
    }

    private void createBackItem(Context context) {
        backItem = findViewById(R.id.backItem);
        Drawable drawable = this.getResources().getDrawable(R.drawable.buttonup);
        drawable.setBounds(0, 0, 24, 24);
        backItem.setImageDrawable(drawable);
        backItem.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                File file = new File(currentPath);
                File parentDirectory = file.getParentFile();
                if (parentDirectory != null) {
                    currentPath = parentDirectory.getPath();
                    RebuildFiles(((OpenFileActivity.FileAdapter) listView.getAdapter()));
                }
            }
        });
    }

    // Возвращает список накопителей
    private List<File> getStorageDevices() {
        List<File> folder = new ArrayList<File>();
        folder.add(new File(PATH_DEFAULT)); // внутренняя память
       storages = StorageHelper.getInstance()
                .getRemovableMountedDevices();
       if (storages.size() != 0) {
           String storagePath = storages.get(0).getPath();
           folder.add(new File(storagePath)); // sd карта
       }
       return folder;
    }


    // -------------------- Прочие классы и интерфейсы ----------------------

    // Адаптер для списка файлов
    private class FileAdapter extends ArrayAdapter<File> {

        public FileAdapter(Context context, List<File> files) {
            super(context, android.R.layout.simple_list_item_1, files);
        }

        // Заполняем данные о файле
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //TextView view = (TextView) super.getView(position, convertView, parent);
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.list_file, null);
            TextView nameText = convertView.findViewById(R.id.fileNameText);
            TextView descriptionText = convertView.findViewById(R.id.fileDescritionText);
            ImageView fileIcon = convertView.findViewById(R.id.fileIcon);
            File file = getItem(position);
            if (file.isDirectory()) {
                nameText.setText(file.getName() + "/");
                nameText.setTextColor(getContext().getResources().getColor(R.color.colorFolder));
                fileIcon.setImageDrawable(getContext().getResources().getDrawable(R.drawable.folder));
                descriptionText.setText(Constants.DESCRIPTION_DEFAULT);
            } else {
                descriptionText.setText(Helper.fileSize(file.length())); // размер файла
                nameText.setText(file.getName());
                nameText.setTextColor(getContext().getResources().getColor(R.color.colorDark));
                fileIcon.setImageDrawable(getExtensionIcon(getFileExtension(file)));
            }
            //descriptionText.setWidth(100);
            return convertView;
        }

        // возврящает иконку файла по его расширению
        private  Drawable getExtensionIcon(String extension) {
            if (extension.equals(Constants.EXTENSION_JPG) || extension.equals(Constants.EXTENSION_PNG) || extension.equals(Constants.EXTENSION_GIF)) {
                return getContext().getResources().getDrawable(R.drawable.image);
            }
            else if (extension.equals(Constants.EXTENSION_DOC) || extension.equals(Constants.EXTENSION_DOCX)) {
                return getContext().getResources().getDrawable(R.drawable.word);
            }
            else if (extension.equals(Constants.EXTENSION_PDF)) {
                return getContext().getResources().getDrawable(R.drawable.pdf);
            }
            else if (extension.equals(Constants.EXTENSION_APK)) {
                return getContext().getResources().getDrawable(R.drawable.apk);
            }
            if (extension.equals(Constants.EXTENSION_MP3) || extension.equals(Constants.EXTENSION_WAV) || extension.equals(Constants.EXTENSION_OGG) || extension.equals(Constants.EXTENSION_WMA)) {
                return getContext().getResources().getDrawable(R.drawable.audio);
            }
            if (extension.equals(Constants.EXTENSION_MP4) || extension.equals(Constants.EXTENSION_MKV) || extension.equals(Constants.EXTENSION_AVI)) {
                return getContext().getResources().getDrawable(R.drawable.video2);
            }
            else return getContext().getResources().getDrawable(R.drawable.document);

        }

        // определяет расширение файла
        private String getFileExtension(File file) {
            String fileName = file.getName();
            // если в имени файла есть точка и она не является первым символом в названии файла
            if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
                // то вырезаем все знаки после последней точки в названии файла, то есть ХХХХХ.txt -> txt
                return fileName.substring(fileName.lastIndexOf(".")+1);
                // в противном случае возвращаем заглушку, то есть расширение не найдено
            else return "";
        }
    }

}
