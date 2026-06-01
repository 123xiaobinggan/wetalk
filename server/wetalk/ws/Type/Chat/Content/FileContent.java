package com.wetalk.ws.Type.Chat.Content;

import lombok.Data;

@Data
public class FileContent {
    private String fileName;
    private String url;
    private int size;
    private String ext;

    public FileContent() {
    }

    public FileContent(String fileName, String url, int size, String ext) {
        this.fileName = fileName;
        this.url = url;
        this.size = size;
        this.ext = ext;
    }
}
