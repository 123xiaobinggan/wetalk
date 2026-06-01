package com.wetalk.mq.producer;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import com.wetalk.mq.protocol.WsEvent;
import com.wetalk.mq.constant.MqTag;
import com.wetalk.mq.constant.MqTopic;

@Component
@RequiredArgsConstructor
public class WsEventProducer {
    private final RocketMQTemplate rocketMQTemplate;

    // chat: send
    public void sendChatSendEvent(WsEvent wsEvent){
        String destination = build(MqTopic.CHAT, MqTag.SEND);
        String hashKey = String.valueOf(wsEvent.getSessionId());
        rocketMQTemplate.syncSendOrderly(destination, wsEvent, hashKey);
    }

    // chat: recall
    public void sendChatRecallEvent(WsEvent wsEvent){
        String destination = build(MqTopic.CHAT, MqTag.RECALL);
        String hashKey = String.valueOf(wsEvent.getSessionId());
        rocketMQTemplate.syncSendOrderly(destination, wsEvent, hashKey);
    }

    // call: audio_call
    public void sendCallAudioCallEvent(WsEvent wsEvent){
        String destination = build(MqTopic.CALL, MqTag.AUDIO_CALL);
        String hashKey = String.valueOf(wsEvent.getSessionId());
        rocketMQTemplate.syncSendOrderly(destination, wsEvent, hashKey);
    }

    // call: audio_end
    public void sendCallAudioEndEvent(WsEvent wsEvent){
        String destination = build(MqTopic.CALL, MqTag.AUDIO_END);
        String hashKey = String.valueOf(wsEvent.getSessionId());
        rocketMQTemplate.syncSendOrderly(destination, wsEvent, hashKey);
    }


    String build(String a,String b){
        return a+":"+b;
    }

}
