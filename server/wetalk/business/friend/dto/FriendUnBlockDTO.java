package com.wetalk.business.friend.dto;

import lombok.Data;

@Data
public class FriendUnBlockDTO {
    private Long userId;
    private Long friendUserId;

    public FriendUnBlockDTO(Long userId, Long friendUserId) {
        this.userId = userId;
        this.friendUserId = friendUserId;
    }
}
