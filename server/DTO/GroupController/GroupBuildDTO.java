package com.wetalk.DTO.GroupController;

import lombok.Data;

@Data
public class GroupBuildDTO {
    private Long[] userIds;
    private String groupName;
    private Long ownerUserId;

    public GroupBuildDTO(Long[] userIds, String groupName, Long ownerUserId) {
        this.userIds = userIds;
        this.groupName = groupName;
        this.ownerUserId = ownerUserId;
    }

    public void setUserIds(Long[] userIds) {
        this.userIds = userIds;
    }

    public Long[] getUserIds() {
        return userIds;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }
}
