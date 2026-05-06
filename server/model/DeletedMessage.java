package com.wetalk.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DeletedMessage {
    private Long msgId;
    private Long userId;
    private LocalDateTime createdTime;

    public DeletedMessage(){}

    public DeletedMessage(Long msgId, Long userId, LocalDateTime createdTime){
        this.msgId = msgId;
        this.userId = userId;
        this.createdTime = createdTime;
    }
}
