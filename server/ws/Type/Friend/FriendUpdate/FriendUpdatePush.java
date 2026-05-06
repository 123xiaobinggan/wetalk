package com.wetalk.ws.Type.Friend.FriendUpdate;

import com.wetalk.model.User;

import lombok.Data;

@Data
public class FriendUpdatePush {
    private User user;

    public FriendUpdatePush(User user) {
        this.user = user;
    }
}
