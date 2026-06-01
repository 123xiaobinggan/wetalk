package com.wetalk.netty.session;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.netty.channel.Channel;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class SessionManager {

    private final ConcurrentHashMap<Long, Channel> userChannelMap = new ConcurrentHashMap<>();

    public void bind(Long userId, Channel channel) {
        userChannelMap.put(userId, channel);
    }

    public void unbind(Long userId, Channel channel) {
        Channel current = userChannelMap.get(userId);
        if (current == channel) {
            userChannelMap.remove(userId);
        }
    }

    public Channel getChannel(Long userId) {
        return userChannelMap.get(userId);
    }

    public boolean isOnline(Long userId) {
        Channel channel = userChannelMap.get(userId);
        return channel != null && channel.isActive();
    }
}
