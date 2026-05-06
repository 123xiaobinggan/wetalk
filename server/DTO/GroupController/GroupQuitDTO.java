package com.wetalk.DTO.GroupController;

import lombok.Data;

@Data
public class GroupQuitDTO {
    private Long[] userIds;
    private Long groupId;

    public GroupQuitDTO(Long[] userIds, Long groupId) {
        this.userIds = userIds;
        this.groupId = groupId;
    }

}
