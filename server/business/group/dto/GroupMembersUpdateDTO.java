package com.wetalk.business.group.dto;

import com.wetalk.model.GroupMember;

import lombok.Data;

@Data
public class GroupMembersUpdateDTO {
    private GroupMember[] groupMembers;

    public GroupMembersUpdateDTO() {
    }

    public GroupMembersUpdateDTO(GroupMember[] groupMembers) {
        this.groupMembers = groupMembers;
    }

}
