package com.wetalk.ws.Type.Chat.Content;

import lombok.Data;

@Data
public class SystemContent<T> {
    private String systemType;
    private T data;

    public SystemContent() {
    }

    public SystemContent(String systemType, T data) {
        this.systemType = systemType;
        this.data = data;
    }
}
