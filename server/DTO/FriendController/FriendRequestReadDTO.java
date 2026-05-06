package com.wetalk.DTO.FriendController;

import lombok.Data;

@Data
public class FriendRequestReadDTO {
    private Long friendRequestId;

    public FriendRequestReadDTO(){}

    public Long getFriendRequestId(){
        return friendRequestId;
    }
}
