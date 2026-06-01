package com.wetalk.mapper;

import com.wetalk.model.Conversation;
import java.util.*;
import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConversationMapper {
    // 个人角度：
    Conversation[] fetchConversationsByUserId(@Param("userId") Long userId);

    // sessionId角度
    Conversation[] fetchConversationsByUserIdsAndSessionId(@Param("userIds") Long[] userIds, @Param("sessionId") Long sessionId);

    // convId 角度
    Conversation getConversationByConvId(@Param("convId") Long convId);

    // userId + peerId
    Conversation getConversationByUserIdAndPeerId(@Param("userId") Long userId, @Param("peerId") Long peerId);

    int createNewConversations(@Param("convs") Conversation[] convs);

    // 单独更新
    int updateConversation(@Param("conv") Conversation conversation);

    // 批量更新
    int updateConversations(@Param("conv") Conversation conversation, @Param("userIds") Long[] userIds);

    int incrementUnreadCnt(@Param("conv") Conversation conversation, @Param("userIds") Long[] userIds);

    int markUnreadCnt(@Param("convId") Long convId);

    int clearChatHistory(@Param("convId") Long convId, @Param("lastClearedTime") LocalDateTime lastClearedTime);

}
