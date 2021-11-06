package com.app.evolution.engine.app.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.util.Log;

import com.app.evolution.R;
import com.app.evolution.engine.app.config.BaseConfig;

public class OpenRootUtil {

    private static final String TAG = "OpenRootUtil";
    /** 配置文件 */
    private static final String PREFS_NAME = "SafeCenter";
    /** 上次授权超时的时间 */
    private static final String PREFS_KEY_LastOpenOutTime = "LastOpenOutTime";
    /** 申请授权间隔时间为3天 */
    private static final long RE_OPEN_ROOT_TIME = 3 * 24 * 60 * 60 * 1000;
    /**
     * root工具集合的jar包
     */
    private static final String COMMAND_FILE_NAME = "nd.jar";
    /**
     * shell文件大小,如有重新编译panda_super_shell,需要修改文件大小
     */
    private static final int SHELL_FILE_SIZE = 6744;

    /**
     * 是否拥有root权限
     * 
     * @return boolean
     */
    public static boolean hasRootPermission() {
        boolean rooted = true;
        try {
            File su = new File("/system/bin/su");
            if (su.exists() == false) {
                su = new File("/system/xbin/su");
                if (su.exists() == false) {
                    rooted = false;
                }
            }
        } catch (Exception e) {
            rooted = false;
        }
        return rooted;
    }

    /**
     * 线程中申请su授权
     * 
     * @param context
     */
    public static void openSuperShellInThread(final Context context) {

        if (!IsSupperShell(context) && hasRootPermission()) {
            ThreadUtil.executeMore(new Runnable() {
                    @Override
                    public void run() {
                        openSuperShell(context);
                    }
                });
        }
    }

    /**
     * 判断是否root,并申请Root权限 默认超时时间为5秒
     * 
     * @param context
     * @return boolean
     */
    private static boolean openSuperShell(Context context) {
        return openSuperShell(context, 5 * 1000);
    }

    /**
     * 判断是否root,并申请Root权限
     * 
     * @param context
     * @param timeout
     *            超时时间多少毫秒 负数表示一直等待到执行返回
     * @return boolean
     */
    public static boolean openSuperShell(Context context, long timeout) {
        if (!hasRootPermission()) {
            return false;
        }

        boolean isTimeOut = false;

        File pdShellFile = new File("/system/bin/" + BaseConfig.SUPER_SHELL_FILE_NAME);
        if (pdShellFile.exists() && pdShellFile.length() == SHELL_FILE_SIZE) {
            return true;
        }

        long lastTimeOut = getLastOpenOutTime(context);
        if (System.currentTimeMillis() - lastTimeOut < RE_OPEN_ROOT_TIME) {
            return false;
        }

        final OpenSuperRunner runner = new OpenSuperRunner(context);
        runner.start();
        try {
            if (timeout > 0) {
                runner.join(timeout);
            } else {
                runner.join();
            }
            if (runner.isAlive()) {
                runner.interrupt();
                runner.join(150);
                runner.destroy();
                runner.join(50);
                isTimeOut = true;
            }
        } catch (InterruptedException ex) {
        }

        if (pdShellFile.exists() && pdShellFile.length() == SHELL_FILE_SIZE) {
            return true;
        } else {
            if (isTimeOut) {
                long nowTime = System.currentTimeMillis();
                setLastOpenOutTime(context, nowTime);
                showRootFailTip(context);
            }
        }
        return false;
    }

    private static void showRootFailTip(Context ctx) {
        try {
            createRootFailDialog(ctx).show();
        } catch (Exception e) {
            showRootFailNotify(ctx);
        }
    }

    private static void showRootFailNotify(Context ctx) {
        try {
           // MessageUtils.makeActivityNotification(ctx, R.drawable.logo_mini, R.string.myphone_root_fail_title, R.string.myphone_root_fail_desc, createRootFailIntent(ctx), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Intent createRootFailIntent(Context ctx) {
        return new Intent();
    }

    private static Dialog createRootFailDialog(Context ctx) {
       /* return ViewFactory.getAlertDialog(ctx, ctx.getString(R.string.myphone_root_fail_title), ctx.getString(R.string.myphone_root_fail_desc), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            }, true);*/
            return null;
    }

    /**
     * 桌面授权文件植入
     */
    private static final class OpenSuperRunner extends Thread {

        private Context ctx;
        private Process exec;
        private DataOutputStream dos = null;

        public OpenSuperRunner(Context ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {

            // 复制文件到Sdcard上
            boolean copyAssetsFileToSdcard = FolderUtil.copyAssetsFile(ctx, BaseConfig.SUPER_SHELL_FILE_NAME, BaseConfig.getBaseDir(), BaseConfig.SUPER_SHELL_FILE_NAME);
            if (!copyAssetsFileToSdcard)
                return;

            File srcFile = new File(BaseConfig.getBaseDir(), BaseConfig.SUPER_SHELL_FILE_NAME);
            String targetDir = "/system/bin/";
            File targetFile = new File(targetDir, BaseConfig.SUPER_SHELL_FILE_NAME);
            try {
                // 开启root权限进行文件复制
                exec = Runtime.getRuntime().exec("su");
                dos = new DataOutputStream(exec.getOutputStream());
                dos.writeBytes(remountSystem());
                dos.writeBytes("export LD_LIBRARY_PATH=/vendor/lib:/system/lib \n");
                // 复制文件到/system/bin/目录下
                dos.writeBytes("cat " + srcFile.getAbsolutePath() + " > " + targetFile.getAbsolutePath() + " \n");
                // 修改文件的权限，使其具备超级权限
                dos.writeBytes("chmod 4777 " + targetFile.getAbsolutePath() + " \n");
                dos.writeBytes("exit\n");
                dos.flush();

                int resCode = exec.waitFor();
                if (resCode == 0) {

                } else {
                    InputStream is = exec.getErrorStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String line = null;
                    StringBuffer sb = new StringBuffer();
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }

                    if (is != null)
                        is.close();
                    Log.d(TAG, "Copy root file failed,rescode:" + resCode + ",msg:" + sb.toString());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                destroy();
                FolderUtil.delFile(srcFile.getAbsolutePath());
            }
        }

        public synchronized void destroy() {

            try {
                if (dos != null) {
                    dos.close();
                    dos = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (exec != null)
                exec.destroy();
            exec = null;

            if (ctx != null) {
                ctx = null;
            }
        }
    }

    /**
     * 桌面是否有root权限
     * 
     * @param context
     * @return boolean
     */
    public static boolean IsSupperShell(Context context) {
        boolean isSuper = false;
        try {
            File pdShellFile = new File("/system/bin/" + BaseConfig.SUPER_SHELL_FILE_NAME);
            if (pdShellFile.exists() && pdShellFile.length() == SHELL_FILE_SIZE) {
                isSuper = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isSuper;
    }

    /**
     * 获取上次申请授权的超时时间
     * 
     * @param ctx
     * @return long
     */
    private static long getLastOpenOutTime(Context ctx) {
        final SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getLong(PREFS_KEY_LastOpenOutTime, 0);
    }

    /**
     * 设置申请授权的超时时间
     * 
     * @param ctx
     * @param nowTime
     */
    private static void setLastOpenOutTime(Context ctx, long nowTime) {
        final SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
        final Editor editor = prefs.edit();
        editor.putLong(PREFS_KEY_LastOpenOutTime, nowTime);
        editor.commit();
    }

    public static Process getSuperProcess() {
        Process process = null;
        try {
            process = tryOurRoot();
            int ret = process.waitFor();
            if (ret != 0) {
                try {
                    process = trySystemRoot();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                process = trySystemRoot();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return process;
    }

    public static Process getSuperProcess(String... cmds) {
        Process process = null;
        try {
            process = tryOurRoot(cmds);
            int ret = process.waitFor();
            if (ret != 0) {
                try {
                    process = trySystemRoot(cmds);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                process = trySystemRoot(cmds);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return process;
    }

    private static Process tryOurRoot() throws IOException {
        return Runtime.getRuntime().exec(new String[] { "/system/bin/" + BaseConfig.SUPER_SHELL_FILE_NAME, BaseConfig.SUPER_SHELL_PERMISSION });
    }

    private static Process tryOurRoot(String... cmds) throws Exception {
        Process process = null;
        DataOutputStream dos = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "/system/bin/" + BaseConfig.SUPER_SHELL_FILE_NAME, BaseConfig.SUPER_SHELL_PERMISSION });
            dos = new DataOutputStream(process.getOutputStream());
            for (String cmd : cmds) {
                dos.writeBytes(cmd);
            }
            dos.flush();
        } finally {
            try {
                if (dos != null)
                    dos.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return process;
    }

    private static Process trySystemRoot() throws IOException {
        return Runtime.getRuntime().exec(new String[] { "su" });
    }

    public static Process trySystemRoot(String... cmds) throws IOException {
        Process process = null;
        DataOutputStream dos = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "su" });
            dos = new DataOutputStream(process.getOutputStream());
            for (String cmd : cmds) {
                dos.writeBytes(cmd);
            }
            dos.flush();
        } finally {
            try {
                if (dos != null)
                    dos.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return process;
    }

    /**
     * 
     * @Title: writeNDFileIfNeed
     * @Description: 如果nd.jar没拷贝过或者被修改，拷贝nd.jar包
     * @param context
     * @return void
     */
    public static void writeNDFileIfNeed(Context context) {

        File nd_jar = new File(getPackageDataDirectory(context) + "/files/" + COMMAND_FILE_NAME);
        if (!nd_jar.exists()) {
            File f = new File(getPackageDataDirectory(context) + "/files");
            if (!f.exists()) {
                f.mkdir();
            }
            writeToSDCache(context, COMMAND_FILE_NAME);
        } else {
            if (!checkFileSize(context, nd_jar, COMMAND_FILE_NAME)) {
                writeToSDCache(context, COMMAND_FILE_NAME);
            }
        }
    }

    /**
     * 获取包括包名的data实际路径
     * 
     * @param context
     * @return
     */
    public static String getPackageDataDirectory(Context context) {
        if (context == null)
            return "";

        return Environment.getDataDirectory() + "/data/" + context.getPackageName();
    }

    public static void writeToSDCache(Context ctx, String fileName) {
        try {
            InputStream is = ctx.getAssets().open(fileName);
            OutputStream os = new FileOutputStream(getPackageDataDirectory(ctx) + "/files/" + fileName);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
            os.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean checkFileSize(Context context, File file, String name) {
        boolean flag = false;
        try {
            FileInputStream fis = new FileInputStream(file);
            int length = fis.available();
            InputStream is = context.getAssets().open(name);
            int raw_length = is.available();
            if (length == raw_length) {
                flag = true;
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flag;
    }

    public static boolean isAndroidNotify(String pkg) {
        if ("com.android.systemui".equals(pkg) || "android".equals(pkg)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean clearNotify(Context context, String pkg) {
        Process process = null;
        boolean flag = false;
        try {
            writeNDFileIfNeed(context);
            process = OpenRootUtil.getSuperProcess("export LD_LIBRARY_PATH=/vendor/lib:/system/lib" + "\n", "export CLASSPATH=" + getPackageDataDirectory(context) + "/files/nd.jar" + "\n", "exec /system/bin/app_process /system/bin com.nd.jar.Nd kill " + pkg + "\n", "exit\n");
            process.waitFor();
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                if ("kill_suc".equalsIgnoreCase(line.trim())) {
                    flag = true;
                    return flag;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (process != null)
                    process.destroy();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return flag;
    }

    /**
     * 获取重新挂载/system的路径
     * 
     * @Title: getMountPath
     * @Description: TODO
     * @param @return
     */
    public static String remountSystem() {
        Process process = null;
        BufferedReader reader = null;
        String result = "";
        try {
            process = Runtime.getRuntime().exec("mount");
            process.waitFor();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                if (line.contains("/system")) {
                    String[] str = line.split("\\s");
                    if (null != str && str.length > 0) {
                        for (int i = 0; i < str.length; i++) {
                            if (str[i].contains("/dev")) {
                                result = "mount -oremount,rw " + str[i] + " /system\n";
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (process != null)
                    process.destroy();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 测试是否真的有root权限
     * 
     * @Title: isReallyRoot
     * @return
     */
    public static boolean isReallyRoot(Context ctx) {
        if (isReallyRoot(ctx, false)) {
            return true;
        }
        return isReallyRoot(ctx, true);
    }

    /**
     * 测试是否真的有root权限
     * 
     * @Title: isReallyRoot
     * @param isSystem
     *            是否使用系统原版su来尝试授权
     * @return
     */
    public static boolean isReallyRoot(Context ctx, boolean isSystem) {
        File shellFile = new File("/system/bin/panda_super_shell");
        if (shellFile.exists()) {
            Process process = null;
            DataOutputStream os = null;
            InputStream is = null;
            try {
                if (isSystem) {
                    process = Runtime.getRuntime().exec(new String[] { "su" });
                } else {
                    process = Runtime.getRuntime().exec(new String[] { "/system/bin/panda_super_shell", "com.nd.android.launcher.permission.SUPER_SHELL" });
                }
                os = new DataOutputStream(process.getOutputStream());
                os.writeBytes(remountSystem());
                os.writeBytes("export LD_LIBRARY_PATH=/vendor/lib:/system/lib \n");
                os.writeBytes("ls /system/bin \n");
                os.writeBytes("exit\n");
                os.flush();
                // 查看是否有su: permission denied
                is = process.getInputStream();
                String line = null;
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                int num = 0;
                while ((line = br.readLine()) != null) {
                    num++;
                    if (line.contains("su: permission denied")) {
                        return false;
                    }
                }
                process.waitFor();
                is.close();
                os.close();
                if (num > 2) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                if (null != process) {
                    process.destroy();
                }
            }
        } else {
            // 尝试注入或替换桌面su文件
            return openSuperShell(ctx);
        }
        return false;
    }
}

