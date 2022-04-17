package com.example.filetransfer.domain;

import android.os.Environment;

import com.example.filetransfer.MyApplication;
import com.example.filetransfer.utils.SharedPreferencesUtil;

/**
 * UserConfig
 *
 * @author uchiha.ben
 * @version 1.0
 * @date 2021/09/08
 */
public final class DefaultConfig {
    private DefaultConfig() {
    }

    private static class SingletonHolder {
        private static final DefaultConfig INSTANCE = new DefaultConfig();
    }

    public static DefaultConfig getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 默认文件夹路径
     */
    private String defaultFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

    /**
     * 默认端口
     */
    private Integer defaultPort = 8080;

    /**
     * 是否为开放模式 (默认false true:开启(允许访问所有目录) false:关闭)
     */
    private Boolean isPublicMode = Boolean.FALSE;

    public String getDefaultFolderPath() {
        return (String) SharedPreferencesUtil.get(MyApplication.getContext(), "default_folder_path", defaultFolderPath);
    }

    public void setDefaultFolderPath(String defaultFolderPath) {
        SharedPreferencesUtil.put(MyApplication.getContext(), "default_folder_path", defaultFolderPath);
    }

    public Integer getDefaultPort() {
        return (Integer) SharedPreferencesUtil.get(MyApplication.getContext(), "default_port", defaultPort);
    }

    public void setDefaultPort(Integer defaultPort) {
        SharedPreferencesUtil.put(MyApplication.getContext(), "default_port", defaultPort);
    }

    public Boolean getPublicMode() {
        return (Boolean) SharedPreferencesUtil.get(MyApplication.getContext(), "public_mode", isPublicMode);
    }

    public void setPublicMode(Boolean publicMode) {
        SharedPreferencesUtil.put(MyApplication.getContext(), "public_mode", publicMode);
    }
}
