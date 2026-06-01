package com.wetalk.business.chatPush.service;

import lombok.RequiredArgsConstructor;

import com.wetalk.utils.WsSender;
import com.wetalk.netty.session.SessionManager;
import com.wetalk.mq.protocol.ChatPush;
import com.wetalk.mq.protocol.ChatPushUnit;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ChatPushService{
    private final SessionManager sessionManager;

    public void chatPush(ChatPush chatPush){
        for(ChatPushUnit chatPushUnit:chatPush.getChatPushUnits()){
            Channel channel = sessionManager.getChannel(chatPushUnit.getUserId());
            if(channel != null){
                WsSender.send(channel, chatPushUnit.getWsResult());
            }
        }
    }
}
