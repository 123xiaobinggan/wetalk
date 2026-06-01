package com.wetalk.mq.protocol;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.time.LocalTime;

@Data
@RequiredArgsConstructor
public class WsEvent {
    private String type;
    private String event;
    private Long fromUserId;
    private Long sessionId;
    private String payload;

    public WsEvent(String type, String event, Long fromUserId, Long sessionId, String payload) {
        this.type = type;
        this.event = event;
        this.fromUserId = fromUserId;
        this.sessionId = sessionId;
        this.payload = payload;
    }
}
