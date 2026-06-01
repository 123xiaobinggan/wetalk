package com.wetalk.ws.protocol.event;

public final class ChatEvent {
    public static final String SEND = "send";
    public static final String RECEIVE = "receive";
    public static final String RECALL = "recall";
    public static final String ACK = "ack";
    public static final String FAILED = "failed";

    private ChatEvent() {
    }
}