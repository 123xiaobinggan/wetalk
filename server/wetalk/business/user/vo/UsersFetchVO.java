package com.wetalk.business.user.vo;

import com.wetalk.model.User;

public class UsersFetchVO {
    private User[] users;

    public UsersFetchVO(User[] users) {
        this.users = users;
    }

    public User[] getUsers() {
        return users;
    }
}
