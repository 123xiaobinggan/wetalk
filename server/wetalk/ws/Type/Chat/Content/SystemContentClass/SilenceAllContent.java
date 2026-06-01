package com.wetalk.ws.Type.Chat.Content.SystemContentClass;

import lombok.Data;

@Data
public class SilenceAllContent {
    private Long userId;

    public SilenceAllContent(Long userId) {
        this.userId = userId;
    }
}
