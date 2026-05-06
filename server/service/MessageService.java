package com.wetalk.service;

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
import com.wetalk.VO.HttpResult;
import com.wetalk.DTO.MessageController.*;
import com.wetalk.VO.MessageController.*;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class MessageService {
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

    @Transactional
    public void handleSendMessage(ChannelHandlerContext ctx, WsIncomeMessage<?> wsIncomeMessage) {
        SendReq sendReq = (SendReq) wsIncomeMessage.getData();
        Object parsedContent = messageContentParser(sendReq);
        sendReq.setContent(parsedContent);
        String sessionId = String.valueOf(sendReq.getSessionId());
        String contentJson = JSON.toJSONString(parsedContent);
        Message message = new Message(sendReq.getConvType(), sendReq.getSessionId(), sendReq.getSenderId(),
                sendReq.getPeerId(), sendReq.getMsgType(), contentJson, sendReq.getQuoteMsgId(),
                sendReq.getRecallFlag());
        System.out.println("handleSendMessage: " + message);
        Long senderId = ctx.channel().attr(ChannelAttrKey.USER_ID).get();
        if (!Objects.equals(senderId, sendReq.getSenderId())) {
            WsSender.send(ctx.channel(),
                    new WsResult<>(WsType.CHAT, ChatEvent.FAILED, 1, "消息被篡改", wsIncomeMessage.getClientMsgId(),
                            null));
            return;
        }
        try {
            String msgBrief = buildMsgBrief(parsedContent, message);
            // 私聊
            if (Boolean.FALSE.equals(message.getConvType())) {
                Friendship peerFriendship = friendMapper.getFriendship(message.getPeerId(), message.getSenderId());
                if (peerFriendship == null || Boolean.TRUE.equals(peerFriendship.getDeleted())) {
                    WsSender.send(ctx.channel(),
                            new WsResult<>(WsType.CHAT, ChatEvent.FAILED, 1, "你不是对方好友",
                                    wsIncomeMessage.getClientMsgId(), null));
                    return;
                } else if (Boolean.TRUE.equals(peerFriendship.getBlocked())) {
                    WsSender.send(ctx.channel(),
                            new WsResult<>(WsType.CHAT, ChatEvent.FAILED, 1, "你已被对方拉黑",
                                    wsIncomeMessage.getClientMsgId(), null));
                    return;
                }
                int rows = messageMapper.insertMessage(message);
                if (rows <= 0) {
                    System.out.println("发送信息失败");
                    WsSender.send(ctx.channel(),
                            new WsResult<>(WsType.CHAT, ChatEvent.FAILED, 1, "发送失败",
                                    wsIncomeMessage.getClientMsgId(),
                                    null));
                }
                Long[] userIds = { message.getSenderId(), message.getPeerId() };
                // 更新会话
                Conversation conv = new Conversation();
                conv.setSessionId(message.getSessionId());
                conv.setLastMsgId(message.getMsgId());
                conv.setLastMsgTime(message.getCreatedTime());
                conv.setLastMsgBrief(msgBrief);
                conv.setLastMsgSenderId(senderId);
                conversationMapper.updateConversations(conv, userIds);
                // 更新未读数
                conversationMapper.incrementUnreadCnt(conv, new Long[] { message.getPeerId() });
                // 推送消息 -> peer
                if (sessionManager.isOnline(message.getPeerId())) {
                    Conversation[] convs = conversationMapper.fetchConversationsByUserIdsAndSessionId(
                            new Long[] { message.getPeerId() }, message.getSessionId());
                    Channel peerChannel = sessionManager.getChannel(message.getPeerId());
                    WsSender.send(peerChannel,
                            new WsResult<>(WsType.CHAT, ChatEvent.RECEIVE, 0, "收到消息",
                                    null,
                                    new SendPush(
                                            new MessageVO(message.getMsgId(), sessionId, sendReq.getConvType(),
                                                    sendReq.getSenderId(), null, sendReq.getMsgType(),
                                                    sendReq.getContent(), sendReq.getQuoteMsgId(),
                                                    sendReq.getRecallFlag(),
                                                    message.getCreatedTime()),
                                            convs[0])));
                }
                // 发送成功 -> sender
                WsSender.send(ctx.channel(),
                        new WsResult<>(WsType.CHAT, ChatEvent.ACK, 0, "发送成功", wsIncomeMessage.getClientMsgId(),
                                new AckResp(message.getMsgId(), message.getCreatedTime())));

            } else {
                Long[] userIds = cache.getGroupMembersOrLoad(sendReq.getSessionId(), () -> {
                    GroupMember[] groupMembers = groupMapper.fetchGroupMembers(sendReq.getSessionId());
                    return Arrays.stream(groupMembers).map(GroupMember::getMemberUserId)
                            .toArray(Long[]::new);
                });
                // sender 不是群成员
                if (Arrays.stream(userIds).noneMatch(userId -> userId.equals(senderId))) {
                    WsSender.send(ctx.channel(),
                            new WsResult<>(WsType.CHAT, ChatEvent.FAILED, 1, "你不是群成员",
                                    wsIncomeMessage.getClientMsgId(),
                                    null));
                    return;
                }
                // 插入消息
                int rows = messageMapper.insertMessage(message);
                if (rows <= 0) {
                    System.out.println("发送信息失败");
                    WsSender.send(ctx.channel(),
                            new WsResult<>(WsType.CHAT, ChatEvent.FAILED, 1, "发送失败",
                                    wsIncomeMessage.getClientMsgId(),
                                    null));
                }
                // 更新会话
                Conversation conv = new Conversation();
                conv.setSessionId(message.getSessionId());
                conv.setLastMsgId(message.getMsgId());
                conv.setLastMsgTime(message.getCreatedTime());
                conv.setLastMsgBrief(msgBrief);
                conv.setLastMsgSenderId(senderId);
                conversationMapper.updateConversations(conv, userIds);
                // 去除发送者
                userIds = Arrays.stream(userIds).filter(userId -> !userId.equals(senderId))
                        .toArray(Long[]::new);
                conversationMapper.incrementUnreadCnt(conv, userIds);
                Conversation[] convs = conversationMapper.fetchConversationsByUserIdsAndSessionId(userIds,
                        message.getSessionId());
                // 推送
                for (Conversation c : convs) {
                    if (sessionManager.isOnline(c.getUserId())) {
                        Channel memberChannel = sessionManager.getChannel(c.getUserId());
                        if (memberChannel != null) {
                            WsSender.send(memberChannel, new WsResult<>(WsType.CHAT, ChatEvent.RECEIVE, 0,
                                    "收到消息", null,
                                    new SendPush(new MessageVO(message.getMsgId(), sessionId,
                                            sendReq.getConvType(), sendReq.getSenderId(),
                                            null, sendReq.getMsgType(), sendReq.getContent(),
                                            sendReq.getQuoteMsgId(), sendReq.getRecallFlag(),
                                            message.getCreatedTime()),
                                            c)));
                        }
                    }
                }
                // 推送成功 -> sender
                WsSender.send(ctx.channel(),
                        new WsResult<>(WsType.CHAT, ChatEvent.ACK, 0, "发送成功", wsIncomeMessage.getClientMsgId(),
                                new AckResp(message.getMsgId(), message.getCreatedTime())));

            }
        } catch (Exception e) {
            e.printStackTrace();
            WsSender.send(ctx.channel(),
                    new WsResult<>(WsType.CHAT, ChatEvent.FAILED, 1, "发送失败", wsIncomeMessage.getClientMsgId(),
                            null));
            throw new RuntimeException(e);
        }
    }

    public void handleRecallMessage(ChannelHandlerContext ctx, WsIncomeMessage<?> wsIncomeMessage) {
        RecallReq recallReq = (RecallReq) wsIncomeMessage.getData();
        Long msgId = recallReq.getMsgId();
        Long sessionId = recallReq.getSessionId();
        Long peerId = recallReq.getPeerId();
        Boolean convType = recallReq.getConvType();
        System.out.println("recallMessage: " + recallReq);
        if (msgId == null || sessionId == null || convType == null
                || (Boolean.FALSE.equals(convType) && peerId == null)) {
            return;
        }
        try {
            messageMapper.recallMessage(msgId);
            if (Boolean.FALSE.equals(convType)) {
                Channel channel = sessionManager.getChannel(peerId);
                WsSender.send(channel,
                        new WsResult(WsType.CHAT, ChatEvent.RECALL, 0, "消息撤回", null,
                                new SendPush(new MessageVO(msgId, sessionId, recallReq.getConvType(),
                                        null, null, null,
                                        null, null,
                                        true,
                                        null), null)));
                WsSender.send(ctx.channel(),
                        new WsResult(WsType.CHAT, ChatEvent.RECALL, 0, "消息撤回", null,
                                new SendPush(new MessageVO(msgId, sessionId, recallReq.getConvType(),
                                        null, null, null,
                                        null, null,
                                        true,
                                        null), null)));
            } else {
                Long[] groupMemberUserIds = cache.getGroupMembersOrLoad(sessionId, () -> {
                    GroupMember[] groupMembers = groupMapper.fetchGroupMembers(sessionId);
                    return Arrays.stream(groupMembers).map(GroupMember::getMemberUserId)
                            .toArray(Long[]::new);
                });
                for (Long memberUserId : groupMemberUserIds) {
                    Channel channel = sessionManager.getChannel(memberUserId);
                    WsSender.send(channel,
                            new WsResult(WsType.CHAT, ChatEvent.RECALL, 0, "消息撤回", null,
                                    new SendPush(new MessageVO(msgId, sessionId, recallReq.getConvType(),
                                            null, null, null,
                                            null, null,
                                            true,
                                            null), null)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    public void handleCallInvite(ChannelHandlerContext ctx, WsIncomeMessage<?> wsIncomeMessage) {
        CallReq callReq = JSON.parseObject(
                JSON.toJSONString(wsIncomeMessage.getData()),
                CallReq.class);

        System.out.println("callInvite: " + callReq);

        if (callReq == null ||
                callReq.getCallId() == null ||
                callReq.getFromUserId() == null ||
                callReq.getToUserId() == null ||
                callReq.getCallType() == null ||
                callReq.getSessionId() == null ||
                callReq.getP2pData() == null) {
            return;
        }

        Friendship friendship = friendMapper.getFriendship(callReq.getToUserId(), callReq.getFromUserId());
        User user = userMapper.getUserByUserId(callReq.getFromUserId());

        callReq.setRemark(friendship.getRemark());
        callReq.setUsername(user.getUsername());
        callReq.setAvatar(user.getAvatar());

        Channel targetChannel = sessionManager.getChannel(callReq.getToUserId());

        if (targetChannel == null) {
            WsSender.send(
                    ctx.channel(),
                    new WsResult<>(WsType.CALL, CallEvent.AUDIO_REJECT, 1, "对方不在线",
                            wsIncomeMessage.getClientMsgId(), callReq));
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

        WsSender.send(
                targetChannel,
                new WsResult<>(WsType.CALL, CallEvent.AUDIO_CALL,
                        0, "发起语音通话",
                        wsIncomeMessage.getClientMsgId(),
                        callReq));
    }

    public void handleCallForward(ChannelHandlerContext ctx, WsIncomeMessage<?> wsIncomeMessage, String event,
            String msg) {
        CallReq callReq = JSON.parseObject(
                JSON.toJSONString(wsIncomeMessage.getData()),
                CallReq.class);

        Channel targetChannel = sessionManager.getChannel(callReq.getToUserId());

        if (targetChannel == null) {
            WsSender.send(
                    ctx.channel(),
                    new WsResult<>(WsType.CALL, CallEvent.AUDIO_REJECT, 1, "对方不在线",
                            wsIncomeMessage.getClientMsgId(), callReq));
            return;
        }

        WsSender.send(
                targetChannel,
                new WsResult<>(WsType.CALL, event,
                        0, msg, wsIncomeMessage.getClientMsgId(),
                        callReq));
    }

    public void handleCallEnd(ChannelHandlerContext ctx, WsIncomeMessage<?> wsIncomeMessage) {
        CallReq callReq = JSON.parseObject(
                JSON.toJSONString(wsIncomeMessage.getData()),
                CallReq.class);

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
                WsSender.send(
                        targetChannel,
                        new WsResult<>(WsType.CALL, CallEvent.AUDIO_END,
                                0, "通话结束", null,
                                messageVO));

            }

            if (textContent != null) {
                WsSender.send(
                        ctx.channel(),
                        new WsResult<>(WsType.CALL, CallEvent.AUDIO_END,
                                0, "通话结束", null,
                                messageVO));
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

    private String buildMsgBrief(Object parsedContent, Message message) {
        try {
            switch (message.getMsgType()) {
                case 1:
                    TextContent textContent = (TextContent) parsedContent;
                    String text = textContent.getText();
                    return text.length() > 20 ? text.substring(0, 20) : text;
                case 2:
                    return "[语音]";
                case 3:
                    return "[图片]";
                case 4:
                    return "[视频]";
                case 5:
                    FileContent fileContent = (FileContent) parsedContent;
                    String fileName = fileContent.getFileName();
                    String ext = fileContent.getExt();
                    return fileName + ext;
                default:
                    return "[消息]";
            }
        } catch (Exception e) {
            return "[消息]";
        }
    }

    private Object messageContentParser(SendReq sendReq) {
        Integer msgType = sendReq.getMsgType();
        Object rawContent = sendReq.getContent();

        switch (msgType) {
            case 1:
                return JSON.parseObject(JSON.toJSONString(rawContent), TextContent.class);
            case 2:
                return JSON.parseObject(JSON.toJSONString(rawContent), AudioContent.class);
            case 3:
                return JSON.parseObject(JSON.toJSONString(rawContent), ImageContent.class);
            case 4:
                return JSON.parseObject(JSON.toJSONString(rawContent), VideoContent.class);
            case 5:
                return JSON.parseObject(JSON.toJSONString(rawContent), FileContent.class);
            default:
                return rawContent;
        }
    }
}
