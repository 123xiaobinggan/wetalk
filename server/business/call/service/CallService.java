package com.wetalk.business.call.service;

import lombok.RequiredArgsConstructor;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import org.springframework.stereotype.Service;

import com.wetalk.business.message.vo.*;
import com.wetalk.mapper.FriendMapper;
import com.wetalk.mapper.MessageMapper;
import com.wetalk.mapper.UserMapper;
import com.wetalk.model.Friendship;
import com.wetalk.model.Message;
import com.wetalk.model.User;
import com.wetalk.utils.Cache;
import com.wetalk.utils.WsSender;
import com.wetalk.ws.Type.Chat.Content.TextContent;
import com.wetalk.ws.protocol.WsResult;
import com.wetalk.ws.Type.Call.CallReq;
import com.wetalk.ws.protocol.WsType;
import com.wetalk.ws.protocol.event.CallEvent;

import com.wetalk.netty.session.SessionManager;

import com.wetalk.mq.protocol.ChatPush;
import com.wetalk.mq.protocol.ChatPushUnit;
import com.wetalk.mq.event.WsEvent;
import com.wetalk.mq.producer.ChatPushProducer;

@RequiredArgsConstructor
@Service
public class CallService {
    private final SessionManager sessionManager;
    private final Cache cache;
    private final UserMapper userMapper;
    private final FriendMapper friendMapper;
    private final MessageMapper messageMapper;
    private final ChatPushProducer chatPushProducer;
    private final ObjectMapper objectMapper;

    public void handleCallInvite(WsEvent wsEvent) {
        CallReq callReq = JSON.parseObject(
                JSON.toJSONString(wsEvent.getPayload()),
                CallReq.class);

        System.out.println("callInvite: " + callReq);
        if (callReq == null
                || callReq.getCallId() == null
                || callReq.getFromUserId() == null
                || callReq.getToUserId() == null
                || callReq.getCallType() == null
                || callReq.getSessionId() == null
                || callReq.getP2pData() == null)
            return;
        Long userId = wsEvent.getFromUserId();
        Long sessionId = callReq.getSessionId();
        Friendship friendship = friendMapper.getFriendship(callReq.getToUserId(), callReq.getFromUserId());
        User user = userMapper.getUserByUserId(callReq.getFromUserId());

        callReq.setRemark(friendship.getRemark());
        callReq.setUsername(user.getUsername());
        callReq.setAvatar(user.getAvatar());
        Channel channel = sessionManager.getChannel(userId);
        Channel targetChannel = sessionManager.getChannel(callReq.getToUserId());

        if (targetChannel == null) {
            List<ChatPushUnit> chatPushUnits = List.of(
                    new ChatPushUnit(userId, sessionId, new WsResult<>(WsType.CALL, CallEvent.AUDIO_REJECT, 1, "对方不在线",
                            callReq.getClientMsgId(), callReq)));
            chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));
            return;
        }

        TextContent textContent = new TextContent("[发起通话] 00:00");

        Message message = new Message(
                callReq.getConvType(),
                callReq.getSessionId(),
                callReq.getFromUserId(),
                callReq.getToUserId(),
                1,
                JSON.toJSONString(textContent),
                null,
                false);

        messageMapper.insertMessage(message);

        cache.setCallStart(callReq.getCallId(), LocalDateTime.now());

        cache.setCallMsgId(callReq.getCallId(), message.getMsgId());

        List<ChatPushUnit> chatPushUnits = List.of(new ChatPushUnit(callReq.getToUserId(), sessionId,
                new WsResult<>(WsType.CALL, CallEvent.AUDIO_CALL,
                        0, "发起语音通话",
                        callReq.getClientMsgId(),
                        callReq)));
        chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));
    }

    public void handleCallEnd(WsEvent wsEvent) {
        CallReq callReq = JSON.parseObject(
                JSON.toJSONString(wsEvent.getPayload()),
                CallReq.class);
        Long userId = wsEvent.getFromUserId();
        Long sessionId = callReq.getSessionId();
        Channel channel = sessionManager.getChannel(userId);
        try {
            LocalDateTime startTime = cache.getCallStart(callReq.getCallId());
            Long msgId = cache.getCallMsgId(callReq.getCallId());
            System.out.println("callEnd: " + callReq + startTime + msgId);
            Message message = null;
            if (startTime != null && msgId != null) {
                Duration duration = Duration.between(startTime, LocalDateTime.now());

                String durationText = formatDuration(duration);

                TextContent textContent = new TextContent("[语音通话] " + durationText);

                messageMapper.updateMessageContent(
                        msgId,
                        JSON.toJSONString(textContent));
                message = messageMapper.getMessage(msgId);
            }
            TextContent textContent = null;
            if (message != null) {
                textContent = objectMapper.readValue(message.getContent(), TextContent.class);
            }

            Channel targetChannel = sessionManager.getChannel(callReq.getToUserId());
            MessageVO messageVO = new MessageVO(message.getMsgId(), message.getSessionId(), message.getConvType(),
                    message.getSenderId(), message.getPeerId(), message.getMsgType(),
                    textContent, message.getQuoteMsgId(),
                    message.getRecallFlag(), message.getCreatedTime());
            if (targetChannel != null) {
                List<ChatPushUnit> chatPushUnits = List.of(new ChatPushUnit(callReq.getToUserId(), sessionId,
                        new WsResult<>(WsType.CALL, CallEvent.AUDIO_END,
                                0, "通话结束", null,
                                messageVO)));
                chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));

            }

            if (textContent != null) {
                List<ChatPushUnit> chatPushUnits = List.of(new ChatPushUnit(userId, sessionId,
                        new WsResult<>(WsType.CALL, CallEvent.AUDIO_END,
                                0, "通话结束", null,
                                messageVO)));
                chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));
            }
            cache.deleteCallStart(callReq.getCallId());
            cache.deleteCallMsgId(callReq.getCallId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天:");
        }
        if (hours > 0 || sb.length() > 0) {
            sb.append(String.format("%02d小时:", hours));
        }
        if (minutes > 0 || sb.length() > 0) {
            sb.append(String.format("%02d分钟:", minutes));
        }
        sb.append(String.format("%02d秒", seconds));

        return sb.toString();
    }

}
