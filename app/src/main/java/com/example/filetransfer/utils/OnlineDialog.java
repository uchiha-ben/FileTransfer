package com.example.filetransfer.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * ConvertUtil
 *
 * @author uchiha.ben
 * @version 1.0
 * @date 2021/09/08
 */
public class OnlineDialog {
    public static final OkHttpClient okHttpClient = new OkHttpClient();

    public static void init(final Context context, final String gtUrl) {
        try {
            final AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... strings) {
                    try {
                        Request request = new Request.Builder().url(gtUrl).addHeader("User-Agent",
                                "Mozilla/5.0 (iPhone; CPU iPhone OS 12_1_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/16D57 Version/12.0 Safari/604.1").build();
                        Response response = okHttpClient.newCall(request).execute();
                        String msg = response.body().string();
                        return msg;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(final String result) {
                    try {
                        if (result != null && !result.isEmpty()) {
                            //解析JSON
                            JSONObject jsonObject = JSON.parseObject(result);
                            final String title = jsonObject.getString("title");
                            final String message = jsonObject.getString("message");
                            final String uriStr = jsonObject.getString("uri");
                            final String txt = jsonObject.getString("txt");
                            final String intentStr = jsonObject.getString("intent");
                            if (!getUpdateMessage(context).trim().equalsIgnoreCase(result.trim())) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(title)
                                        .setMessage(message).setPositiveButton("确定", (dialogInterface, i) -> {
                                            if (!TextUtils.isEmpty(txt)) {
                                                ClipBoardUtil.copy(context, txt);
                                            }
                                            if (!TextUtils.isEmpty(uriStr) && !TextUtils.isEmpty(intentStr)) {
                                                Uri uri = Uri.parse(uriStr);
                                                Intent intent = new Intent(intentStr, uri);
                                                context.startActivity(intent);
                                            }
                                        }).setNeutralButton("不再提醒", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                setUpdateMessage(context, result.trim());
                                            }
                                        }).setNegativeButton("取消", null);
                                builder.create().show();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            asyncTask.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setUpdateMessage(Context context, String content) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("update", content);
        editor.commit();
    }

    private static String getUpdateMessage(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("data", Context.MODE_PRIVATE);
        return sharedPreferences.getString("update", "");
    }
}
