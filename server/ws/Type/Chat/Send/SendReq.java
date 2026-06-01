package com.wetalk.ws.Type.Chat.Send;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SendReq {
    private Long sessionId;
    private Long senderId;
    private String clientMsgId;
    private Boolean convType;
    private Long peerId;
    private int msgType;
    private Object content;
    private Long quoteMsgId;
    private Boolean recallFlag;
    private LocalDateTime createdTime;

    public SendReq() {
    }

    public SendReq(Long senderId, String sessionId, Boolean convType,
            Long peerId, int msgType, Object content,
            Long quoteMsgId, Boolean recallFlag,
            LocalDateTime createdTime) {
        this.senderId = senderId;
        this.sessionId = Long.valueOf(sessionId);
        this.convType = convType;
        this.peerId = peerId;
        this.msgType = msgType;
        this.content = content;
        this.quoteMsgId = quoteMsgId;
        this.recallFlag = recallFlag;
        if (this.recallFlag == null) {
            this.recallFlag = false;
        }
        this.createdTime = createdTime;
    }

}
