package com.example.filetransfer;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.filetransfer.manager.KeepLiveManager;

public class KeepLiveActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("KeepLiveActivity", "开启KeepLiveActivity");

        //左上角显示
        Window window = getWindow();
        window.setGravity(Gravity.START | Gravity.TOP);

        //设置为1像素大小
        WindowManager.LayoutParams params = window.getAttributes();
        params.x = 0;
        params.y = 0;
        params.width = 1;
        params.height = 1;
        window.setAttributes(params);
        KeepLiveManager.getInstance().setKeepLiveActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("KeepLiveActivity", "关闭KeepLiveActivity");
    }
}
