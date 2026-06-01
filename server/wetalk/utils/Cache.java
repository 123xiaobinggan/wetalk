package com.wetalk.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.function.Supplier;
import java.time.LocalDateTime;

import com.wetalk.model.*;
import com.wetalk.utils.RedisUtil;

@RequiredArgsConstructor
@Component
public class Cache {
    private final RedisUtil redisUtil;

    private static final String USER = "user:";
    private static final String USER_TOKEN = "user:token:";
    private static final String GROUP = "group:";
    private static final String GROUP_MEMBER = "group:members:";
    private static final String CALL_START = "call_start:";
    private static final String CALL_MSG = "call_msg:";

    private static final Long USER_EXPIRE = 7 * 24 * 3600L;
    private static final Long GROUP_EXPIRE = 30 * 24 * 3600L;

    public User getUserOrLoad(Long userId, Supplier<User> loader) {
        String key = USER + userId;
        User user = (User) redisUtil.get(key);
        if (user == null) {
            user = loader.get();
            setUser(user);
        }
        return user;
    }

    public void setUser(User user) {
        String key = USER + user.getUserId();
        redisUtil.set(key, user, USER_EXPIRE);
    }

    public String getToken(Long userId) {
        String key = USER_TOKEN + userId;
        return (String) redisUtil.get(key);
    }

    public void setToken(Long userId, String token) {
        String key = USER_TOKEN + userId;
        redisUtil.set(key, token, USER_EXPIRE);
    }

    public Long[] getGroupMembersOrLoad(Long groupId, Supplier<Long[]> loader) {
        String key = GROUP_MEMBER + groupId;
        Set<Object> groupMembers = redisUtil.getSet(key);
        if (groupMembers == null) {
            Long[] dbGroupMembers = loader.get();
            if (dbGroupMembers == null) {
                return new Long[] {};
            }
            setGroupMembers(groupId, dbGroupMembers);
            return dbGroupMembers;
        }

        return groupMembers.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .toArray(Long[]::new);
    }

    public void setGroupMembers(Long groupId, Long[] groupMemberUserIds) {
        String key = GROUP_MEMBER + groupId;
        String[] stringGroupMemberUserIds = Arrays.stream(groupMemberUserIds)
                .map(String::valueOf)
                .toArray(String[]::new);
        redisUtil.delete(key);
        redisUtil.setAdd(key, GROUP_EXPIRE, Arrays.asList(stringGroupMemberUserIds));
    }

    public void groupMembersJoin(Long groupId, Long[] memberUserIds) {
        String key = GROUP_MEMBER + groupId;
        List<String> userIds = Arrays.stream(memberUserIds)
                .map(String::valueOf)
                .collect(Collectors.toList());
        redisUtil.setAdd(key, GROUP_EXPIRE, userIds);
    }

    public void groupMembersQuit(Long groupId, Long[] memberUserIds) {
        String key = GROUP_MEMBER + groupId;
        List<String> userIds = Arrays.stream(memberUserIds)
                .map(String::valueOf)
                .collect(Collectors.toList());
        redisUtil.setRemove(key, GROUP_EXPIRE, userIds);
    }

    public LocalDateTime getCallStart(String callId) {
        String key = CALL_START + callId;
        Object value = redisUtil.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }

        // 如果是字符串，解析成 LocalDateTime
        if (value instanceof String) {
            String timeStr = (String) value;
            return LocalDateTime.parse(timeStr);
        }

        return null;
    }

    public void setCallStart(String callId, LocalDateTime startTime) {
        String key = CALL_START + callId;
        redisUtil.set(key, startTime, USER_EXPIRE);
    }

    public void deleteCallStart(String callId) {
        String key = CALL_START + callId;
        redisUtil.delete(key);
    }

    public Long getCallMsgId(String callId) {
        String key = CALL_MSG + callId;
        Object value = redisUtil.get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        return null;
    }

    public void setCallMsgId(String callId, Long msgId) {
        String key = CALL_MSG + callId;
        redisUtil.set(key, msgId, USER_EXPIRE);
    }

    public void deleteCallMsgId(String callId) {
        String key = CALL_START + callId;
        redisUtil.delete(key);
    }

    public void delete(Long groupId) {
        String key = GROUP_MEMBER + groupId;
        redisUtil.delete(key);
    }
}
