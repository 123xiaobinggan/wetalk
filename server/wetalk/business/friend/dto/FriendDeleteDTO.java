package com.wetalk.business.friend.dto;

import lombok.Data;

@Data
public class FriendDeleteDTO {
    private Long userId;
    private Long friendUserId;

    public FriendDeleteDTO() {
    }

    public FriendDeleteDTO(Long userId, Long friendUserId) {
        this.userId = userId;
        this.friendUserId = friendUserId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFriendUserId() {
        return friendUserId;
    }

    public void setFriendUserId(Long friendUserId) {
        this.friendUserId = friendUserId;
    }

}
