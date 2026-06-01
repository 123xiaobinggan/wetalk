package com.wetalk.business.message.service;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import com.alibaba.fastjson2.JSON;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.wetalk.utils.*;
import com.wetalk.mapper.*;
import com.wetalk.model.*;
import com.wetalk.netty.constant.ChannelAttrKey;
import com.wetalk.netty.session.SessionManager;

import com.wetalk.ws.Type.Chat.Content.*;
import com.wetalk.ws.Type.Chat.Send.*;
import com.wetalk.ws.Type.Friend.RequestAccept.*;
import com.wetalk.ws.Type.Friend.RequestSend.*;
import com.wetalk.ws.protocol.WsType;
import com.wetalk.ws.protocol.WsIncomeMessage;
import com.wetalk.ws.protocol.event.*;
import com.wetalk.ws.protocol.WsResult;
import com.wetalk.ws.Type.Conn.*;
import com.wetalk.ws.Type.Call.*;
import com.wetalk.vo.HttpResult;
import com.wetalk.business.message.dto.*;
import com.wetalk.business.message.vo.*;
import com.wetalk.mq.protocol.ChatPush;
import com.wetalk.mq.protocol.ChatPushUnit;
import com.wetalk.mq.producer.ChatPushProducer;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class MessageService {
    private final ChatPushProducer chatPushProducer;
    private final SessionManager sessionManager;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final GroupMapper groupMapper;
    private final FriendMapper friendMapper;
    private final UserMapper userMapper;
    private final Cache cache;
    private final ObjectMapper objectMapper;

    // fetchMessages
    public HttpResult<Object> fetchMessages(MessagesFetchDTO dto) {
        System.out.println("getMessages: " + dto);
        Long myUserId = dto.getMyUserId();
        Long sessionId = dto.getSessionId();
        Long msgId = dto.getMsgId();
        try {
            Message[] messages = messageMapper.fetchMessages(myUserId, sessionId, msgId);
            Arrays.sort(messages, (a, b) -> a.getMsgId().compareTo(b.getMsgId()));
            return HttpResult.success(0, "获取消息成功", new MessagesFetchVO(messages));
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "获取消息失败", null);
        }
    }

    // fetchMessagesByParams
    public HttpResult<Object> fetchMessagesByParams(MessagesFetchByParamsDTO dto) {
        System.out.println("fetchMessagesByParams" + dto);
        Long convId = dto.getConvId();
        Long sessionId = dto.getSessionId();
        Long senderId = dto.getSenderId();
        String keyword = dto.getKeyword();
        Integer[] msgTypes = dto.getMsgTypes();
        LocalDate date = dto.getDate();
        Long msgId = dto.getMsgId();
        try {
            LocalDateTime dateStart = null, dateEnd = null;
            if (date != null) {
                dateStart = date.atStartOfDay();
                dateEnd = date.plusDays(1).atStartOfDay();
            }
            Message[] messages = messageMapper.fetchMessagesByParams(convId, sessionId,
                    senderId, keyword, msgTypes, dateStart, dateEnd, msgId);
            System.out.println("messagesfetch: " + Arrays.toString(messages));
            Arrays.sort(messages, (a, b) -> a.getMsgId().compareTo(b.getMsgId()));
            return HttpResult.success(0, "消息获取成功", new MessagesFetchVO(messages));
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "获取消息失败", null);
        }
    }

    public HttpResult<Object> handleDeleteMessage(MessageDeleteDTO dto) {
        Long msgId = dto.getMsgId();
        Long userId = dto.getUserId();
        System.out.println("deleteMessage: " + dto);
        if (msgId == null || userId == null) {
            return HttpResult.error(1, "删除失败", null);
        }
        try {
            messageMapper.deleteMessage(msgId, userId);
            return HttpResult.success(0, "删除消息成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "删除失败", null);
        }
    }

    public void handleCallForward(ChannelHandlerContext ctx, WsIncomeMessage<?> wsIncomeMessage, String event,
            String msg) {
        CallReq callReq = JSON.parseObject(
                JSON.toJSONString(wsIncomeMessage.getData()),
                CallReq.class);
        Channel channel = ctx.channel();
        Channel targetChannel = sessionManager.getChannel(callReq.getToUserId());
        Long userId = channel.attr(ChannelAttrKey.USER_ID).get();
        Long targetUserId = targetChannel.attr(ChannelAttrKey.USER_ID).get();
        Long sessionId = callReq.getSessionId();
        if (targetChannel == null) {
            List<ChatPushUnit> chatPushUnit = List.of(new ChatPushUnit(userId, sessionId, new WsResult<>(WsType.CALL, CallEvent.AUDIO_REJECT, 1, "对方不在线",
                            wsIncomeMessage.getClientMsgId(), callReq)));
            chatPushProducer.pushChatMessage(new ChatPush(chatPushUnit));
            return;
        }
        List<ChatPushUnit> chatPushUnit = List.of(new ChatPushUnit(targetUserId, sessionId, new WsResult<>(WsType.CALL, event,
                        0, msg, wsIncomeMessage.getClientMsgId(),
                        callReq)));
        chatPushProducer.pushChatMessage(new ChatPush(chatPushUnit));
        
    }

    
    }
