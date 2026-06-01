package com.wetalk.ws.protocol;

import java.time.LocalDateTime;

public class WsResult<T> {
    private String type;
    private String event;
    private int code;
    private String msg;
    private LocalDateTime resptime;
    private String clientMsgId;
    private T data;

    public WsResult() {
    }

    public WsResult(String type, String event,
            int code, String msg, String clientMsgId, T data) {
        this.type = type;
        this.event = event;
        this.code = code;
        this.msg = msg;
        this.resptime = LocalDateTime.now();
        this.clientMsgId = clientMsgId;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public String getEvent() {
        return event;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public LocalDateTime getResptime() {
        return resptime;
    }

    public String getClientMsgId() {
        return clientMsgId;
    }

    public T getData() {
        return data;
    }

}
