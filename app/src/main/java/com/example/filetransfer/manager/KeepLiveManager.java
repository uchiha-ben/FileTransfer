package com.example.filetransfer.manager;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.example.filetransfer.KeepLiveActivity;
import com.example.filetransfer.receiver.KeepLiveReceiver;

import java.lang.ref.WeakReference;

public class KeepLiveManager {
    private static final KeepLiveManager ourInstance = new KeepLiveManager();

    public static KeepLiveManager getInstance() {
        return ourInstance;
    }

    private KeepLiveManager() {
    }

    // 弱引用，防止内存泄漏
    private WeakReference<KeepLiveActivity> reference;

    private KeepLiveReceiver receiver;

    public void setKeepLiveActivity(KeepLiveActivity activity) {
        reference = new WeakReference<>(activity);
    }

    // 开启透明Activity
    public void startKeepLiveActivity(Context context) {
        Intent intent = new Intent(context, KeepLiveActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    // 关闭透明Activity
    public void finishKeepLiveActivity() {
        if (reference != null && reference.get() != null) {
            reference.get().finish();
        }
    }

    // 注册广播
    public void registerKeepLiveReceiver(Context context) {
        receiver = new KeepLiveReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        context.registerReceiver(receiver, filter);
    }

    // 反注册
    public void unregisterKeepLiveReceiver(Context context){
        if(receiver != null){
            context.unregisterReceiver(receiver);
        }
    }
}
