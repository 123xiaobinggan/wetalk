package com.wetalk.ws.Type.Group.Create;

import com.wetalk.model.*;

import lombok.Data;

@Data
public class GroupCreatePush {
    private Conversation conversation;
    private Group group;

    public GroupCreatePush(Conversation conversation, Group group) {
        this.conversation = conversation;
        this.group = group;
    }

}
