package com.wetalk.DTO.GroupController;

import com.wetalk.model.GroupMember;
import lombok.Data;

@Data
public class GroupMembersRoleSetDTO {
    private GroupMember[] groupMembers;
    private Long ownerUserId;

    public GroupMembersRoleSetDTO(){}

    public GroupMembersRoleSetDTO(GroupMember[] groupMembers, Long ownerUserId) {
        this.groupMembers = groupMembers;
        this.ownerUserId = ownerUserId;
    }
}
