package com.wetalk.business.chatPush.consumer;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import com.wetalk.business.chatPush.service.ChatPushService;
import com.wetalk.mq.constant.MqTopic;
import com.wetalk.mq.constant.MqTag;
import com.wetalk.mq.protocol.ChatPush;
import com.wetalk.mq.constant.MqConsumerGroup;
import com.wetalk.ws.protocol.event.ChatEvent;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = MqTopic.CHAT_PUSH,
    consumerGroup = MqConsumerGroup.CHAT_PUSH_GROUP,
    consumeMode = ConsumeMode.ORDERLY,
    messageModel = MessageModel.CLUSTERING
)
public class ChatPushEventConsumer implements RocketMQListener<ChatPush> {
    private final ChatPushService chatPushService;

    @Override
    public void onMessage(ChatPush chatPush){
        chatPushService.chatPush(chatPush);
    }
}
