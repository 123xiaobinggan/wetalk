package com.wetalk.ws.protocol;

import java.util.*;
import lombok.Data;

@Data
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
}
