package com.wetalk.ws.protocol;

import java.util.*;

public class WsIncomeMessage<T> {
    private String type;
    private String event;
    private String clientMsgId;
    private T data;

    public WsIncomeMessage() {
    }

    public WsIncomeMessage(String type, String event, String clientMsgId, T data) {
        this.type = type;
        this.event = event;
        this.clientMsgId = clientMsgId;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getClientMsgId(){
        return clientMsgId;
    }

    public void setClientMsgId(String clientMsgId){
        this.clientMsgId = clientMsgId;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
