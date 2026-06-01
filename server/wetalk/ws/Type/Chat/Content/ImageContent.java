package com.wetalk.ws.Type.Chat.Content;

import lombok.Data;

@Data
public class ImageContent {
    private String url;
    private int width;
    private int height;
    private int size;
    private String format;

    public ImageContent() {
    }

    public ImageContent(String url, int width, int height, int size, String format) {
        this.url = url;
        this.width = width;
        this.height = height;
        this.size = size;
        this.format = format;
    }

}
