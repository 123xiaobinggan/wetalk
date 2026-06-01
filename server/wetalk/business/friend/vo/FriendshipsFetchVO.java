package com.wetalk.business.friend.vo;

import com.wetalk.model.Friendship;

public class FriendshipsFetchVO {
    private Friendship[] friendships;

    public FriendshipsFetchVO(Friendship[] friendships) {
        this.friendships = friendships;
    }

    public Friendship[] getFriendships() {
        return friendships;
    }

    public void setFriendships(Friendship[] friendships) {
        this.friendships = friendships;
    }
}
