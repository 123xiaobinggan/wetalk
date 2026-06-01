package com.wetalk.mq.protocol;

import com.wetalk.mq.protocol.ChatPushUnit;
import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class ChatPush {
    private List<ChatPushUnit> chatPushUnits;

    public ChatPush(List<ChatPushUnit> chatPushUnits){
        this.chatPushUnits = chatPushUnits;
    }
}
