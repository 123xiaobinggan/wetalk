package com.wetalk.ws.Type.Chat.Content.SystemContentClass;

import lombok.Data;

@Data
public class UnSilenceAllContent {
    private Long userId;

    public UnSilenceAllContent(Long userId){
        this.userId = userId;
    }
}
