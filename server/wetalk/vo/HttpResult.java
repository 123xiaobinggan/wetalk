package com.wetalk.vo;

public class HttpResult<T> {
    private int code;
    private String msg;
    private T data;

    public HttpResult() {
    }

    public HttpResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;   
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

    public static <T> HttpResult<T> success(int code, String msg, T data) {
        return new HttpResult<>(code, msg, data);
    }

    public static <T> HttpResult<T> error(int code, String msg, T data) {
        return new HttpResult<>(code, msg, data);
    }
}
