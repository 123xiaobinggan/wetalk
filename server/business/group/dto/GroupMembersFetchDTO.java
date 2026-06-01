package com.wetalk.business.group.dto;

import lombok.Data;

@Data
public class GroupMembersFetchDTO {
    private Long groupId;

    public GroupMembersFetchDTO() {
    }

    public GroupMembersFetchDTO(Long groupId) {
        this.groupId = groupId;
    }
}
