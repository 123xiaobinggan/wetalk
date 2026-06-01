package com.wetalk.business.friend.dto;

import lombok.Data;

@Data
public class FriendRequestReadDTO {
    private Long friendRequestId;

    public FriendRequestReadDTO(){}

    public Long getFriendRequestId(){
        return friendRequestId;
    }
}
