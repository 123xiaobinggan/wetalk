package com.wetalk.business.user.dto;

import lombok.Data;

@Data
public class UsersFetchDTO {
    private Long[] userIds;

    public UsersFetchDTO() {
    }

    public UsersFetchDTO(Long[] userIds) {
        this.userIds = userIds;
    }

    public void setUserIds(Long[] userIds) {
        this.userIds = userIds;
    }

    public Long[] getUserIds() {
        return userIds;
    }
}
