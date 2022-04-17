package com.example.filetransfer.vo;

import java.util.List;

/**
 * FileVo
 *
 * @author uchiha.ben
 * @version 1.0
 * @date 2021/09/08
 */
public class FileResultVo {
    /**
     * 当前路径
     */
    private String currentPath;

    /**
     * 父级路径
     */
    private String parentPath;

    /**
     * 目录下文件
     */
    private List<FileVo> files;

    public String getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public List<FileVo> getFiles() {
        return files;
    }

    public void setFiles(List<FileVo> files) {
        this.files = files;
    }
}
