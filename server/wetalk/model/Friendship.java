package com.wetalk.model;

import org.apache.ibatis.annotations.Mapper;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Friendship {
    private Long userId;
    private Long friendUserId;
    private String remark;
    private Boolean blocked;
    private Boolean deleted;
    private Boolean hideMyMoments;
    private Boolean hideFriendMoments;
    private LocalDateTime createdTime;

    public Friendship() {
    }

    public Friendship(Long userId, Long friendUserId, String remark,
            Boolean hideMyMoments, Boolean hideFriendMoments,
            Boolean blocked, Boolean deleted, LocalDateTime createdTime) {
        this.userId = userId;
        this.friendUserId = friendUserId;
        this.remark = remark;
        this.blocked = blocked;
        this.deleted = deleted;
        this.hideMyMoments = hideMyMoments;
        this.hideFriendMoments = hideFriendMoments;
        this.createdTime = createdTime;
    }
}
