package com.wetalk.mq.protocol;

import com.wetalk.ws.protocol.WsResult;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ChatPushUnit {
    private Long userId;
    private Long sessionId;
    private WsResult<?> wsResult;

    public ChatPushUnit(Long userId, Long sessionId, WsResult<?> wsResult){
        this.userId = userId;
        this.sessionId = sessionId;
        this.wsResult = wsResult;
    }

    public ChatPushUnit(Long userId, String sessionId, WsResult<?> wsResult){
        this.userId = userId;
        this.sessionId = Long.valueOf(sessionId);
        this.wsResult = wsResult;
    }
}
