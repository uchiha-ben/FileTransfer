package com.example.filetransfer.vo;

/**
 * FileVo
 *
 * @author uchiha.ben
 * @version 1.0
 * @date 2021/09/08
 */
public class FileVo {
    /**
     * 文件名
     */
    private String name;

    /**
     * 文件大小
     */
    private String size;

    /**
     * 文件路径
     */
    private String path;

    /**
     * 文件夹
     */
    private String folder;

    /**
     * 最新更新时间戳
     */
    private Long lastModified;

    /**
     * 是否为文件
     */
    private boolean isFile;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }
}
