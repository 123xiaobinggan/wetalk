package com.wetalk.mq.protocol;

import com.wetalk.ws.protocol.WsResult;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class NotifyPushUnit {
    private Long userId;
    private WsResult<?> wsResult;

    public NotifyPushUnit(Long userId, WsResult<?> wsResult) {
        this.userId = userId;
        this.wsResult = wsResult;
    }
    
}
