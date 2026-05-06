package com.wetalk.model;

import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class Conversation {
    private Long userId;
    private Long convId;
    private Long sessionId;
    private Boolean convType;
    private Long peerId;
    private Long groupId;
    private String groupRemark;
    private Boolean pinned;
    private Boolean muted;
    private Integer unreadCnt;
    private Long lastMsgId;
    private String lastMsgBrief;
    private Long lastMsgSenderId;
    private LocalDateTime lastMsgTime;
    private LocalDateTime lastClearedTime;
    private Boolean deleted;

    public Conversation() {
    }

    public Conversation(Long userId, Long sessionId,
            Boolean convType, Long peerId,
            Long groupId, String groupRemark, Integer unreadCnt
            ) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.convType = convType;
        this.peerId = peerId;
        this.groupId = groupId;
        this.groupRemark = groupRemark;
        this.unreadCnt = unreadCnt;
    }

    public String getSessionId() {
        return sessionId == null ? null : sessionId.toString();
    }

}
