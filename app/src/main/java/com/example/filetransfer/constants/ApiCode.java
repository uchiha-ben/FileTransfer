package com.example.filetransfer.constants;

public enum ApiCode {
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    UN_LOGIN(401, "未登录"),
    UN_AUTH(403, "未授权");
    Integer code;
    String message;

    ApiCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
