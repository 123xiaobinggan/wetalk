package com.wetalk.mq.producer;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import com.wetalk.mq.protocol.NotifyPush;
import com.wetalk.mq.constant.MqTopic;

@RequiredArgsConstructor
@Component
public class NotifyPushProducer {
    private final RocketMQTemplate rocketMQTemplate;

    public void pushNotification(NotifyPush notifyPush){
        rocketMQTemplate.syncSend(MqTopic.NOTIFY_PUSH, notifyPush);
    }
}
