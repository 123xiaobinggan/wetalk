package com.wetalk.ws.Type.Chat.Content.SystemContentClass;

import lombok.Data;

@Data
public class GroupCreateContent {
    private Long ownerUserId;

    public GroupCreateContent() {
    }

    public GroupCreateContent(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }
}
