package com.example.filetransfer.service;

import android.content.Context;
import android.os.Environment;

import com.alibaba.fastjson.JSON;
import com.example.filetransfer.domain.ApiResponse;
import com.example.filetransfer.server.CustomAsyncHttpServer;
import com.example.filetransfer.vo.FileVo;
import com.koushikdutta.async.Util;
import com.koushikdutta.async.http.body.MultipartFormDataBody;

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
public class WebService {
    public interface IOnUploadListener {
        void onRefresh(File file);
    }

    public static final Integer PORT = 8080;
    public static final String FILE_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
    public static final File mFile = new File(FILE_PATH, "upload");
    private static final CustomAsyncHttpServer server = new CustomAsyncHttpServer();

    public static void start(Context context, IOnUploadListener onUploadListener) {
        // 处理文件上传
        server.post("/upload", (request, response) -> {
            if (!mFile.exists() || !mFile.isDirectory()) {
                mFile.mkdirs();
            }
            MultipartFormDataBody mBody = request.getBody();
            AtomicReference<OutputStream> mFileOutputStream = new AtomicReference();
            AtomicReference<File> cache_file = new AtomicReference();
            mBody.setMultipartCallback(part -> {
                if (part.isFile()) {
                    cache_file.set(new File(mFile.getAbsolutePath(), part.getFilename()));
                    try {
                        mFileOutputStream.set(new FileOutputStream(cache_file.get()));
                        mBody.setDataCallback((emitter, bb) -> {
                            byte[] allByteArray = bb.getAllByteArray();
                            try {
                                mFileOutputStream.get().write(allByteArray, 0, allByteArray.length);
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
                    if (mFileOutputStream.get() != null) {
                        mFileOutputStream.get().close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (onUploadListener != null && cache_file.get() != null) {
                    onUploadListener.onRefresh(cache_file.get());
                }
                response.send("application/json", JSON.toJSONString(ApiResponse.success()));
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
                if (onUploadListener != null && file != null) {
                    onUploadListener.onRefresh(file);
                }
                response.send("application/json", JSON.toJSONString(ApiResponse.success()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });

        // 处理首页
        server.directory(context, "/", "public");

        // 处理静态资源
        server.statics(context, "/.*", "public");

        // 监听端口
        server.listen(PORT);
    }
}
