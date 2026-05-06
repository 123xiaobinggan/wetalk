package com.wetalk.VO.UserController;

import com.wetalk.model.User;

public class UpdateUserInfoVO {
    private User user;

    public UpdateUserInfoVO(User user){
        this.user = user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser(){
        return user;
    }
}
