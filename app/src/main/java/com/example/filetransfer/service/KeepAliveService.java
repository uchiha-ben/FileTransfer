package com.example.filetransfer.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;


public class KeepAliveService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(WebService.SERVICE_ID, new Notification());
        stopSelf();
        stopForeground(true);
        return super.onStartCommand(intent, flags, startId);
    }
}