package com.wetalk.ws.Type.Chat.Content.SystemContentClass;

import lombok.Data;

@Data
public class MembersJoinContent {
    private Long inviterUserId;
    private Long[] inviteeUserIds;

    public MembersJoinContent() {
    }

    public MembersJoinContent(Long inviterUserId, Long[] inviteeUserIds) {
        this.inviterUserId = inviterUserId;
        this.inviteeUserIds = inviteeUserIds;
    }

}
