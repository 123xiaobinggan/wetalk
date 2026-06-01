package com.wetalk.ws.Type.Friend.RequestAccept;

import com.wetalk.model.*;

public class RequestAcceptResp {
    private Friendship friendship;
    private Conversation conversation;
    private Long friendRequestId;

    public RequestAcceptResp() {
    }

    public RequestAcceptResp(Friendship friendship, Conversation conversation, Long friendRequestId) {
        this.friendship = friendship;
        this.conversation = conversation;
        this.friendRequestId = friendRequestId;
    }

    public Friendship getFriendship() {
        return friendship;
    }

    public void setFriendship(Friendship friendship) {
        this.friendship = friendship;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public Long getFriendRequestId() {
        return friendRequestId;
    }

    public void setFriendRequestId(Long friendRequestId) {
        this.friendRequestId = friendRequestId;
    }
}
