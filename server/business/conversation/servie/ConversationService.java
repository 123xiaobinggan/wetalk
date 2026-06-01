package com.wetalk.business.conversation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestPart;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.Map;
import java.util.HashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.*;

import com.wetalk.model.*;
import com.wetalk.utils.*;
import com.wetalk.mapper.*;
import com.wetalk.netty.session.SessionManager;
import com.wetalk.business.conversation.dto.*;
import com.wetalk.vo.HttpResult;
import com.wetalk.business.conversation.vo.*;
import com.wetalk.ws.protocol.WsType;
import com.wetalk.ws.protocol.event.*;
import com.wetalk.ws.protocol.WsResult;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ConversationService {
    private final ConversationMapper conversationMapper;

    // 个人角度:获取会话列表
    public HttpResult<ConversationsFetchVO> fetchConversationsByUserId(ConversationsFetchDTO dto) {
        System.out.println("fetchConversationsByUserId: " + dto);
        Long userId = dto.getUserId();
        try {
            Conversation[] conversations = conversationMapper.fetchConversationsByUserId(userId);
            System.out.println(conversations);
            return HttpResult.success(0, "获取会话列表成功", new ConversationsFetchVO(conversations));
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "获取会话列表失败", null);
        }
    }

    // 获取单个会话
    public HttpResult<Object> getConversation(ConversationGetDTO dto) {
        System.out.println("getConversation: " + dto);
        Boolean convType = dto.getConvType();
        Long userId = dto.getUserId();
        Long friendUserId = dto.getFriendUserId();
        Long groupId = dto.getGroupId();
        try {
            Conversation conv;
            // 私聊
            if (Boolean.FALSE.equals(convType)) {
                conv = conversationMapper.getConversationByUserIdAndPeerId(userId, friendUserId);
            } else {
                Conversation[] convs = conversationMapper.fetchConversationsByUserIdsAndSessionId(new Long[] { userId },
                        groupId);
                conv = convs[0];
            }
            conv.setDeleted(false);
            conversationMapper.updateConversation(conv);
            return HttpResult.success(0, "获取会话成功", new ConversationGetVO(conv));
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "获取会话失败", null);
        }
    }

    // 标记 unread_cnt
    public HttpResult<Object> markConversationRead(ConversationMarkReadDTO dto) {
        Long convId = dto.getConvId();
        System.out.println("markConversationRead: " + dto);
        try {
            int rows = conversationMapper.markUnreadCnt(convId);
            if (rows <= 0) {
                return HttpResult.error(1, "消息已读错误", null);
            }
            return HttpResult.success(0, "消息已读成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "消息已读错误", null);
        }
    }

    // 更新conversation
    public HttpResult<Object> updateConversation(ConversationUpdateDTO dto) {
        Conversation conv = new Conversation();
        conv.setConvId(dto.getConvId());
        conv.setGroupRemark(dto.getGroupRemark());
        conv.setPinned(dto.getPinned());
        conv.setMuted(dto.getMuted());
        conv.setDeleted(dto.getDeleted());
        System.out.println("updateConversation " + dto);
        try {
            conversationMapper.updateConversation(conv);
            conv = conversationMapper.getConversationByConvId(conv.getConvId());
            return HttpResult.success(0, "更新setting成功", new ConversationGetVO(conv));
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "更新setting失败", null);
        }
    }

    // 清除消息记录：更新lastClearedTime
    public HttpResult<Object> clearChatHistory(ConversationClearChatHistoryDTO dto) {
        Long convId = dto.getConvId();
        LocalDateTime lastClearedTime = LocalDateTime.now();
        System.out.println("clearChatHistory: " + dto);
        try {
            int rows = conversationMapper.clearChatHistory(convId, lastClearedTime);
            if (rows <= 0) {
                return HttpResult.error(1, "清除历史消息失败", null);
            }
            return HttpResult.success(0, "清除历史消息成功", new ConversationClearChatHistoryVO(lastClearedTime));
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "清除历史消息失败", null);
        }
    }

}
