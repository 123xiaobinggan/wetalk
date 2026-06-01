package com.wetalk.ws.Type.Conn;

import java.time.LocalDateTime;

public class AckResp {
    private Long msgId;
    private LocalDateTime createdTime;

    public AckResp() {
    }

    public AckResp(Long msgId, LocalDateTime createdTime) {
        this.msgId = msgId;
        this.createdTime = createdTime;
    }

    public Long getMsgId() {
        return msgId;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

}
