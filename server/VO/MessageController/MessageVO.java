package com.wetalk.VO.MessageController;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MessageVO<T> {
    private Long msgId;
    private String sessionId;
    private Boolean convType;
    private Long senderId;
    private Long peerId;
    private Integer msgType;
    private T content;
    private Boolean recallFlag;
    private Long quoteMsgId;
    private LocalDateTime createdTime;

    public MessageVO(Long msgId, String sessionId, Boolean convType,
            Long senderId, Long peerId, Integer msgType, T content,
            Long quoteMsgId, Boolean recallFlag, LocalDateTime createdTime) {
        this.msgId = msgId;
        this.sessionId = sessionId;
        this.convType = convType;
        this.senderId = senderId;
        this.peerId = peerId;
        this.msgType = msgType;
        this.content = content;
        this.recallFlag = recallFlag;
        this.quoteMsgId = quoteMsgId;
        this.createdTime = createdTime;
    }

    public MessageVO(Long msgId, Long sessionId, Boolean convType,
            Long senderId, Long peerId, Integer msgType, T content,
            Long quoteMsgId, Boolean recallFlag, LocalDateTime createdTime) {
        this.msgId = msgId;
        this.sessionId = String.valueOf(sessionId);
        this.convType = convType;
        this.senderId = senderId;
        this.peerId = peerId;
        this.msgType = msgType;
        this.content = content;
        this.recallFlag = recallFlag;
        this.quoteMsgId = quoteMsgId;
        this.createdTime = createdTime;
    }

}
