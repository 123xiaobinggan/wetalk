package com.wetalk.ws.Type.Chat.Send;

import lombok.Data;

@Data
public class RecallReq {
    private Long sessionId;
    private String clientMsgId;
    private Long msgId;
    private Long peerId;
    private Boolean convType;

    public RecallReq(Long sessionId, Long msgId, Long peerId, Boolean convType) {
        this.sessionId = sessionId;
        this.msgId = msgId;
        this.peerId = peerId;
        this.convType = convType;
    }
}
