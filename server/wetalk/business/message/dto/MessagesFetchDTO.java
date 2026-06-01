package com.wetalk.business.message.dto;

import lombok.Data;

@Data
public class MessagesFetchDTO {
    private Long myUserId;
    private Long sessionId;
    private Long msgId;

    public MessagesFetchDTO() {
    }

    public MessagesFetchDTO(Long myUserId, String sessionId, Long msgId) {
        this.myUserId = myUserId;
        this.sessionId = Long.valueOf(sessionId);
        this.msgId = msgId;
    }

}
