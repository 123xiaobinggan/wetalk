package com.wetalk.mapper;

import com.wetalk.model.*;
import java.util.*;
import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MessageMapper {
        int insertMessage(@Param("msg") Message msg);

        Message[] fetchMessages(@Param("myUserId") Long myUserId, @Param("sessionId") Long sessionId,
                        @Param("msgId") Long msgId);

        Message[] fetchMessagesByParams(@Param("convId") Long convId, @Param("sessionId") Long sessionid,
                        @Param("senderId") Long senderId, @Param("keyword") String keyword,
                        @Param("msgTypes") Integer[] msgTypes,
                        @Param("dateStart") LocalDateTime dateStart, @Param("dateEnd") LocalDateTime dateEnd,
                        @Param("msgId") Long msgId);

        Message getMessage(@Param("msgId") Long msgId);

        int recallMessage(@Param("msgId") Long msgId);

        int deleteMessage(@Param("msgId") Long msgId, @Param("userId") Long userId);

        int updateMessageContent(@Param("msgId") Long msgId, @Param("content") String content);

        int clearHistoryMessages(@Param("convId") Long convId);

        int clearMessages(@Param("msgIds") Long[] msgIds, @Param("userId") Long userId);
}
