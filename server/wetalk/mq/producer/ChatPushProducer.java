package com.wetalk.mq.producer;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import com.wetalk.mq.protocol.ChatPush;
import com.wetalk.mq.constant.MqTopic;

@RequiredArgsConstructor
@Component
public class ChatPushProducer {
    private final RocketMQTemplate rocketMQTemplate;

    public void pushChatMessage(ChatPush chatPush) {
        if (chatPush == null
                || chatPush.getChatPushUnits() == null
                || chatPush.getChatPushUnits().isEmpty()) {
            return;
        }
        String hashKey = String.valueOf(chatPush.getChatPushUnits().get(0).getSessionId());
        rocketMQTemplate.syncSendOrderly(MqTopic.CHAT_PUSH, chatPush, hashKey);
    }
}
