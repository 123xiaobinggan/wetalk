package com.wetalk.VO.ConversationController;

import com.wetalk.model.Conversation;

import lombok.Data;

@Data
public class ConversationGetVO {
    private Conversation conv;

    public ConversationGetVO(Conversation conv) {
        this.conv = conv;
    }
}
