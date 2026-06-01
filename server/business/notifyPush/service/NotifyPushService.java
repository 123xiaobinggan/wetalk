package com.wetalk.business.notifyPush.service;

import lombok.RequiredArgsConstructor;

import com.wetalk.utils.WsSender;
import com.wetalk.ws.protocol.WsResult;
import com.wetalk.netty.session.SessionManager;
import com.wetalk.mq.protocol.NotifyPush;
import com.wetalk.mq.protocol.NotifyPushUnit;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class NotifyPushService {
    private final SessionManager sessionManager;

    public void notifyPush(NotifyPush notifyPush){
        for(NotifyPushUnit unit:notifyPush.getNotifyPushUnits()){
            Channel channel = sessionManager.getChannel(unit.getUserId());
            if(channel != null){
                WsSender.send(channel, unit.getWsResult());
            }
        }
    }
}
