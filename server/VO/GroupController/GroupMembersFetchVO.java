package com.wetalk.VO.GroupController;

import com.wetalk.model.GroupMember;
import lombok.Data;

@Data
public class GroupMembersFetchVO {
    private GroupMember[] groupMembers;

    public GroupMembersFetchVO(GroupMember[] groupMembers) {
        this.groupMembers = groupMembers;
    }
}
