package com.wetalk.DTO.FriendController;

import lombok.Data;

@Data
public class FriendRequestGetDTO {
    private Long userId;

    public FriendRequestGetDTO(){}

    public FriendRequestGetDTO(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
