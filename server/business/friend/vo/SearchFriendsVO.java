package com.wetalk.business.friend.vo;

import com.wetalk.model.User;

public class SearchFriendsVO {
    private User[] searchUsers;

    public SearchFriendsVO(User[] searchUsers) {
        this.searchUsers = searchUsers;
    }

    public void setSearchUsers(User[] searchUsers) {
        this.searchUsers = searchUsers;
    }

    public User[] getSearchUsers(){
        return searchUsers;
    }

}
