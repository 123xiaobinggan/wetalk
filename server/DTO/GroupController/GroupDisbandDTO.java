package com.wetalk.DTO.GroupController;

import lombok.Data;

@Data
public class GroupDisbandDTO {
    private Long groupId;
    private Long ownerUserId;

    public GroupDisbandDTO(Long groupId, Long ownerUserId) {
        this.groupId = groupId;
        this.ownerUserId = ownerUserId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }
}
