package com.wetalk.DTO.GroupController;

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
