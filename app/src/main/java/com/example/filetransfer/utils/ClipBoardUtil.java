package com.example.filetransfer.utils;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;

/**
 * ClipBoardUtil
 *
 * @author uchiha.ben
 * @version 1.0
 * @date 2021/09/08
 */
public class ClipBoardUtil {

    public static void copy(Context context, String content) {
        //获取剪贴板管理器
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        // 创建普通字符型ClipData
        ClipData mClipData = ClipData.newPlainText("content", content);
        // 将ClipData内容放到系统剪贴板里。
        cm.setPrimaryClip(mClipData);
    }

    /**
     * 获取剪切板内容
     *
     * @return
     */
    public static String paste(Context context) {
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager != null) {
            if (manager.hasPrimaryClip() && manager.getPrimaryClip().getItemCount() > 0) {
                String addedText = manager.getPrimaryClip().getItemAt(0).getText().toString();
                if (!TextUtils.isEmpty(addedText)) {
                    return addedText;
                }
            }
        }
        return "";
    }

    /**
     * 清空剪切板
     */
    public static void clear(Context context) {
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager != null) {
            try {
                manager.setPrimaryClip(manager.getPrimaryClip());
                manager.setPrimaryClip(ClipData.newPlainText("", ""));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
