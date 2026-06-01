package com.wetalk.business.friend.dto;
import lombok.Data;

@Data
public class FriendshipsFetchDTO {
    private Long userId;

    public FriendshipsFetchDTO(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
