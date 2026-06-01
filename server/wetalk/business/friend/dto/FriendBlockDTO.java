package com.wetalk.business.friend.dto;

import lombok.Data;

@Data
public class FriendBlockDTO {
    private Long userId;
    private Long friendUserId;

    public FriendBlockDTO(Long userId, Long friendUserId) {
        this.userId = userId;
        this.friendUserId = friendUserId;
    }
}
