package com.app.evolution.engine.app.config;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class BaseConfig {
    
    public static String TAG = BaseConfig.class.getSimpleName();
    protected static Context applicationContext;
    private static boolean initConfig = false;

	
    private static String BASE_DIR = Environment.getExternalStorageDirectory() + "/bitants";
    public static boolean isInitConfig() {
        return initConfig;
    }
	
    /** 防止图库扫描 */
	public static String NO_MEDIA = getBaseDir() + "/.nomedia";
    
    public static String TEMP_DIR = getBaseDir() + "/tmp/";

    /** 备份目录 */
	public static String BACKUP_DIR = getBaseDir() + "/Backup/";
    
    public static final String SUPER_SHELL_FILE_NAME = "super_shell";
    /**
     * 调用 root权限的shell时传进的参数，过滤权限，防止其他应用的操作
     */
    public static final String SUPER_SHELL_PERMISSION = "com.bitants.launcher.permission.SUPER_SHELL";
    
    public static String getBaseDir(){
        if(!isInitConfig()){
            init(applicationContext);
        }
        if(isInitConfig()){          
            return BASE_DIR;
        }else{
            throw new RuntimeException("getBaseDir() Exception"); 
        }
    }
    
    public static void init(Context mContext){
        try{
            if(initConfig)
                return;

            if(mContext == null || mContext.getResources() == null)
                return;


			initConfig = true;

        }catch(Exception e){
            Log.e(TAG, e.toString());
		}
    }
    
    public static void initDir(String base_app_dir) {
        BASE_DIR = Environment.getExternalStorageDirectory() + "/"
                + base_app_dir;

        /** 防止图库扫描 */
        NO_MEDIA = BASE_DIR + "/.nomedia";
       

        TEMP_DIR = BASE_DIR + "/tmp/";

        /** 备份目录 */
        BACKUP_DIR = BASE_DIR + "/Backup/";

    }
    
    public static Context getApplicationContext() {
        return applicationContext;
    }

    public static void setApplicationContext(Context ctx) {
        applicationContext = ctx;
    }
	
	public static String getApplicationDataPath(){
        return Environment.getDataDirectory() + "/data/" + getApplicationContext().getPackageName();
	}
    
}
