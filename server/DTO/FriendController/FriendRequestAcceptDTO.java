package com.wetalk.DTO.FriendController;

import lombok.Data;

@Data
public class FriendRequestAcceptDTO {
    private Long friendRequestId;
    private String remark;
    private Boolean hideMyMoments;
    private Boolean hideFriendMoments;

    public FriendRequestAcceptDTO() {
    }

    public FriendRequestAcceptDTO(Long friendRequestId, String remark, Boolean hideMyMoments,
            Boolean hideFriendMoments) {
        this.friendRequestId = friendRequestId;
        this.remark = remark;
        this.hideMyMoments = hideMyMoments;
        this.hideFriendMoments = hideFriendMoments;
    }

    public Long getFriendRequestId() {
        return friendRequestId;
    }

    public String getRemark() {
        return remark;
    }

    public Boolean getHideMyMoments() {
        return hideMyMoments;
    }

    public Boolean getHideFriendMoments() {
        return hideFriendMoments;
    }
}
