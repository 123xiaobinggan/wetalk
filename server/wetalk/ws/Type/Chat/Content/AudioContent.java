package com.wetalk.ws.Type.Chat.Content;

import lombok.Data;

@Data
public class AudioContent {
    private String url;
    private int duration;
    private int size;
    private String format;

    public AudioContent() {
    }

    public AudioContent(String url, int duration, int size, String format) {
        this.url = url;
        this.duration = duration;
        this.size = size;
        this.format = format;
    }

}
