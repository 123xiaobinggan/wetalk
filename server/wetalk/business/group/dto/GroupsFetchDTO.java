package com.wetalk.business.group.dto;

import lombok.Data;

@Data
public class GroupsFetchDTO {
    private Long userId;

    public GroupsFetchDTO(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
