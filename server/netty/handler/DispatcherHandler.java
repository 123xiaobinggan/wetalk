package com.wetalk.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import com.wetalk.service.MessageService;
import io.netty.channel.ChannelHandler;

import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import com.wetalk.service.*;
import com.wetalk.ws.Type.Chat.Send.*;
import com.wetalk.ws.protocol.event.*;
import com.wetalk.ws.protocol.WsType;
import com.wetalk.ws.protocol.WsIncomeMessage;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
@ChannelHandler.Sharable
public class DispatcherHandler extends ChannelInboundHandlerAdapter {

    private final MessageService messageService;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("DispatcherHandler: 有客户端连接：" + ctx.channel().id().asShortText());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("DispatcherHandler: 客户端断开：" + ctx.channel().id().asShortText());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        try {
            if (message instanceof TextWebSocketFrame) {
                TextWebSocketFrame msg = (TextWebSocketFrame) message;
                String json = msg.text();
                WsIncomeMessage<Object> wsIncomeMessage = JSON.parseObject(json,
                        new TypeReference<WsIncomeMessage<Object>>() {
                        });

                if (wsIncomeMessage == null) {
                    System.out.println("DispatcherHandler: wsIncomeMessage 解析失败");
                    return;
                }

                String type = wsIncomeMessage.getType();
                String event = wsIncomeMessage.getEvent();
                if (type == null || type.isBlank() || event == null || event.isBlank()) {
                    System.out.println("DispatcherHandler: type 或 event 为空");
                    return;
                }

                dispatchMsg(ctx, type, event, wsIncomeMessage);
            } else {
                ctx.fireChannelRead(message);
            }
        } finally {
            ReferenceCountUtil.release(message);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void dispatchMsg(ChannelHandlerContext ctx, String type, String event,
            WsIncomeMessage<Object> wsIncomeMessage) {
        switch (type) {
            case WsType.CHAT:
                dispatchChat(ctx, event, wsIncomeMessage);
                break;
            case WsType.FRIEND:
                dispatchFriend(ctx, event, wsIncomeMessage);
                break;
            case WsType.CONN:
                dispatchConn(ctx, event, wsIncomeMessage);
                break;
            case WsType.GROUP:
                dispatchGroup(ctx, event, wsIncomeMessage);
                break;
            case WsType.CALL:
                dispatchCall(ctx, event, wsIncomeMessage);
                break;
            case WsType.SYSTEM:
                dispatchSystem(ctx, event, wsIncomeMessage);
                break;
            default:
                System.out.println("未知 type: " + type);
        }

    }

    private void dispatchChat(ChannelHandlerContext ctx, String event, WsIncomeMessage<Object> wsIncomeMessage) {
        Object data = wsIncomeMessage.getData();
        switch (event) {
            case ChatEvent.SEND:
                wsIncomeMessage.setData(JSON.parseObject(
                        JSON.toJSONString(data),
                        SendReq.class));
                messageService.handleSendMessage(ctx, wsIncomeMessage);
                break;
            case ChatEvent.RECALL:
                wsIncomeMessage.setData(JSON.parseObject(
                        JSON.toJSONString(data),
                        RecallReq.class));
                messageService.handleRecallMessage(ctx, wsIncomeMessage);
                break;
            default:
                System.out.println("未知 chat event: " + event);
        }
    }

    private void dispatchFriend(ChannelHandlerContext ctx, String event, WsIncomeMessage<Object> wsIncomMessage) {
    }

    private void dispatchCall(ChannelHandlerContext ctx, String event, WsIncomeMessage<Object> wsIncomeMessage) {
        switch (event) {
            case "audio_call":
                messageService.handleCallInvite(ctx, wsIncomeMessage);
                break;
            case "audio_reject":
                messageService.handleCallForward(ctx, wsIncomeMessage, "audio_reject", "对方已拒绝");
                break;
            case "audio_accept":
                messageService.handleCallForward(ctx, wsIncomeMessage, "audio_accept", "对方已接受");
                break;
            case "audio_candidate":
                messageService.handleCallForward(ctx, wsIncomeMessage, "audio_candidate", null);
                break;
            case "audio_busy":
                messageService.handleCallForward(ctx, wsIncomeMessage, "audio_busy", "对方忙线中");
                break;
            case "audio_end":
                messageService.handleCallEnd(ctx, wsIncomeMessage);
        }

    }

    private void dispatchConn(ChannelHandlerContext ctx, String event, WsIncomeMessage<Object> wsIncomeMessage) {
    }

    private void dispatchGroup(ChannelHandlerContext ctx, String event, WsIncomeMessage<Object> wsIncomeMessage) {
    }

    private void dispatchSystem(ChannelHandlerContext ctx, String event, WsIncomeMessage<Object> wsIncomeMessage) {
    }
}