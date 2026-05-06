package com.wetalk.model;

import org.apache.ibatis.annotations.Mapper;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FriendRequest {
    private Long friendRequestId;
    private Long requesterUserId;
    private Long requesteeUserId;
    private String requestMsg;
    private String remark;
    private Integer status;
    private Boolean hideMyMoments;
    private Boolean hideFriendMoments;
    private LocalDateTime createdTime;

    public FriendRequest() {
    }

    public FriendRequest(Long requesterUserId, Long requesteeUserId, String requestMsg,
            String remark, Integer status, Boolean hideMyMoments,
            Boolean hideFriendMoments, LocalDateTime createdTime) {
        this.requesterUserId = requesterUserId;
        this.requesteeUserId = requesteeUserId;
        this.requestMsg = requestMsg;
        this.remark = remark;
        this.status = status;
        this.hideMyMoments = hideMyMoments;
        this.hideFriendMoments = hideFriendMoments;
        this.createdTime = createdTime;
    }
}
