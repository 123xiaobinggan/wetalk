package com.wetalk.ws.Type.Chat.Send;

import com.wetalk.model.Conversation;
import com.wetalk.business.message.vo.MessageVO;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SendPush<T> {
    private MessageVO<?> message;
    private Conversation conversation;

    public SendPush() {
    }

    public SendPush(MessageVO<?> message, Conversation conversation) {
        this.message = message;
        this.conversation = conversation;
    }
}
