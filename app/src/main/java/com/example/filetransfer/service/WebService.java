package com.example.filetransfer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;

import com.alibaba.fastjson.JSON;
import com.example.filetransfer.MainActivity;
import com.example.filetransfer.R;
import com.example.filetransfer.domain.ApiResponse;
import com.example.filetransfer.domain.DefaultConfig;
import com.example.filetransfer.server.CustomAsyncHttpServer;
import com.example.filetransfer.vo.FileVo;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.util.StreamUtility;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;

/**
 * WebService
 *
 * @author uchiha.ben
 * @version 1.0
 * @date 2021/09/08
 */
public class WebService extends Service {
    public static final Integer SERVICE_ID = 0X11;
    private static final String NOTIFICATION_CHANNEL_ID = "notification_id";

    private CustomAsyncHttpServer server = new CustomAsyncHttpServer();
    private FileUploadHolder fileUploadHolder = new FileUploadHolder();
    private NotificationManager mNotifyMgr;

    /**
     * 启动服务
     *
     * @param context
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, WebService.class);
        context.startService(intent);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT < 18) {
            startForeground(WebService.SERVICE_ID, new Notification());
        } else {
            Intent innerIntent = new Intent(this, KeepAliveService.class);
            startService(innerIntent);
            startForeground(WebService.SERVICE_ID, new Notification());
        }

        initNotificationManager();
        startServer();
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
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "FILE_TRANSFER", NotificationManager.IMPORTANCE_HIGH);
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
            MultipartFormDataBody mBody = request.getBody();
            mBody.setMultipartCallback(part -> {
                if (!part.isFile()) {
                    if (mBody.getDataCallback() == null) {
                        mBody.setDataCallback((DataEmitter emitter, ByteBufferList bb) -> {
                            String path = readURLStr(new String(bb.getAllByteArray()));
                            fileUploadHolder.path = path;
                            bb.recycle();
                        });
                    }
                } else {
                    fileUploadHolder.file = new File(fileUploadHolder.path, part.getFilename());
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
        server.get("/file", (request, response) -> {
            String path = readURLStr(request.get("path"));
            File file = new File(path);
            if (!file.isFile()) {
                response.code(404);
                response.end();
                return;
            }
            try {
                FileInputStream is = new FileInputStream(file);
                response.code(200);
                response.getHeaders().add("Content-Type", server.getContentType(path));
                response.getHeaders().add("content-disposition", "attachment;filename=" + URLEncoder.encode(file.getName(), "UTF-8"));
                Util.pump(is, is.available(), response,
                        ex -> {
                            StreamUtility.closeQuietly(is);
                            response.end();
                        });
            } catch (IOException ex) {
                response.code(404);
                response.end();
            }
        });

        // 处理文件列表
        server.get("/list", (request, response) -> {
            String path = readURLStr(TextUtils.isEmpty(request.get("path")) ? DefaultConfig.getInstance().getDefaultFolderPath() : request.get("path"));
            File mFile = new File(path);
            if (mFile.isDirectory() && !mFile.getAbsolutePath().equals(
                    Environment.getExternalStorageDirectory().getParentFile()
                            .getAbsolutePath()) && (mFile.getAbsolutePath().equals(DefaultConfig.getInstance().getDefaultFolderPath()) || DefaultConfig.getInstance().getPublicMode())) {
                ArrayList<FileVo> files = new ArrayList<FileVo>();
                FileVo root = new FileVo();
                root.setName("../");
                root.setFile(false);
                root.setPath(mFile.getAbsolutePath());
                root.setFolder(mFile.getParent());
                root.setLastModified(Long.MAX_VALUE);
                files.add(root);
                if (mFile.listFiles() != null) {
                    for (File f : mFile.listFiles()) {
                        if (!f.getName().endsWith(".hwbk")) {
                            FileVo fileVo = new FileVo();
                            fileVo.setName(f.getName());
                            fileVo.setPath(f.getAbsolutePath());
                            if (!f.isDirectory()) {
                                fileVo.setFolder(f.getParent());
                                long fileLen = f.length();
                                DecimalFormat df = new DecimalFormat("0.00");
                                if (fileLen > 1024 * 1024) {
                                    fileVo.setSize(df.format(fileLen * 1f / 1024 / 1024) + "MB");
                                } else if (fileLen > 1024) {
                                    fileVo.setSize(df.format(fileLen * 1f / 1024) + "KB");
                                } else {
                                    fileVo.setSize(df.format(fileLen) + "B");
                                }
                            } else {
                                fileVo.setFolder(f.getAbsolutePath());
                            }
                            fileVo.setLastModified(f.lastModified());
                            fileVo.setFile(f.isFile());
                            files.add(fileVo);
                        }
                    }
                }
                Collections.sort(files, (lhs, rhs) -> lhs.getLastModified() - rhs.getLastModified() > 0 ? 1 : 0);
                response.send("application/json", JSON.toJSONString(ApiResponse.success().data(files)));
            } else {
                response.send("application/json", JSON.toJSONString(ApiResponse.error().message("目录不存在或没有权限")));
            }
        });

        // 处理文件删除
        server.get("/delete", (request, response) -> {
            String path = readURLStr(request.get("path"));
            File file = new File(path);
            if (!file.isFile()) {
                response.code(404);
                response.end();
                return;
            }
            file.delete();
            response.send("application/json", JSON.toJSONString(ApiResponse.success()));
            doNotifyActivity(file);
        });

        // 处理首页
        server.directory(getBaseContext(), "/", "public");

        // 处理静态资源
        server.statics(getBaseContext(), "/.*", "public");

        // 监听端口
        server.listen(DefaultConfig.getInstance().getDefaultPort());
    }

    /**
     * 解析URL中文字符
     *
     * @param str
     * @return
     */
    private String readURLStr(String str) {
        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    class FileUploadHolder {
        private String path;
        private File file;
        private BufferedOutputStream outputStream;
    }

}
