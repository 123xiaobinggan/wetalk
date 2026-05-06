package com.wetalk.DTO.MessageController;

import java.time.LocalDate;

import lombok.Data;

@Data
public class MessagesFetchByParamsDTO {
    private Long convId;
    private Long sessionId;
    private Long senderId;
    private String keyword;
    private LocalDate date;
    private Integer[] msgTypes;
    private Long msgId;

    public MessagesFetchByParamsDTO(Long convId, Long sessionId,
            Long senderId, String keyword, LocalDate date, Integer[] msgTypes,
            Long msgId) {
        this.convId = convId;
        this.sessionId = sessionId;
        this.senderId = senderId;
        this.keyword = keyword;
        this.date = date;
        this.msgTypes = msgTypes;
        this.msgId = msgId;
    }
}
