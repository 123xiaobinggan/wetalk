package com.wetalk.business.chat.consumer;

import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import com.wetalk.business.chat.service.ChatMessageService;
import com.wetalk.mq.constant.MqTopic;
import com.wetalk.mq.constant.MqTag;
import com.wetalk.mq.protocol.WsEvent;
import com.wetalk.mq.constant.MqConsumerGroup;
import com.wetalk.ws.protocol.event.ChatEvent;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = MqTopic.CHAT,
    selectorExpression = MqTag.SEND + " || " + MqTag.RECALL,
    consumerGroup = MqConsumerGroup.CHAT_GROUP,
    consumeMode = ConsumeMode.ORDERLY,
    messageModel = MessageModel.CLUSTERING
)
public class ChatEventConsumer implements RocketMQListener<WsEvent> {
    private final ChatMessageService chatMessageService;

    @Override
    public void onMessage(WsEvent wsEvent){
        switch(wsEvent.getEvent()){
            case ChatEvent.SEND:
                chatMessageService.handleSendMessage(wsEvent);
                break;
            case ChatEvent.RECALL:
                chatMessageService.handleRecallMessage(wsEvent);
                break;
        }
        
    }

}
