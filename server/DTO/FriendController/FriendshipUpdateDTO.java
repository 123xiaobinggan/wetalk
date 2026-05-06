package com.wetalk.DTO.FriendController;

import lombok.Data;

@Data
public class FriendshipUpdateDTO {
    private Long userId;
    private Long friendUserId;
    private String remark;
    private Boolean hideMyMoments;
    private Boolean hideFriendMoments;

    public FriendshipUpdateDTO() {
    }

    public FriendshipUpdateDTO(Long userId, Long friendUserId, String remark, Boolean hideMyMoments,
            Boolean hideFriendMoments) {
        this.userId = userId;
        this.friendUserId = friendUserId;
        this.remark = remark;
        this.hideMyMoments = hideMyMoments;
        this.hideFriendMoments = hideFriendMoments;
    }

    public void setUserId(Long userId){
        this.userId = userId;
    }

    public Long getUserId(){
        return userId;
    }

    public void setFriendUserId(Long friendUserId){
        this.friendUserId = friendUserId;
    }

    public Long getFriendUserId(){
        return friendUserId;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getRemark() {
        return remark;
    }

    public void setHideMyMoments(Boolean hideMyMoments) {
        this.hideMyMoments = hideMyMoments;
    }

    public Boolean getHideMyMoments() {
        return hideMyMoments;
    }

    public void setHideFriendMoments(Boolean hideFriendMoments) {
        this.hideFriendMoments = hideFriendMoments;
    }

    public Boolean getHideFriendMoments() {
        return hideFriendMoments;
    }
}
