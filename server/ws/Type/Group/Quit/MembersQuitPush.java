package com.wetalk.ws.Type.Group.Quit;

import lombok.Data;

@Data
public class MembersQuitPush {
    private Long groupId;
    private Long[] memberUserIds;

    public MembersQuitPush(Long groupId, Long[] memberUserIds) {
        this.groupId = groupId;
        this.memberUserIds = memberUserIds;
    }
}
