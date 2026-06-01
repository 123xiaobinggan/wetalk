package com.wetalk.model;

import org.apache.ibatis.annotations.Mapper;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.wetalk.utils.*;
import lombok.Data;

@Data
public class Message {
    private Long msgId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;
    private Boolean convType;
    private Long senderId;
    private Long peerId;
    private Integer msgType;
    private String content;
    private Boolean recallFlag;
    private Long quoteMsgId;
    private LocalDateTime createdTime;

    public Message() {
    }

    public Message(Boolean convType, Long sessionId, Long senderId,
            Long peerId, Integer msgType, String content, Long quoteMsgId, Boolean recallFlag) {
        this.convType = convType;
        this.sessionId = sessionId;
        this.senderId = senderId;
        this.peerId = peerId;
        this.msgType = msgType;
        this.content = content;
        this.quoteMsgId = quoteMsgId;
        this.recallFlag = recallFlag;
        this.createdTime = TimeUtils.currentTime();
    }

    public Message(Boolean convType, String sessionId, Long senderId,
            Long peerId, Integer msgType, String content, Long quoteMsgId, Boolean recallFlag) {
        this.convType = convType;
        this.sessionId = Long.valueOf(sessionId);
        this.senderId = senderId;
        this.peerId = peerId;
        this.msgType = msgType;
        this.content = content;
        this.quoteMsgId = quoteMsgId;
        this.recallFlag = recallFlag;
        this.createdTime = TimeUtils.currentTime();
    }

}
