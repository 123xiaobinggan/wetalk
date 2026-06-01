package com.wetalk.business.conversation.dto;

import lombok.Data;

@Data
public class ConversationGetDTO {
    private Boolean convType;
    private Long userId;
    private Long friendUserId;
    private Long groupId;

    public ConversationGetDTO() {
    }

    public ConversationGetDTO(Boolean convType, Long userId,
            Long friendUserId, Long groupId) {
        this.convType = convType;
        this.userId = userId;
        this.friendUserId = friendUserId;
        this.groupId = groupId;
    }
}
