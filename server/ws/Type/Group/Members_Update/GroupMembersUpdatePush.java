package com.wetalk.ws.Type.Group.Members_Update;

import com.wetalk.model.GroupMember;
import lombok.Data;

@Data
public class GroupMembersUpdatePush {
    private GroupMember[] groupMembers;

    public GroupMembersUpdatePush(GroupMember[] groupMembers) {
        this.groupMembers = groupMembers;
    }
}
