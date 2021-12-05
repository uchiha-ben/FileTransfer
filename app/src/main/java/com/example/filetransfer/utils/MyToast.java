package com.example.filetransfer.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * MyToast
 *
 * @author uchiha.ben
 * @version 1.0
 * @date 2021/09/08
 */
public class MyToast {
    private static Toast toast;

    public static synchronized void makeText(Context context, String text) {
        if (toast == null) {
            toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        } else {
            toast.setText(text);
        }
        toast.show();
    }
}
