package com.app.evolution.application.updater.listeners;

import org.json.JSONObject;

import com.app.evolution.application.updater.models.UpdateModel;

public interface UpdateListener {
    void onJsonDataReceived(UpdateModel updateModel, JSONObject jsonObject);
    void onError(String error);
}
