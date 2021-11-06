package com.app.evolution.application;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

import com.app.evolution.engine.app.receivers.AntBroadcastStaticReceiver;
import com.app.evolution.engine.app.utils.NetworkUtil;
import android.widget.Toast;

public class ApplicationChangeReceiver extends AntBroadcastStaticReceiver {
    
    @Override
    public void onReceiveHandler(Context ctx, Intent intent) {
        if (null == intent)
            return;
        final String action = intent.getAction();
        if (Intent.ACTION_PACKAGE_CHANGED.equals(action) ||
            Intent.ACTION_PACKAGE_REMOVED.equals(action) ||
            Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            // TODO: need to update the all app list
            final String packageName = intent.getData().getSchemeSpecificPart();
            final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

            if (packageName == null || packageName.length() == 0) {
                return;
            }

            if (ctx.getPackageName().equals(packageName)) {
                return;
            }

            if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {

            } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                if (!replacing) {
                    Toast.makeText(ctx, "Package Removed", Toast.LENGTH_LONG).show();                    
                }
            } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                if (!replacing) {

                } 
            }
        }
    }
    
}

