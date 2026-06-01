package com.wetalk.business.friend.vo;

import com.wetalk.model.FriendRequest;

public class FriendRequestVO {
    private FriendRequest[] friendRequests;

    public FriendRequestVO(FriendRequest[] friendRequests){
        this.friendRequests = friendRequests;
    }

    public FriendRequest[] getFriendRequests(){
        return friendRequests;
    }

    public void setFriendRequests(FriendRequest[] friendRequests){
        this.friendRequests = friendRequests;
    }

}
