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
    private Long size;

    /**
     * 文件路径
     */
    private String path;

    /**
     * 最新更新时间戳
     */
    private Long lastModified;

    /**
     * 最新更新时间
     */
    private String lastModifiedTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
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

    public String getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(String lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
