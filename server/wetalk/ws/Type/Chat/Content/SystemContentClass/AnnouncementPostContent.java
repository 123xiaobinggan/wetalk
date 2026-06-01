package com.wetalk.ws.Type.Chat.Content.SystemContentClass;

import lombok.Data;

@Data
public class AnnouncementPostContent {
    private Long posterUserId;
    private String announcement;

    public AnnouncementPostContent() {
    }

    public AnnouncementPostContent(Long posterUserId, String announcement) {
        this.posterUserId = posterUserId;
        this.announcement = announcement;
    }
}
