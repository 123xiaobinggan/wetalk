package com.wetalk.utils;

import com.alibaba.fastjson2.JSON;
import com.wetalk.ws.protocol.*;
import com.wetalk.ws.protocol.WsResult;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class WsSender {

    public static void send(Channel channel, WsResult<?> wsIncomeMessage) {
        if (channel == null || !channel.isActive()) {
            return;
        }

        String json = JSON.toJSONString(wsIncomeMessage);
        System.out.println("WsSender: " + json);
        channel.writeAndFlush(new TextWebSocketFrame(json));
    }

}