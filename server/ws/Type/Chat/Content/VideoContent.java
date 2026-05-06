package com.wetalk.ws.Type.Chat.Content;

import lombok.Data;

@Data
public class VideoContent {
    private String url;
    private int duration;
    private int width;
    private int height;
    private int size;
    private String cover;

    public VideoContent() {
    }

    public VideoContent(String url, int duration, int width, int height, int size, String cover) {
        this.url = url;
        this.duration = duration;
        this.width = width;
        this.height = height;
        this.size = size;
        this.cover = cover;
    }

}
