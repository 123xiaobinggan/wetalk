package com.wetalk.VO.UserController;

import com.wetalk.model.User;

public class AutoEnterVO {
    private String token;
    private User user;

    public AutoEnterVO(String token, User user) {
        this.token = token;
        this.user = user;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
