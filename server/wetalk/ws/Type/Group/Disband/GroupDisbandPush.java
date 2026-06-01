package com.wetalk.ws.Type.Group.Disband;

public class GroupDisbandPush {
    private Long groupId;

    public GroupDisbandPush(Long groupId) {
        this.groupId = groupId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
}
