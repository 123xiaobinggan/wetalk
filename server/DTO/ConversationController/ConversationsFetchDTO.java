package com.wetalk.DTO.ConversationController;

import lombok.Data;

@Data
public class ConversationsFetchDTO {
    private Long userId;

    public ConversationsFetchDTO(){}

    public ConversationsFetchDTO(Long userId){
        this.userId = userId;
    }

    public Long getUserId(){
        return userId;
    }
}
