package com.wetalk.business.chat.service;

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
import com.wetalk.mq.producer.ChatPushProducer;
import com.wetalk.mq.protocol.ChatPushUnit;
import com.wetalk.mq.protocol.ChatPush;
import com.wetalk.mq.event.WsEvent;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ChatMessageService {
        private final SessionManager sessionManager;
        private final ChatPushProducer chatPushProducer;
        private final ConversationMapper conversationMapper;
        private final MessageMapper messageMapper;
        private final GroupMapper groupMapper;
        private final FriendMapper friendMapper;
        private final UserMapper userMapper;
        private final Cache cache;
        private final ObjectMapper objectMapper;

        @Transactional
        public void handleSendMessage(WsEvent wsEvent) {
                SendReq sendReq = JSON.parseObject(wsEvent.getPayload(), SendReq.class);
                Object parsedContent = messageContentParser(sendReq);
                sendReq.setContent(parsedContent);
                String sessionId = String.valueOf(sendReq.getSessionId());
                String contentJson = JSON.toJSONString(parsedContent);
                Message message = new Message(sendReq.getConvType(), sendReq.getSessionId(), sendReq.getSenderId(),
                                sendReq.getPeerId(), sendReq.getMsgType(), contentJson, sendReq.getQuoteMsgId(),
                                sendReq.getRecallFlag());
                System.out.println("handleSendMessage: " + message);
                Long senderId = wsEvent.getFromUserId();
                if (!Objects.equals(senderId, sendReq.getSenderId())) {
                        List<ChatPushUnit> chatPushUnits = List.of(new ChatPushUnit(senderId, sessionId,
                                        new WsResult<>(WsType.CHAT, ChatEvent.FAILED, 1, "消息被篡改",
                                                        sendReq.getClientMsgId(),
                                                        null)));
                        chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));
                        return;
                }
                try {
                        String msgBrief = buildMsgBrief(parsedContent, message);
                        // 私聊
                        if (Boolean.FALSE.equals(message.getConvType())) {
                                Friendship peerFriendship = friendMapper.getFriendship(message.getPeerId(),
                                                message.getSenderId());
                                if (peerFriendship == null || Boolean.TRUE.equals(peerFriendship.getDeleted())) {
                                        List<ChatPushUnit> chatPushUnits = List.of(new ChatPushUnit(senderId, sessionId,
                                                        new WsResult<>(WsType.CHAT, ChatEvent.FAILED, 1, "你不是对方好友",
                                                                        sendReq.getClientMsgId(), null)));
                                        chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));
                                        return;
                                } else if (Boolean.TRUE.equals(peerFriendship.getBlocked())) {
                                        List<ChatPushUnit> chatPushUnits = List.of(new ChatPushUnit(senderId, sessionId,
                                                        new WsResult<>(WsType.CHAT, ChatEvent.FAILED, 1, "你已被对方拉黑",
                                                                        sendReq.getClientMsgId(), null)));
                                        chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));
                                        return;
                                }
                                int rows = messageMapper.insertMessage(message);
                                if (rows <= 0) {
                                        System.out.println("发送信息失败");
                                        List<ChatPushUnit> chatPushUnits = List.of(new ChatPushUnit(senderId, sessionId,
                                                        new WsResult<>(WsType.CHAT, ChatEvent.FAILED, 1, "发送失败",
                                                                        sendReq.getClientMsgId(),
                                                                        null)));
                                        chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));
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
                                Conversation[] convs = conversationMapper
                                                .fetchConversationsByUserIdsAndSessionId(
                                                                new Long[] { message.getPeerId() },
                                                                message.getSessionId());
                                List<ChatPushUnit> chatPushUnits = List.of(new ChatPushUnit(message.getPeerId(),
                                                sessionId,
                                                new WsResult<>(WsType.CHAT, ChatEvent.RECEIVE, 0, "收到消息",
                                                                null,
                                                                new SendPush(
                                                                                new MessageVO(message
                                                                                                .getMsgId(),
                                                                                                sessionId,
                                                                                                sendReq.getConvType(),
                                                                                                sendReq.getSenderId(),
                                                                                                null,
                                                                                                sendReq.getMsgType(),
                                                                                                sendReq.getContent(),
                                                                                                sendReq.getQuoteMsgId(),
                                                                                                sendReq.getRecallFlag(),
                                                                                                message.getCreatedTime()),
                                                                                convs[0]))));
                                chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));

                                // 发送成功 -> sender
                                chatPushUnits = List.of(new ChatPushUnit(senderId, sessionId,
                                                new WsResult<>(WsType.CHAT, ChatEvent.ACK, 0, "发送成功",
                                                                sendReq.getClientMsgId(),
                                                                new AckResp(message.getMsgId(),
                                                                                message.getCreatedTime()))));
                                chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));
                        }
                        // 群聊
                        else {
                                Long[] userIds = cache.getGroupMembersOrLoad(sendReq.getSessionId(), () -> {
                                        GroupMember[] groupMembers = groupMapper
                                                        .fetchGroupMembers(sendReq.getSessionId());
                                        return Arrays.stream(groupMembers).map(GroupMember::getMemberUserId)
                                                        .toArray(Long[]::new);
                                });
                                // sender 不是群成员
                                if (Arrays.stream(userIds).noneMatch(userId -> userId.equals(senderId))) {
                                        List<ChatPushUnit> chatPushUnits = List.of(new ChatPushUnit(senderId, sessionId,
                                                        new WsResult<>(WsType.CHAT, ChatEvent.FAILED, 1, "你不是群成员",
                                                                        sendReq.getClientMsgId(),
                                                                        null)));
                                        chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));
                                        return;
                                }
                                // 插入消息
                                int rows = messageMapper.insertMessage(message);
                                if (rows <= 0) {
                                        System.out.println("发送信息失败");
                                        List<ChatPushUnit> chatPushUnits = List.of(new ChatPushUnit(senderId, sessionId,
                                                        new WsResult<>(WsType.CHAT, ChatEvent.FAILED, 1, "发送失败",
                                                                        sendReq.getClientMsgId(),
                                                                        null)));
                                        chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));
                                        return;
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
                                Conversation[] convs = conversationMapper.fetchConversationsByUserIdsAndSessionId(
                                                userIds,
                                                message.getSessionId());
                                // 推送
                                List<ChatPushUnit> chatPushUnits = new ArrayList<>();
                                for (Conversation c : convs) {
                                        chatPushUnits.add(new ChatPushUnit(c.getUserId(), sessionId,
                                                        new WsResult<>(
                                                                        WsType.CHAT,
                                                                        ChatEvent.RECEIVE, 0,
                                                                        "收到消息", null,
                                                                        new SendPush(new MessageVO(
                                                                                        message.getMsgId(),
                                                                                        sessionId,
                                                                                        sendReq.getConvType(),
                                                                                        sendReq.getSenderId(),
                                                                                        null,
                                                                                        sendReq.getMsgType(),
                                                                                        sendReq.getContent(),
                                                                                        sendReq.getQuoteMsgId(),
                                                                                        sendReq.getRecallFlag(),
                                                                                        message.getCreatedTime()),
                                                                                        c))));

                                }
                                chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));
                                // 推送成功 -> sender
                                chatPushUnits = List.of(new ChatPushUnit(senderId, sessionId,
                                                new WsResult<>(WsType.CHAT, ChatEvent.ACK, 0, "发送成功",
                                                                sendReq.getClientMsgId(),
                                                                new AckResp(message.getMsgId(),
                                                                                message.getCreatedTime()))));
                                chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));

                        }
                } catch (Exception e) {
                        e.printStackTrace();
                        List<ChatPushUnit> chatPushUnits = List
                                        .of(new ChatPushUnit(senderId, sessionId,
                                                        new WsResult<>(WsType.CHAT, ChatEvent.FAILED, 1, "发送失败",
                                                                        sendReq.getClientMsgId(),
                                                                        null)));
                        chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));
                        throw new RuntimeException(e);
                }
        }

        public void handleRecallMessage(WsEvent wsEvent) {
                RecallReq recallReq = JSON.parseObject(wsEvent.getPayload(), RecallReq.class);
                Long msgId = recallReq.getMsgId();
                Long sessionId = recallReq.getSessionId();
                Long userId = wsEvent.getFromUserId();
                Long peerId = recallReq.getPeerId();
                Boolean convType = recallReq.getConvType();
                System.out.println("recallMessage: " + recallReq);
                if (msgId == null || sessionId == null || convType == null
                                || (Boolean.FALSE.equals(convType) && peerId == null)) {
                        return;
                }
                try {
                        messageMapper.recallMessage(msgId);
                        // 私聊
                        if (Boolean.FALSE.equals(convType)) {
                                List<ChatPushUnit> chatPushUnits = List
                                                .of(new ChatPushUnit(peerId, sessionId,
                                                                new WsResult(WsType.CHAT, ChatEvent.RECALL, 0, "消息撤回",
                                                                                null,
                                                                                new SendPush(new MessageVO(msgId,
                                                                                                sessionId,
                                                                                                recallReq.getConvType(),
                                                                                                null, null, null,
                                                                                                null, null,
                                                                                                true,
                                                                                                null), null))));
                                chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));

                                chatPushUnits = List.of(new ChatPushUnit(userId, sessionId,
                                                new WsResult(WsType.CHAT, ChatEvent.RECALL, 0, "消息撤回", null,
                                                                new SendPush(new MessageVO(msgId, sessionId,
                                                                                recallReq.getConvType(),
                                                                                null, null, null,
                                                                                null, null,
                                                                                true,
                                                                                null), null))));
                                chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));
                        }
                        // 群聊
                        else {
                                Long[] groupMemberUserIds = cache.getGroupMembersOrLoad(sessionId, () -> {
                                        GroupMember[] groupMembers = groupMapper.fetchGroupMembers(sessionId);
                                        return Arrays.stream(groupMembers).map(GroupMember::getMemberUserId)
                                                        .toArray(Long[]::new);
                                });
                                List<ChatPushUnit> chatPushUnits = new ArrayList<>();
                                for (Long memberUserId : groupMemberUserIds) {
                                        chatPushUnits.add(new ChatPushUnit(memberUserId, sessionId,
                                                        new WsResult(WsType.CHAT, ChatEvent.RECALL, 0, "消息撤回", null,
                                                                        new SendPush(new MessageVO(msgId, sessionId,
                                                                                        recallReq.getConvType(),
                                                                                        null, null, null,
                                                                                        null, null,
                                                                                        true,
                                                                                        null), null))));
                                }
                                chatPushProducer.pushChatMessage(new ChatPush(chatPushUnits));
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }

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
