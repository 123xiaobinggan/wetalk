package com.wetalk.business.friend.dto;

import lombok.Data;

@Data
public class FriendRequestSendDTO {
    private Long requesterUserId;
    private Long requesteeUserId;
    private String requestMsg;
    private String remark;
    private Boolean hideMyMoments;
    private Boolean hideFriendMoments;

    public FriendRequestSendDTO(){}
    
    public Long getRequesterUserId(){
        return requesterUserId;
    }

    public Long getRequesteeUserId(){
        return requesteeUserId;
    }

    public String getRequestMsg(){
        return requestMsg;
    }

    public String getRemark(){
        return remark;
    }

    public Boolean getHideMyMoments(){
        return hideMyMoments;
    }

    public Boolean getHideFriendMoments(){
        return hideFriendMoments;
    }
}
