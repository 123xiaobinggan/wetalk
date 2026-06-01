package com.wetalk.business.message.dto;

import lombok.Data;

@Data
public class MessageDeleteDTO {
    private Long msgId;
    private Long userId;

    public MessageDeleteDTO() {
    }

    public MessageDeleteDTO(Long msgId, Long userId) {
        this.msgId = msgId;
        this.userId = userId;
    }
}
