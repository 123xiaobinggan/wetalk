package com.wetalk.ws.Type.Chat.Content;

import lombok.Data;

@Data
public class TextContent {
    private String text;

    public TextContent() {
    }

    public TextContent(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
