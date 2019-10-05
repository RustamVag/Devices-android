package com.dom.rustam.devices_java;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

// Статья: https://habr.com/ru/post/254813/

public class StorageHelper {
    private static StorageHelper sStorage;
    private MountDeviceGetter mGetter;

    private StorageHelper() {
        mGetter = this.new MountDeviceGetter();
        mGetter.fillDevicesEnvirement();
    }

    public ArrayList<MountDevice> getAllMountedDevices() {
        ArrayList<MountDevice> mountedDevice = new ArrayList<StorageHelper.MountDevice>(
                mGetter.getMountedExternalDevices());
        mountedDevice.addAll(mGetter.getMountedRemovableDevices());

        return mountedDevice;
    }

    public ArrayList<MountDevice> getExternalMountedDevices() {
        return mGetter.getMountedExternalDevices();
    }

    public ArrayList<MountDevice> getRemovableMountedDevices() {
        return mGetter.getMountedRemovableDevices();
    }

    public static StorageHelper getInstance() {
        if (sStorage == null) {
            sStorage = new StorageHelper();
        }
        return sStorage;
    }

    public enum MountDeviceType {
        EXTERNAL_SD_CARD, REMOVABLE_SD_CARD
    }

    public class MountDevice {
        private MountDeviceType mType;
        private String mPath;
        private Integer mHash;

        /**
         * @return the type
         */
        public final MountDeviceType getType() {
            return mType;
        }

        /**
         * @return the path
         */
        public final String getPath() {
            return mPath;
        }

        /**
         * @return the hash
         */
        public final Integer getHash() {
            return mHash;
        }

        public MountDevice(MountDeviceType type, String path, Integer hash) {
            super();
            mType = type;
            mPath = path;
            mHash = hash;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object o) {
            if (!mPath.equals(((MountDevice) o).getPath())) {
                return mHash.equals(((MountDevice) o).getHash());
            }

            return true;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return mHash;
        }

    }

    private class MountDeviceGetter {
        private ArrayList<MountDevice> mMountedExternalDevices = null;
        private ArrayList<MountDevice> mMountedRemovableDevices = null;

        private int calcHash(File dir) {
            StringBuilder tmpHash = new StringBuilder();

            tmpHash.append(dir.getTotalSpace());
            tmpHash.append(dir.getUsableSpace());

            File[] list = dir.listFiles();
            for (File file : list) {
                tmpHash.append(file.getName());
                if (file.isFile()) {
                    tmpHash.append(file.length());
                }
            }

            return tmpHash.toString().hashCode();

        }

        private void testAndAdd(String path, MountDeviceType type) {
            File root = new File(path);
            if (root.exists() && root.isDirectory() && root.canWrite()) {

                MountDevice device = new MountDevice(type, path, calcHash(root));

                switch (type) {
                    case EXTERNAL_SD_CARD:
                        if (!mMountedExternalDevices.contains(device)) {
                            mMountedExternalDevices.add(device);
                        }
                        break;
                    case REMOVABLE_SD_CARD:
                        if (!mMountedRemovableDevices.contains(device)) {
                            mMountedRemovableDevices.add(device);
                        }
                        break;
                }

            }
            root = null;
        }

        /**
         * @return the mountedExternalDevices
         */
        public ArrayList<MountDevice> getMountedExternalDevices() {
            return mMountedExternalDevices;
        }

        /**
         * @return the mountedDevices
         */
        public ArrayList<MountDevice> getMountedRemovableDevices() {
            return mMountedRemovableDevices;
        }

        public void fillDevicesEnvirement() {
            mMountedExternalDevices = new ArrayList<StorageHelper.MountDevice>(3);
            mMountedRemovableDevices = new ArrayList<StorageHelper.MountDevice>(3);
            // получить экстернал
            String path = android.os.Environment.getExternalStorageDirectory()
                    .getAbsolutePath();
            if (!path.trim().isEmpty()
                    && android.os.Environment.getExternalStorageState().equals(
                    android.os.Environment.MEDIA_MOUNTED)) {
                testAndAdd(path, MountDeviceType.EXTERNAL_SD_CARD);
            }

            // Получаем ремувабл
            String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
            if (rawSecondaryStoragesStr != null
                    && !rawSecondaryStoragesStr.isEmpty()) {
                // All Secondary SD-CARDs splited into array
                final String[] rawSecondaryStorages = rawSecondaryStoragesStr
                        .split(File.pathSeparator);
                for (String rawSecondaryStorage : rawSecondaryStorages) {
                    testAndAdd(rawSecondaryStorage,
                            MountDeviceType.REMOVABLE_SD_CARD);
                }
            }
        }

        public void fillDevicesProcess() {
            mMountedExternalDevices = new ArrayList<StorageHelper.MountDevice>(3);
            mMountedRemovableDevices = new ArrayList<StorageHelper.MountDevice>(3);
            InputStream is = null;
            InputStreamReader isr = null;
            BufferedReader br = null;
            Process proc = null;
            String line;
            try {
                Runtime runtime = Runtime.getRuntime();
                proc = runtime.exec("mount");
                try {
                    is = proc.getInputStream();
                    isr = new InputStreamReader(is);
                    br = new BufferedReader(isr);
                    while ((line = br.readLine()) != null) {
                        if (line.contains("secure"))
                            continue;
                        if (line.contains("asec"))
                            continue;

                        if (line.contains("fat")) {// TF card
                            String columns[] = line.split(" ");
                            if (columns != null && columns.length > 1) {
                                testAndAdd(columns[1],
                                        MountDeviceType.REMOVABLE_SD_CARD);
                            }
                        } else if (line.contains("fuse")) {// internal(External)
                            // storage
                            String columns[] = line.split(" ");
                            if (columns != null && columns.length > 1) {
                                // mount = mount.concat(columns[1] + "\n");
                                testAndAdd(columns[1],
                                        MountDeviceType.EXTERNAL_SD_CARD);
                            }
                        }
                    }
                } finally {
                    if (br != null) {
                        br.close();
                    }
                    if (isr != null) {
                        isr.close();
                    }
                    if (proc != null) {
                        proc.destroy();
                    }
                    if (is != null) {
                        is.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}
