package com.wetalk.DTO.ConversationController;

import lombok.Data;

@Data
public class ConversationMarkReadDTO {
    private Long convId;

    public ConversationMarkReadDTO(Long convId){
        this.convId = convId;
    }

    public Long getConvId(){
        return convId;
    }

    public void setConvId(Long convId){
        this.convId = convId;
    }
}
