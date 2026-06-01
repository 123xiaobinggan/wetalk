package com.wetalk.business.friend.vo;

import com.wetalk.model.Friendship;

public class FriendshipGetVO {
    private Friendship friendship;

    public FriendshipGetVO() {
    }

    public FriendshipGetVO(Friendship friendship) {
        this.friendship = friendship;
    }

    public Friendship getFriendship() {
        return friendship;
    }

    public void setFriendship(Friendship friendship) {
        this.friendship = friendship;
    }
}
