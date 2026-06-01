package com.wetalk.business.friend.dto;

import lombok.Data;

@Data
public class SearchFriendsDTO {
    private String searchQuery;
    private Long userId;

    public SearchFriendsDTO(){}

    public String getSearchQuery(){
        return searchQuery;
    }

    public Long getUserId(){
        return userId;
    }
}
