package com.wetalk.DTO.ConversationController;

import lombok.Data;

@Data
public class ConversationUpdateDTO {
    private Long convId;
    private String groupRemark;
    private Boolean pinned;
    private Boolean muted;
    private Boolean deleted;

    public ConversationUpdateDTO(Long convId, String groupRemark,
            Boolean pinned, Boolean muted, Boolean deleted) {
        this.convId = convId;
        this.groupRemark = groupRemark;
        this.pinned = pinned;
        this.muted = muted;
        this.deleted = deleted;
    }

}
