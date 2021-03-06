package com.app.evolution.engine.app.utils;

import android.support.v4.content.ContextCompat;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.app.evolution.R;

public class FolderUtil {
    public static final String TAG = FolderUtil.class.getSimpleName();
    public static final String VIDEO_MIME = "video/*";
    public static final String DEVELOPER_EMAIL = "onetapvideodownload@gmail.com";
    public static final String MP4_FILE_EXTENSION = ".mp4";
    public static final String UPGRADE = "upgrade";
    
    //List taken from Wikipedia
    public static final String[] VIDEO_FILE_EXTENSION = {"webm", "mkv", "flv", "vob", "ogv", "ogg",
            "drc", "gif", "gifv", "mng", "avi", "mov", "qt", "wmv", "yuv", "rm", "rmvb", "asf", "amv",
            "mp4", "m4p", "m4v", "mpg", "mp2", "mpeg", "mpe", "mpv", "m2v", "svi", "3gp", "sg2", "mxf",
            "roq", "nsv", "flb", "f4v", "f4p", "f4a", "f4b" };

    public static String getDeveloperEmail() {
        return DEVELOPER_EMAIL;
    }

    public static String getFilenameFromUrl(String url) {
        Uri uri = Uri.parse(url);
        return uri.getLastPathSegment();
    }

    public static String getDomain(String url) {
        Uri uri = Uri.parse(url);
        return uri.getHost();
    }

    public static String getNewFilename(String filename) {
        int dotPos = filename.lastIndexOf('.');
        if (dotPos == -1) {
            dotPos = filename.length();
        }

        int openingBracketPos = filename.lastIndexOf('(');
        int closingBracketPos = filename.lastIndexOf(')');
        if (openingBracketPos != -1 && closingBracketPos != -1) {
            String numberString = filename.substring(openingBracketPos + 1, closingBracketPos);
            try {
                Integer number = Integer.parseInt(numberString);
                number = number + 1;
                filename = filename.substring(0, openingBracketPos + 1) + number.toString()
                        + filename.substring(closingBracketPos);
            } catch (Exception e) {
                filename = filename.substring(0, dotPos) + " (1)" + filename.substring(dotPos);
            }
        } else {
            filename = filename.substring(0, dotPos) + " (1)" + filename.substring(dotPos);
        }
        return filename;
    }

    public static String suggestName(String location, String filename) {
        boolean validVideoExtension = false;
        for(String extension : VIDEO_FILE_EXTENSION) {
            if (filename.endsWith(extension)) {
                validVideoExtension = true;
            }
        }

        if (!validVideoExtension) {
            filename += MP4_FILE_EXTENSION;
        }

        File downloadFile = new File(location, filename);
        if (!downloadFile.exists()) {
            return downloadFile.getName();
        }

        filename = getNewFilename(filename);
        return suggestName(location, filename);
    }

    public static boolean isResourceAvailable(String urlString) {
        URL url;
        HttpURLConnection urlConnection;
        try {
            url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            int responseCode = urlConnection.getResponseCode();

            // HttpURLConnection will follow up to five HTTP redirects.
            if (responseCode/100 == 2) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String getResourceMime(String urlString) {
        URL url;
        HttpURLConnection urlConnection;
        try {
            url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            int responseCode = urlConnection.getResponseCode();

            // HttpURLConnection will follow up to five HTTP redirects.
            if (responseCode/100 == 2) {
                return urlConnection.getHeaderField("Content-Type");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getValidatedFilename(String filename) {
        StringBuilder filenameBuilder = new StringBuilder(filename);
        for(int i = 0; i < filename.length(); i++) {
            char j = filename.charAt(i);
            String reservedChars = "?:\"*|/\\<>";
            if(reservedChars.indexOf(j) != -1) {
                filenameBuilder.setCharAt(i, ' ');
            }
        }
        return filenameBuilder.toString().trim();
    }
    
    public static String getRootDirPath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File file = ContextCompat.getExternalFilesDirs(context.getApplicationContext(),
                                                           null)[0];
            return file.getAbsolutePath();
        } else {
            return context.getApplicationContext().getFilesDir().getAbsolutePath();
        }
    }

    public static String getDownloadDir(Context context) {
        String dir = null;
        final String dirName = UPGRADE;
        File root = null;
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            root = context.getExternalFilesDir(null);
        } else {
            root = context.getFilesDir();
        }
        File file = new File(root, dirName);
        file.mkdirs();
        dir = file.getAbsolutePath();
        return dir;
    }
    
    public static long getDirectorySize(File file) {
        long size = 0;
        if (file.isDirectory()) {
            for (File childFile : file.listFiles()) {
                size += getDirectorySize(childFile);
            }
        } else {
            size = file.length();
        }
        return size;
    }
    /**
     * ?????????????????????????????????
     * @param file
     * @param datas
     * @return boolean
     */
    public static boolean generateFile(File file, List<byte[]> datas) {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            for (byte[] data : datas) {
                bos.write(data);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * ??????????????????????????????
     * @param file
     * @param datas
     * @return boolean
     */
    public static boolean appendData(File file, byte[]... datas) {
        RandomAccessFile rfile = null;
        try {
            rfile = new RandomAccessFile(file, "rw");
            rfile.seek(file.length());
            for (byte[] data : datas) {
                rfile.write(data);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rfile != null) {
                try {
                    rfile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * ???????????????
     * @param dir
     */
    public static void createDir(String dir) {
        File f = new File(dir);
        if (!f.exists()) {
            f.mkdirs();
        }

    }

    /**
     * ??????????????????????????????????????????
     * @param path
     * @param content
     * @param append
     */
    public static void writeFile(String path, String content, boolean append) {
        try {
            File f = new File(path);
            if (!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            if (!f.exists()) {
                f.createNewFile();
                f = new File(path); // ???????????????
            }
            FileWriter fw = new FileWriter(f, append);
            if ((content != null) && !"".equals(content)) {
                fw.write(content);
                fw.flush();
            }
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????????????????
     * @param path ????????????
     */
    public static void delFile(String path) {
        try {
            File f = new File(path);
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ???????????????
     * @param folderPath
     */
    public static void delFolder(final String folderPath) {
        try {
            delAllFile(folderPath); // ???????????????????????????
            delFile(folderPath);// ??????????????????
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????path ???????????????
     * @param path
     * @return true????????????     
     */
    public static boolean delAllFile(String path) {
        return delAllFile(path, null);
    }


    /**
     * ??????????????????????????????
     * @param path
     * @param filenameFilter  ????????? ??????null
     * @return boolean
     */
    public static boolean delAllFile(String path, FilenameFilter filenameFilter) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return true;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        File[] tempList = file.listFiles(filenameFilter);
        int length = tempList.length;
        for (int i = 0; i < length; i++) {

            if (tempList[i].isFile()) {
                tempList[i].delete();
            }
            if (tempList[i].isDirectory()) {
                /**
                 * ??????????????????
                 */
                delAllFile(tempList[i].getAbsolutePath(), filenameFilter);
                /**
                 * ??????????????????
                 */
                String[] ifEmptyDir = tempList[i].list();
                if (null == ifEmptyDir || ifEmptyDir.length <= 0) {
                    tempList[i].delete();
                }
                flag = true;
            }
        }
        return flag;
    }


    /**
     * ???????????????
     * @param srcFile ?????????
     * @param destFile ????????????
     * @return boolean
     */
    public static boolean copy(String srcFile, String destFile) {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(srcFile);
            out = new FileOutputStream(destFile);
            byte[] bytes = new byte[1024];
            int c;
            while ((c = in.read(bytes)) != -1) {
                out.write(bytes, 0, c);
            }
            out.flush();
            return true;
        } catch (Exception e) {
            System.out.println("Error!" + e);
            return false;
        }finally{
            if(null != in){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(null != out){
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * ???????????????????????????
     * @param oldPath
     *            String ??????????????? ??????c:/fqf
     * @param newPath
     *            String ??????????????? ??????f:/fqf/ff
     */
    public static void copyFolder(String oldPath, String newPath) {
        (new File(newPath)).mkdirs(); // ???????????????????????? ?????????????????????
        File a = new File(oldPath);
        String[] file = a.list();
        if (null == file)
            return;
        File temp = null;
        for (int i = 0; i < file.length; i++) {
            try {
                if (oldPath.endsWith(File.separator)) {
                    temp = new File(oldPath + file[i]);
                } else {
                    temp = new File(oldPath + File.separator + file[i]);
                }

                if (temp.isFile()) {
                    FileInputStream input = new FileInputStream(temp);
                    FileOutputStream output = new FileOutputStream(newPath + "/" + (temp.getName()).toString());
                    byte[] b = new byte[1024 * 5];
                    int len;
                    while ((len = input.read(b)) != -1) {
                        output.write(b, 0, len);
                    }
                    output.flush();
                    output.close();
                    input.close();
                }
                if (temp.isDirectory()) {// ?????????????????????
                    copyFolder(oldPath + "/" + file[i], newPath + "/" + file[i]);
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    /**
     * ???????????????????????????
     * @param oldPath
     *            String ??????c:/fqf.txt
     * @param newPath
     *            String ??????d:/fqf.txt
     */
    public static void moveFile(String oldPath, String newPath) {
        copy(oldPath, newPath);
        delFile(oldPath);

    }

    /**
     * ???????????????????????????
     * @param resFilePath
     *            ???????????????
     * @param newFilePath
     *            ?????????
     * @return ??????????????????
     */
    public static boolean renameFile(String resFilePath, String newFilePath) {
        File resFile = new File(resFilePath);
        File newFile = new File(newFilePath);
        return resFile.renameTo(newFile);
    }

    /**
     * ??????????????????????????????
     * @param oldPath
     *            String ??????c:/fqf
     * @param newPath
     *            String ??????d:/fqf
     */
    public static void moveFolder(String oldPath, String newPath) {
        copyFolder(oldPath, newPath);
        delFolder(oldPath);
    }

    /**
     * ???????????????????????????
     * @param in
     * @param fileName ????????????
     */
    public static void saveStream2File(InputStream in, String fileName) {
        int size;
        byte[] buffer = new byte[1000];
        BufferedOutputStream bufferedOutputStream = null;
        try {
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(new File(fileName)));
            while ((size = in.read(buffer)) > -1) {
                bufferedOutputStream.write(buffer, 0, size);
            }
            bufferedOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ?????????????????????
     */
    public static FileFilter imagefileFilter = new FileFilter() {
        public boolean accept(File pathname) {
            String tmp = pathname.getName().toLowerCase();
            if (tmp.endsWith(".png") || tmp.endsWith(".jpg") || tmp.endsWith(".bmp") || tmp.endsWith(".gif") || tmp.endsWith(".jpeg")) {
                return true;
            }
            return false;
        }
    };

    /**
     * ??????????????????
     */
    public static FileFilter folderFilter = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };

    /**
     * ?????????????????????
     */
    public static FileFilter mp3fileFilter = new FileFilter() {
        public boolean accept(File pathname) {
            String tmp = pathname.getName().toLowerCase();
            if (tmp.endsWith(".mp3")) {
                return true;
            }
            return false;
        }
    };

    public static File[] getFilesFromDir(String dirPath, FileFilter fileFilter) {
        File dir = new File(dirPath);
        if (dir.isDirectory()) {
            if (fileFilter != null)
                return dir.listFiles(fileFilter);
            else
                return dir.listFiles();
        }
        return null;
    }

    /**
     * ?????????????????????????????????
     * @param dir ??????
     * @param fileFilter ?????????
     * @param hasSuffix ??????????????????
     * @return List<String>
     */
    public static List<String> getExistsFileNames(String dir, FileFilter fileFilter, boolean hasSuffix) {
        String path = dir;
        File file = new File(path);
        File[] files = file.listFiles(fileFilter);
        List<String> fileNameList = new ArrayList<String>();
        if (null != files) {
            for (File tmpFile : files) {
                String tmppath = tmpFile.getAbsolutePath();
                String fileName = getFileName(tmppath, hasSuffix);
                fileNameList.add(fileName);
            }
        }
        return fileNameList;
    }

    /**
     * ????????????????????????????????? (???????????????)
     * @param dir ??????
     * @param hasSuffix ??????????????????
     * @return List<String>
     */
    public static List<String> getAllExistsFileNames(String dir, boolean hasSuffix) {
        String path = dir;
        File file = new File(path);
        File[] files = file.listFiles();
        List<String> fileNameList = new ArrayList<String>();
        if (null != files) {
            for (File tmpFile : files) {
                if (tmpFile.isDirectory()) {
                    fileNameList.addAll(getAllExistsFileNames(tmpFile.getPath(), hasSuffix));
                } else {
                    String tmp = tmpFile.getName().toLowerCase();
                    if (tmp.endsWith(".png") || tmp.endsWith(".jpg") || tmp.endsWith(".bmp") || tmp.endsWith(".gif") || tmp.endsWith(".jpeg")) {
                        fileNameList.add(tmpFile.getAbsolutePath());
                    }
                }
            }
        }
        return fileNameList;
    }

    /**
     * ?????????????????? ?????????
     * @param path
     * @param hasSuffix ??????????????????
     * @return String
     */
    public static String getFileName(String path, boolean hasSuffix) {
        if (null == path || -1 == path.lastIndexOf("/") || -1 == path.lastIndexOf("."))
            return null;
        if (!hasSuffix)
            return path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
        else
            return path.substring(path.lastIndexOf("/") + 1);
    }

    /**
     * ????????????
     * @param path
     * @return String
     */
    public static String getPath(String path) {
        File file = new File(path);

        try {
            if (!file.exists() || !file.isDirectory())
                file.mkdirs();
            return file.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ????????????????????????????????????????????????
     * @param dir ??????
     * @param fileName ????????????
     * @return boolean
     */
    public static boolean isFileExits(String dir, String fileName) {
        fileName = fileName == null ? "" : fileName;
        dir = dir == null ? "" : dir;
        int index = dir.lastIndexOf("/");
        String filePath;
        if (index == dir.length() - 1)
            filePath = dir + fileName;
        else
            filePath = dir + "/" + fileName;
        File file = new File(filePath);
        return file.exists();
    }

    /**
     * ?????????????????????????????????
     * @param filePath ????????????
     * @return boolean
     */
    public static boolean isFileExits(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists())
                return true;
        } catch (Exception e) {

        }
        return false;
    }

    /**
     * ????????????
     * @param dirPath
     * @param fileName
     * @param bmp
     * @return boolean
     */
    public static boolean saveImageFile(String dirPath, String fileName, Bitmap bmp) {
        try {
            File dir = new File(dirPath);

            // ???????????????????????????
            if (!dir.exists()) {
                boolean flag = dir.mkdirs();
                if (flag == false)
                    return false;
            }

            // ???????????????????????????????????????????????????
            if (fileName == null || fileName.trim().length() == 0)
                fileName = System.currentTimeMillis() + ".jpg";
            File picPath = new File(dirPath, fileName);
            FileOutputStream fos = new FileOutputStream(picPath);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ????????????,??????????????????jpg\png
     * @param dirPath
     * @param fileName
     * @param bmp
     * @param format ???????????????
     * @return boolean
     */
    public static boolean saveImageFile(String dirPath, String fileName, Bitmap bmp, Bitmap.CompressFormat format) {
        try {
            File dir = new File(dirPath);

            // ???????????????????????????
            if (!dir.exists()) {
                boolean flag = dir.mkdirs();
                if (flag == false)
                    return false;
            }

            format = format == null ? Bitmap.CompressFormat.JPEG : format;
            // ???????????????????????????????????????????????????
            if (fileName == null || fileName.trim().length() == 0) {
                fileName = System.currentTimeMillis() + "";
                if (format.equals(Bitmap.CompressFormat.PNG))
                    fileName += ".png";
                else
                    fileName += ".jpg";
            }
            File picPath = new File(dirPath, fileName);
            FileOutputStream fos = new FileOutputStream(picPath);
            bmp.compress(format, 100, fos);
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ???????????????????????? B??????
     * @param path
     * @return long
     */
    public static long getFileAllSize(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                if (file.isDirectory()) {
                    File[] children = file.listFiles();
                    long size = 0;
                    for (File f : children)
                        size += getFileAllSize(f.getPath());
                    return size;
                } else {
                    long size = file.length();
                    return size;
                }
            } else {
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * ??????????????????
     * 
     * @param path
     * @return String
     */
    public static String readFileContent(String path) {
        StringBuffer sb = new StringBuffer();
        if (!isFileExits(path)) {
            return sb.toString();
        }
        InputStream ins = null;
        try {
            ins = new FileInputStream(new File(path));
            BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    // ???FileManagerUtil.java????????????
    /**
     * ???????????????
     * @param cxt
     * @param path
     * @param defaultId
     * @return Bitmap
     */
    public static Bitmap getBitmapThumbnail(Context cxt, String path, int defaultId) {
        int photoWidth = cxt.getResources().getDimensionPixelSize(R.dimen.myfile_photo_width);
        int photoHeight = cxt.getResources().getDimensionPixelSize(R.dimen.myfile_photo_height);
        Bitmap bitmap = null;
        try {
            bitmap = getBitmapFromCache(cxt, path, photoWidth, photoHeight, defaultId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * ?????????????????????????????????B KB MB GB TB
     * @param size
     * @return String
     */
    public static String getMemorySizeString(long size) {
        float result = size;
        if (result < 1024) {
            BigDecimal temp = new BigDecimal(result);
            temp = temp.setScale(2, BigDecimal.ROUND_HALF_UP);
            return temp + "Bytes";
        } else {
            result = result / 1024;
            if (result < 1024) {
                BigDecimal temp = new BigDecimal(result);
                temp = temp.setScale(2, BigDecimal.ROUND_HALF_UP);
                return temp + "KB";
            } else {
                result = result / 1024;
                if (result < 1024) {
                    BigDecimal temp = new BigDecimal(result);
                    temp = temp.setScale(2, BigDecimal.ROUND_HALF_UP);
                    return temp + "MB";
                } else {
                    result = result / 1024;
                    if (result < 1024) {
                        BigDecimal temp = new BigDecimal(result);
                        temp = temp.setScale(2, BigDecimal.ROUND_HALF_UP);
                        return temp + "GB";
                    } else {
                        result = result / 1024;
                        BigDecimal temp = new BigDecimal(result);
                        temp = temp.setScale(2, BigDecimal.ROUND_HALF_UP);
                        return temp + "TB";
                    }
                }
            }
        }
    }

    /**
     * ????????????????????????%
     * @param percent
     * @return String
     */
    public static String getMemoryPercentString(float percent) {
        BigDecimal result = new BigDecimal(percent * 100.0f);
        return result.setScale(2, BigDecimal.ROUND_HALF_UP) + "%";
    }

    /**
     * ???????????????
     * @param context
     * @param path
     * @param width
     * @param height
     * @param defaultId
     * @return Bitmap
     */
    public static Bitmap getBitmapFromCache(Context context, String path, int width, int height, int defaultId) {
        Bitmap bitmap = null;
        Bitmap bp = null;
        try {
            InputStream is = new FileInputStream(path);
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                // ????????????????????????????????????????????????bitmap???null
                bp = BitmapFactory.decodeFile(path, options);
                options.inJustDecodeBounds = false; // ?????? false
                // ???????????????
                int h = options.outHeight;
                int w = options.outWidth;
                int beWidth = w / width;
                int beHeight = h / height;
                int be = 1;
                if (beWidth < beHeight) {
                    be = beWidth;
                } else {
                    be = beHeight;
                }
                if (be <= 0) {
                    be = 1;
                }
                options.inSampleSize = be;
                // ???????????????????????????????????????bitmap?????????????????????options.inJustDecodeBounds ?????? false
                bp = BitmapFactory.decodeFile(path, options);
                bitmap = ThumbnailUtils.extractThumbnail(bp, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            } finally {
                try {
                    is.close();
                    is = null;
                    if (bp != null && !bp.isRecycled())
                        bp.recycle();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e1) {
            return null;
        }
        if (bitmap == null || bitmap.isRecycled()) {
            if (defaultId == 0) {
                defaultId = R.mipmap.ic_launcher_round;
            }
            bitmap = BitmapFactory.decodeResource(context.getResources(), defaultId);
        }
        return bitmap;
    }

    /**
     * ???assets?????????????????????????????????
     * @param context
     * @param srcFileName ??????????????????
     * @param targetDir ????????????
     * @param targetFileName ???????????????
     * @return boolean
     */
    public static boolean copyAssetsFile(Context context,String srcFileName,String targetDir,String targetFileName)
    {
        AssetManager asm=null;
        FileOutputStream fos=null;
        DataInputStream dis=null;
        try {
            asm=context.getAssets();
            dis=new DataInputStream(asm.open(srcFileName));
            createDir(targetDir);
            File targetFile=new File(targetDir, targetFileName);
            if(targetFile.exists())
            {
                targetFile.delete();
            }

            fos=new FileOutputStream(targetFile);
            byte[] buffer=new byte[1024];
            int len=0;
            while((len=dis.read(buffer))!=-1)
            {
                fos.write(buffer, 0, len);
            }
            fos.flush();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "copy assets file failed:"+e.toString());
        } finally{
            try {
                if(fos!=null)
                    fos.close();
                if(dis!=null)
                    dis.close();
            } catch (Exception e2) {
            }
        }

        return false;
    }//end copyAssetsFile


    public static boolean downloadFileByURL(String downloadUrl, String localPath) {
        File f = null;
        InputStream is = null;
        OutputStream os = null;
        try {
            f = new File(localPath);
            if (!f.exists()) {
                URL url = new URL(downloadUrl);
                URLConnection con = url.openConnection();
                con.setConnectTimeout(8 * 1000);
                con.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9.2) Gecko/20100115 Firefox/3.6");
                is = con.getInputStream();
                if (con.getContentEncoding() != null
                    && con.getContentEncoding().equalsIgnoreCase("gzip")) {
                    is = new GZIPInputStream(con.getInputStream());
                }
                byte[] bs = new byte[2048];
                int len = -1;
                os = new FileOutputStream(f);
                while ((len = is.read(bs)) != -1) {
                    os.write(bs, 0, len);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (f != null && f.exists()) {
                FolderUtil.delFile(f.getAbsolutePath());
            }
            return false;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
	}
}
