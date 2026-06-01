package com.wetalk.business.call.consumer;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import com.wetalk.business.call.service.CallService;
import com.wetalk.mq.constant.MqTopic;
import com.wetalk.mq.constant.MqTag;
import com.wetalk.mq.event.WsEvent;
import com.wetalk.mq.constant.MqConsumerGroup;
import com.wetalk.ws.protocol.event.CallEvent;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = MqTopic.CALL,
    selectorExpression = MqTag.AUDIO_CALL + " || " + MqTag.AUDIO_END,
    consumerGroup = MqConsumerGroup.CALL_GROUP,
    consumeMode = ConsumeMode.ORDERLY,
    messageModel = MessageModel.CLUSTERING
)
public class CallEventConsumer implements RocketMQListener<WsEvent> {
    private final CallService callService;

    @Override
    public void onMessage(WsEvent wsEvent){
        switch(wsEvent.getEvent()){
            case CallEvent.AUDIO_CALL:
                callService.handleCallInvite(wsEvent);
                break;
            case CallEvent.AUDIO_END:
                callService.handleCallEnd(wsEvent);
                break;
        }
    }
}
