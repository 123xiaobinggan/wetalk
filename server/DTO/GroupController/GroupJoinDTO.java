package com.wetalk.DTO.GroupController;

import lombok.Data;

@Data
public class GroupJoinDTO {
    private Long inviterUserId;
    private Long[] inviteeUserIds;
    private Long groupId;

    public GroupJoinDTO(Long inviterUserId, Long[] inviteeUserIds, Long groupId) {
        this.inviterUserId = inviterUserId;
        this.inviteeUserIds = inviteeUserIds;
        this.groupId = groupId;
    }
}
