package com.wetalk.business.notifyPush.consumer;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import com.wetalk.business.notifyPush.service.NotifyPushService;
import com.wetalk.mq.constant.MqTopic;
import com.wetalk.mq.constant.MqTag;
import com.wetalk.mq.protocol.NotifyPush;
import com.wetalk.mq.constant.MqConsumerGroup;
import com.wetalk.ws.protocol.event.ChatEvent;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = MqTopic.NOTIFY_PUSH,
    consumerGroup = MqConsumerGroup.NOTIFY_PUSH_GROUP,
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING
)
public class NotifyPushConsumer implements RocketMQListener<NotifyPush> {
    private final NotifyPushService notifyPushService;

    @Override
    public void onMessage(NotifyPush notifyPush){
        notifyPushService.notifyPush(notifyPush);
    }
}
