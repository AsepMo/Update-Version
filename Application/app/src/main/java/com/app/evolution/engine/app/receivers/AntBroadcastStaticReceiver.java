package com.app.evolution.engine.app.receivers;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.util.Log;


public abstract class AntBroadcastStaticReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try{
            onReceiveHandler(context, intent);
        }catch(Exception e){
            Log.e("Error",  e.getMessage());
        }

    }

    public abstract void onReceiveHandler(Context context, Intent intent);
}

