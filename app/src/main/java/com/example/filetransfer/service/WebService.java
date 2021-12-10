package com.example.filetransfer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.alibaba.fastjson.JSON;
import com.example.filetransfer.MainActivity;
import com.example.filetransfer.R;
import com.example.filetransfer.domain.ApiResponse;
import com.example.filetransfer.server.CustomAsyncHttpServer;
import com.example.filetransfer.utils.MyToast;
import com.example.filetransfer.vo.FileVo;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.Part;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WebService
 *
 * @author uchiha.ben
 * @version 1.0
 * @date 2021/09/08
 */
public class WebService extends Service {
    public static final Integer PORT = 8080;
    public static final String FILE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
    public static final File mFile = new File(FILE_PATH, "upload");
    private static final CustomAsyncHttpServer server = new CustomAsyncHttpServer();
    private static final String ACTION_START_WEB_SERVICE = "START_WEB_SERVICE";
    private static final String ACTION_STOP_WEB_SERVICE = "STOP_WEB_SERVICE";
    private static final String NOTIFICATION_CHANNEL_ID = "notification_id";

    private FileUploadHolder fileUploadHolder = new FileUploadHolder();
    private NotificationManager mNotifyMgr;

    /**
     * 启动服务
     *
     * @param context
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, WebService.class);
        intent.setAction(ACTION_START_WEB_SERVICE);
        context.startService(intent);
    }

    /**
     * 停止服务
     *
     * @param context
     */
    public static void stop(Context context) {
        Intent intent = new Intent(context, WebService.class);
        intent.setAction(ACTION_STOP_WEB_SERVICE);
        context.startService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_WEB_SERVICE.equals(action)) {
                initNotificationManager();
                startServer();
            } else if (ACTION_STOP_WEB_SERVICE.equals(action)) {
                stopSelf();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
        }
    }

    /**
     * 初始化通知管理器
     */
    private void initNotificationManager() {
        mNotifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "APP_NAME", NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(null, null);
            if (mNotifyMgr != null) {
                mNotifyMgr.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 发送事件通知
     *
     * @param file
     */
    private void doSendNotify(File file) {
        if (file != null && file.exists()) {
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
    }

    /**
     * 通知Activity
     *
     * @param file
     */
    private void doNotifyActivity(File file) {
        if (file != null) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    /**
     * 启动文件服务器
     */
    private void startServer() {
        // 处理文件上传
        server.post("/upload", (request, response) -> {
            if (!mFile.exists() || !mFile.isDirectory()) {
                mFile.mkdirs();
            }
            MultipartFormDataBody mBody = request.getBody();
            mBody.setMultipartCallback(part -> {
                if (part.isFile()) {
                    fileUploadHolder.file = new File(mFile.getAbsolutePath(), part.getFilename());
                    try {
                        fileUploadHolder.outputStream = new BufferedOutputStream(new FileOutputStream(fileUploadHolder.file));
                        mBody.setDataCallback((emitter, bb) -> {
                            byte[] allByteArray = bb.getAllByteArray();
                            try {
                                fileUploadHolder.outputStream.write(allByteArray, 0, allByteArray.length);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });
            request.setEndCallback(ex -> {
                try {
                    if (fileUploadHolder.outputStream != null) {
                        fileUploadHolder.outputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (ex != null && fileUploadHolder.file != null) {
                    fileUploadHolder.file.delete();
                }
                response.send("application/json", JSON.toJSONString(ApiResponse.success()));
                doSendNotify(fileUploadHolder.file);
                doNotifyActivity(fileUploadHolder.file);
            });
        });

        // 处理文件下载
        server.get("/file/.*", (request, response) -> {
            try {
                String fileName = request.getUrl().replaceAll("/file/", "");
                fileName = URLDecoder.decode(fileName, "UTF-8");
                File file = new File(mFile, fileName);
                if (!file.isFile()) {
                    response.code(404);
                    response.end();
                    return;
                }
                try {
                    FileInputStream is = new FileInputStream(file);
                    response.getHeaders().set("Content-Length", String.valueOf(is.available()));
                    response.getHeaders().add("Content-Type", server.getContentType(file.getPath()));
                    response.code(200);
                    Util.pump(is, is.available(), response, ex -> response.end());
                } catch (IOException ex) {
                    response.code(404);
                    response.end();
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });

        // 处理文件列表
        server.get("/list", (request, response) -> {
            ArrayList<FileVo> files = new ArrayList<FileVo>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (mFile.listFiles() != null) {
                for (File f : mFile.listFiles()) {
                    if (f.isFile() && !f.getName().endsWith(".hwbk")) {
                        FileVo fileVo = new FileVo();
                        fileVo.setName(f.getName());
                        fileVo.setPath(f.getPath());
                        fileVo.setLastModified(f.lastModified());
                        fileVo.setLastModifiedTime(sdf.format(new Date(f.lastModified())));
                        files.add(fileVo);
                    }
                }
            }
            Collections.sort(files, (lhs, rhs) -> lhs.getLastModified() - rhs.getLastModified() > 0 ? 1 : 0);
            response.send("application/json", JSON.toJSONString(ApiResponse.success().data(files)));
        });

        // 处理文件删除
        server.get("/delete/.*", (request, response) -> {
            try {
                String fileName = request.getUrl().replaceAll("/delete/", "");
                fileName = URLDecoder.decode(fileName, "UTF-8");
                File file = new File(mFile, fileName);
                if (!file.isFile()) {
                    response.code(404);
                    response.end();
                    return;
                }
                file.delete();
                response.send("application/json", JSON.toJSONString(ApiResponse.success()));
                doNotifyActivity(file);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });

        // 处理首页
        server.directory(getBaseContext(), "/", "public");

        // 处理静态资源
        server.statics(getBaseContext(), "/.*", "public");

        // 监听端口
        server.listen(PORT);
    }

    class FileUploadHolder {
        private File file;
        private BufferedOutputStream outputStream;
    }

}
