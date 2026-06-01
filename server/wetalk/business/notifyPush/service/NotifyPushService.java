package com.wetalk.business.notifyPush.service;

import lombok.RequiredArgsConstructor;

import com.wetalk.mq.protocol.NotifyPush;
import com.wetalk.mq.protocol.NotifyPushUnit;
import com.wetalk.netty.session.SessionManager;
import com.wetalk.utils.WsSender;
import io.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class NotifyPushService {

    private final SessionManager sessionManager;
    @Qualifier("notifyPushExecutor")
    private final ThreadPoolTaskExecutor notifyPushExecutor;

    public void notifyPush(NotifyPush notifyPush) {
        if (notifyPush == null
                || notifyPush.getNotifyPushUnits() == null
                || notifyPush.getNotifyPushUnits().isEmpty()) {
            return;
        }

        for (NotifyPushUnit unit : notifyPush.getNotifyPushUnits()) {
            notifyPushExecutor.execute(() -> pushOne(unit));
        }
    }

    private void pushOne(NotifyPushUnit unit) {
        try {
            if (unit == null || unit.getUserId() == null || unit.getWsResult() == null) {
                return;
            }

            Channel channel = sessionManager.getChannel(unit.getUserId());

            if (channel != null) {
                WsSender.send(channel, unit.getWsResult());
            }
        } catch (Exception e) {
            System.out.println("notify push failed, userId="
                    + (unit == null ? null : unit.getUserId())
                    + ", error=" + e.getMessage());
        }
    }
}