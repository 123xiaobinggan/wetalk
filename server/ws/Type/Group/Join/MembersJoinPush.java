package com.wetalk.ws.Type.Group.Join;

import com.wetalk.model.*;
import lombok.Data;

@Data
public class MembersJoinPush {
    private Group group;
    private GroupMember[] groupMembers;

    public MembersJoinPush() {
    }

    public MembersJoinPush(Group group, GroupMember[] groupMembers) {
        this.group = group;
        this.groupMembers = groupMembers;
    }

}
