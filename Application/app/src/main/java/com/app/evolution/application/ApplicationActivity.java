package com.app.evolution.application;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.Snackbar;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.app.evolution.R;
import com.app.evolution.application.updater.UpdateVersion;
import com.app.evolution.application.updater.models.UpdateModel;
import com.app.evolution.application.updater.utils.UpdateUtils;
import com.app.evolution.application.updater.listeners.UpdateListener;
import com.app.evolution.application.updater.tasks.UpdateTask;
import com.app.evolution.engine.app.utils.FolderUtil;

public class ApplicationActivity extends AppCompatActivity {

    private ApplicationNetworkReceiver network;
    static final String ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    IntentFilter filter = new IntentFilter(ACTION);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application);
        Toolbar mToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        boolean checkUpdate = UpdateVersion.getInstance().checkForUpdates(this);
        if (!checkUpdate) {
            UpdateVersion update = new UpdateVersion();
            update.with(this);
            update.getUpdateVersion();
            update.setOnUpdateVersionListener(new UpdateVersion.OnUpdateVersionListener(){
                    @Override
                    public void onUpdateVersion(final UpdateModel models) {
                        new AlertDialog.Builder(ApplicationActivity.this)
                            .setTitle(R.string.actions_update)
                            .setMessage(String.format(getResources().getString(R.string.update_dialog_msg), models.getVersionCode()))
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(ApplicationActivity.this, models.getUrl(), Toast.LENGTH_SHORT).show();
                                    Intent mApplication = new Intent(ApplicationActivity.this, UpdateActivity.class);
                                    mApplication.putExtra(UpdateActivity.EXTRA_NAME, "update-version-code-" + models.getVersionCode() + ".apk");
                                    mApplication.putExtra(UpdateActivity.EXTRA_PATH, FolderUtil.getDownloadDir(ApplicationActivity.this));
                                    mApplication.putExtra(UpdateActivity.EXTRA_URL, models.getUrl());
                                    startActivity(mApplication);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    }

                    @Override
                    public void onLatestVersion(UpdateModel models) {
                        Toast.makeText(ApplicationActivity.this, models.getLatestCode(ApplicationActivity.this), Toast.LENGTH_SHORT).show();
                    }
                });
        } else {

        }

        network = new ApplicationNetworkReceiver(this);
        network.setOnConnectionStatus(new ApplicationNetworkReceiver.OnConnectionStatus(){
                @Override
                public void onConnect() {                 
                    ApplicationLogger.sendBroadcast(ApplicationActivity.this, "Connection Success");
                }
                @Override
                public void onDiskConnect() {
                    ApplicationLogger.sendBroadcast(ApplicationActivity.this, "Disk Connect");
                }   
            });
        registerReceiver(network, filter);
    }

    public void displaySnackbar(String text, String actionName, View.OnClickListener action) {
        Snackbar snack = Snackbar.make(findViewById(R.id.coordinator_layout), text, Snackbar.LENGTH_LONG)
            .setAction(actionName, action);

        View v = snack.getView();
        v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
        ((TextView) v.findViewById(android.support.design.R.id.snackbar_text)).setTextColor(Color.WHITE);
        ((TextView) v.findViewById(android.support.design.R.id.snackbar_action)).setTextColor(Color.BLACK);

        snack.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.registerReceiver(network, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(network);
    }
}
