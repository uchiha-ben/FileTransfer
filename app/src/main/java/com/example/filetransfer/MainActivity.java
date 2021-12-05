package com.example.filetransfer;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.filetransfer.server.CustomAsyncHttpServer;
import com.example.filetransfer.service.WebService;
import com.example.filetransfer.utils.ClipBoardUtil;
import com.example.filetransfer.utils.IpUtil;
import com.example.filetransfer.utils.MyToast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MainActivity
 *
 * @author uchiha.ben
 * @version 1.0
 * @date 2021/09/08
 */
public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 454;
    private static final String NOTIFICATION_CHANNEL_ID = "notification_id";

    private long mExitTime;
    private TextView tvPath;
    private ListView listview;
    private File currentParent;
    private File[] currentFiles;
    private NotificationManager mNotifyMgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 检查存储权限
        if (!checkStoragePermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
        }

        // 初始化界面
        initView();

        // 初始化通知
        mNotifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "APP_NAME", NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(null, null);
            if (mNotifyMgr != null) {
                mNotifyMgr.createNotificationChannel(channel);
            }
        }

        // 启动Web服务
        WebService.start(getBaseContext(), file -> {
            runOnUiThread(() -> {
                initData();
                inflateListView(currentFiles);

                if (file != null && file.exists()) {
                    shareFile(file);
                    Intent intentGet = new Intent(this, MainActivity.class);
                    PendingIntent pendingIntentGet = PendingIntent.getActivity(this, 0, intentGet, 0);
                    Notification notificationGet = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                            .setAutoCancel(true)
                            .setContentTitle("检测到文件上传")
                            .setContentText(file.getName())
                            .setWhen(System.currentTimeMillis())
                            .setSmallIcon(R.drawable.icon)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon))
                            .setContentIntent(pendingIntentGet)
                            .build();
                    mNotifyMgr.notify(1, notificationGet);
                }
            });
        });
    }

    /**
     * 初始化界面
     */
    private void initView() {
        tvPath = findViewById(R.id.tvPath);
        listview = findViewById(R.id.listview);
        initData();
        inflateListView(currentFiles);
        listview.setOnItemClickListener((arg0, arg1, arg2, arg3) -> {
            if (arg2 == 0) {
                try {
                    if (!currentParent.getCanonicalFile().equals(
                            Environment.getExternalStorageDirectory()
                                    .getAbsoluteFile())) {
                        currentParent = currentParent.getParentFile();
                        currentFiles = currentParent.listFiles();
                        inflateListView(currentFiles);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                arg2 = arg2 - 1;
                if (currentFiles[arg2].isDirectory()) {
                    File[] mfiles = currentFiles[arg2].listFiles();
                    if (mfiles == null || mfiles.length == 0) {
                        MyToast.makeText(MainActivity.this, "当前路径不可访问或该路径下没有文件");
                    } else {
                        currentParent = currentFiles[arg2];
                        currentFiles = mfiles;
                        inflateListView(currentFiles);
                    }
                } else {
                    shareFile(currentFiles[arg2]);
                }
            }
        });
    }

    /**
     * 初始化默认值
     */
    private void initData() {
        File root = new File(WebService.FILE_PATH, "upload");
        if (!root.exists() || !root.isDirectory()) {
            root.mkdirs();
        }
        currentParent = root;
        File[] files = root.listFiles();
        List<File> tmpList = new ArrayList<>();
        if (files != null && files.length > 0) {
            tmpList.addAll(Arrays.asList(files));
        }
        Collections.sort(tmpList, (lhs, rhs) -> lhs.lastModified() - rhs.lastModified() > 0 ? 1 : 0);
        currentFiles = tmpList.toArray(new File[tmpList.size()]);
    }


    /**
     * 文件分享
     *
     * @param file
     */
    private void shareFile(File file) {
        if (null != file && file.exists()) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String fileName = file.getName();
            sendIntent.setType(CustomAsyncHttpServer.getContentType(fileName));
            startActivity(Intent.createChooser(sendIntent, "分享文件"));
        } else {
            Toast.makeText(this, "分享文件不存在", Toast.LENGTH_SHORT).show();
        }
    }

    private void inflateListView(File[] files) {
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Map<String, Object> root = new HashMap<String, Object>();
        root.put("icon", R.drawable.folder);
        root.put("rootName", "../");
        list.add(root);

        for (int i = 0; i < files.length; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            if (files[i].isDirectory()) {
                map.put("icon", R.drawable.folder);
            } else {
                map.put("icon", R.drawable.file);
            }
            map.put("filename", files[i].getName());
            map.put("lastModified", sdf.format(new Date(files[i].lastModified())));
            list.add(map);
        }
        SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, list,
                R.layout.item, new String[]{"icon", "rootName", "filename", "lastModified"}, new int[]{
                R.id.icon, R.id.root_name, R.id.file_name, R.id.last_modified});
        listview.setAdapter(adapter);
        try {
            tvPath.setText(currentParent.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 动态权限校验
     *
     * @return
     */
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 动态权限回调
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "存储权限授权成功", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "存储权限授权失败", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                String address = IpUtil.getIPAddress(this) + ":" + WebService.PORT;
                ClipBoardUtil.copy(getBaseContext(), address);
                Toast.makeText(getBaseContext(), "浏览器访问" + address + "上传文件", Toast.LENGTH_LONG).show();
                mExitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}