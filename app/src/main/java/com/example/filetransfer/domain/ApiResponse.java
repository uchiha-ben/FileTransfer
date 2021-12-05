package com.example.filetransfer.domain;


import com.example.filetransfer.constants.ApiCode;

import java.io.Serializable;

/**
 * ApiResponse
 *
 * @author uchiha.ben
 * @version 1.0
 * @date 2021/09/08
 */
public class ApiResponse<T> implements Serializable {
    private Integer code;

    private String message;

    private T data;

    private ApiResponse() {

    }

    public ApiResponse(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public static ApiResponse success() {
        return new ApiResponse(ApiCode.SUCCESS.getCode(), ApiCode.SUCCESS.getMessage());
    }

    public static ApiResponse error() {
        return new ApiResponse(ApiCode.ERROR.getCode(), ApiCode.ERROR.getMessage());
    }

    public ApiResponse code(Integer code) {
        this.setCode(code);
        return this;
    }

    public ApiResponse message(String message) {
        this.setMessage(message);
        return this;
    }

    public ApiResponse data(T data) {
        this.setData(data);
        return this;
    }
}
