package com.wetalk.VO.MessageController;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetalk.model.Message;
import com.wetalk.VO.MessageController.MessageVO;
import com.wetalk.ws.Type.Chat.Content.*;
import com.wetalk.ws.Type.Chat.Content.SystemContentClass.*;
import com.wetalk.ws.protocol.Chat.SystemContentType;

public class MessagesFetchVO {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private MessageVO[] messages;

    public MessagesFetchVO(Message[] messages) {
        if (messages == null) {
            this.messages = new MessageVO[0];
            return;
        }

        this.messages = new MessageVO[messages.length];
        for (int i = 0; i < messages.length; i++) {
            this.messages[i] = toMessageVO(messages[i]);
        }
    }

    public MessageVO[] getMessages() {
        return messages;
    }

    public void setMessages(MessageVO[] messages) {
        this.messages = messages;
    }

    private MessageVO toMessageVO(Message message) {
        Long msgId = message.getMsgId();
        String sessionId = String.valueOf(message.getSessionId());
        Boolean convType = message.getConvType();
        Long senderId = message.getSenderId();
        Long peerId = message.getPeerId();
        Integer msgType = message.getMsgType();
        String content = message.getContent();
        Boolean recallFlag = message.getRecallFlag();
        Long quoteMsgId = message.getQuoteMsgId();
        LocalDateTime createdTime = message.getCreatedTime();

        if (content == null) {
            return new MessageVO(msgId, sessionId, convType, senderId,
                    peerId, msgType, null, quoteMsgId, recallFlag,
                    createdTime);
        }

        try {
            switch (msgType) {
                case 1:
                    TextContent textContent = objectMapper.readValue(content, TextContent.class);
                    return new MessageVO(msgId, sessionId, convType, senderId, peerId, msgType, textContent,
                            quoteMsgId, recallFlag, createdTime);
                case 2:
                    AudioContent audioContent = objectMapper.readValue(content, AudioContent.class);
                    return new MessageVO(msgId, sessionId, convType, senderId, peerId, msgType, audioContent,
                            quoteMsgId, recallFlag, createdTime);
                case 3:
                    ImageContent imageContent = objectMapper.readValue(content, ImageContent.class);
                    return new MessageVO(msgId, sessionId, convType, senderId, peerId, msgType, imageContent,
                            quoteMsgId, recallFlag, createdTime);
                case 4:
                    VideoContent videoContent = objectMapper.readValue(content, VideoContent.class);
                    return new MessageVO(msgId, sessionId, convType, senderId, peerId, msgType, videoContent,
                            quoteMsgId, recallFlag, createdTime);
                case 5:
                    FileContent fileContent = objectMapper.readValue(content, FileContent.class);
                    return new MessageVO(msgId, sessionId, convType, senderId, peerId, msgType, fileContent,
                            quoteMsgId, recallFlag, createdTime);
                case 6:
                    SystemContent<?> rawSystemContent = objectMapper.readValue(
                            content,
                            new com.fasterxml.jackson.core.type.TypeReference<SystemContent<Object>>() {
                            });

                    if (rawSystemContent.getSystemType() == SystemContentType.GROUP_CREATE) {
                        GroupCreateContent data = objectMapper.convertValue(
                                rawSystemContent.getData(),
                                GroupCreateContent.class);

                        SystemContent<GroupCreateContent> systemContent = new SystemContent<>(
                                rawSystemContent.getSystemType(), data);

                        return new MessageVO(
                                msgId, sessionId, convType, senderId, peerId,
                                msgType, systemContent, quoteMsgId, recallFlag, createdTime);
                    }

                    if (rawSystemContent.getSystemType() == SystemContentType.MEMBERS_JOIN) {
                        MembersJoinContent data = objectMapper.convertValue(
                                rawSystemContent.getData(),
                                MembersJoinContent.class);

                        SystemContent<MembersJoinContent> systemContent = new SystemContent<>(
                                rawSystemContent.getSystemType(), data);

                        return new MessageVO(
                                msgId, sessionId, convType, senderId, peerId,
                                msgType, systemContent, quoteMsgId, recallFlag, createdTime);
                    }

                    return new MessageVO(
                            msgId, sessionId, convType, senderId, peerId,
                            msgType, rawSystemContent, quoteMsgId, recallFlag, createdTime);

                default:
                    return new MessageVO(msgId, sessionId, convType, senderId, peerId, msgType, content,
                            quoteMsgId, recallFlag, createdTime);
            }
        } catch (Exception e) {
            throw new RuntimeException("消息内容解析失败，msgId=" + msgId + ", msgType=" + msgType, e);
        }
    }
}
