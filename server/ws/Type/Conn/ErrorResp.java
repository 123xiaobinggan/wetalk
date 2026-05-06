package com.wetalk.ws.Type.Conn;

import java.time.LocalDateTime;

public class ErrorResp {
    private Long clientMsgId;
    private LocalDateTime timestamp;

    public ErrorResp() {
    }

    public ErrorResp(Long clientMsgId, LocalDateTime timestamp) {
        this.clientMsgId = clientMsgId;
        this.timestamp = timestamp;
    }

    public Long getClientMsgId() {
        return clientMsgId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
