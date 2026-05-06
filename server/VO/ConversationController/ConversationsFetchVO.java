package com.wetalk.VO.ConversationController;

import com.wetalk.model.*;

public class ConversationsFetchVO {
    private Conversation[] conversations;

    public ConversationsFetchVO(Conversation[] conversations){
        this.conversations = conversations;
    }

    public Conversation[] getConversations(){
        return conversations;
    }
}
