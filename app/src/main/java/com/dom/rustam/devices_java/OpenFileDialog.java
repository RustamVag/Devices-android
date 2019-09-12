package com.dom.rustam.devices_java;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Environment;
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

// Класс - диалог для открытия файла
// материал: https://habr.com/ru/post/203884/
public class OpenFileDialog extends AlertDialog.Builder {

    private String currentPath = Environment.getExternalStorageDirectory().getPath();
    private List<File> files = new ArrayList<File>();
    private TextView title;
    private ListView listView;
    private int selectedIndex = -1;
    private OpenDialogListener listener;

    // Пути
    public static String PATH_DOWNLOADS = Environment.getExternalStorageDirectory().getPath() + "/Devices-downloads"; // загрузки
    public static String PATH_DEFAULT = Environment.getExternalStorageDirectory().getPath();

    // Конструктор
    public OpenFileDialog(Context context, String path) {
        super(context);
        if (path.length() > 0) currentPath = path; // задаем папку если она указана
        title = createTitle(context);
        changeTitle();
        LinearLayout linearLayout = createMainLayout(context);
        linearLayout.addView(createBackItem(context));
        files.addAll(getFiles(currentPath));
        listView = createListView(context);
        listView.setAdapter(new FileAdapter(context, files));
        linearLayout.addView(listView);
        setCustomTitle(title)
                .setView(linearLayout)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null);
    }


    // ---------------------------- Основные методы ---------------------

    // Получаем список файлов папки
    private List<File> getFiles(String directoryPath) {
        File directory = new File(directoryPath);
        List<File> fileList = Arrays.asList(directory.listFiles());
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
        ListView listView = new ListView(context);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
                final ArrayAdapter<File> adapter = (FileAdapter) adapterView.getAdapter();
                File file = adapter.getItem(index);
                if (file.isDirectory()) {
                    currentPath = file.getPath();
                    RebuildFiles(adapter);
                } else {
                    listener.OnSelectedFile(currentPath + "/" + file.getName()); // возвращаем полное имя выбранного файла

                }
            }
        });
        return listView;
    }

    // Настраиваем разметку диалога
    private LinearLayout createMainLayout(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setMinimumHeight(getLinearLayoutMinHeight(context));
        return linearLayout;
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

    // создаем кастомный текст заголовока
    private TextView createTitle(Context context) {
        TextView textView = createTextView(context, android.R.style.TextAppearance_DeviceDefault_DialogWindowTitle);
        return textView;
    }

    public int getTextWidth(String text, Paint paint) {
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return bounds.left + bounds.width() + 80;
    }

    private void changeTitle() {
        String titleText = currentPath;
        int screenWidth = getScreenSize(getContext()).x;
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

    private TextView createBackItem(Context context) {
        TextView textView = createTextView(context, android.R.style.TextAppearance_DeviceDefault_Small);
        Drawable drawable = getContext().getResources().getDrawable(android.R.drawable.ic_menu_directions);
        drawable.setBounds(0, 0, 60, 60);
        textView.setCompoundDrawables(drawable, null, null, null);
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        textView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                File file = new File(currentPath);
                File parentDirectory = file.getParentFile();
                if (parentDirectory != null) {
                    currentPath = parentDirectory.getPath();
                    RebuildFiles(((FileAdapter) listView.getAdapter()));
                }
            }
        });
        return textView;
    }

    private TextView createTextView(Context context, int style) {
        TextView textView = new TextView(context);
        textView.setTextAppearance(context, style);
        int itemHeight = getItemHeight(context);
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeight));
        textView.setMinHeight(itemHeight);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setPadding(15, 0, 0, 0);
        return textView;
    }

    // регистрируем слушаетля
    public OpenFileDialog setOpenDialogListener(OpenDialogListener listener) {
        this.listener = listener;
        return this;
    }


    // -------------------- Прочие классы и интерфейсы ----------------------

    // Адаптер для списка файлов
    private class FileAdapter extends ArrayAdapter<File> {

        public FileAdapter(Context context, List<File> files) {
            super(context, android.R.layout.simple_list_item_1, files);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //TextView view = (TextView) super.getView(position, convertView, parent);
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.list_file, null);
            TextView nameText = convertView.findViewById(R.id.fileNameText);
            TextView descriptionText = convertView.findViewById(R.id.fileDescritionText);
            ImageView fileIcon = convertView.findViewById(R.id.fileIcon);
            File file = getItem(position);
            descriptionText.setText(Helper.fileSize(file.length())); // размер файла
            if (file.isDirectory()) {
                nameText.setText(file.getName() + "/");
                nameText.setTextColor(getContext().getResources().getColor(R.color.colorFolder));
                fileIcon.setImageDrawable(getContext().getResources().getDrawable(R.drawable.folder));
            } else {
                nameText.setText(file.getName());
                nameText.setTextColor(getContext().getResources().getColor(R.color.colorDark));
                fileIcon.setImageDrawable(getExtensionIcon(getFileExtension(file)));
            }

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

    // Слушатель выбора файла
    public interface OpenDialogListener {
        public void OnSelectedFile(String fileName);
    }
}

