package com.wetalk.model;

import org.apache.ibatis.annotations.Mapper;
import com.wetalk.utils.*;
import java.time.*;
import lombok.Data;

@Data
public class GroupMember {
    private Long groupId;
    private Long memberUserId;
    private Integer role;
    private String myNickname;
    private Boolean silence;
    private Long joinSeq;
    private LocalDateTime joinedTime;

    public GroupMember() {
    }

    public GroupMember(Long groupId, Long memberUserId, Integer role,
            String myNickname, Boolean silence) {
        this.groupId = groupId;
        this.memberUserId = memberUserId;
        this.role = role;
        this.myNickname = myNickname;
        this.silence = silence;
        this.joinedTime = TimeUtils.currentTime();
    }

    public GroupMember(Long groupId, Long memberUserId, Integer role) {
        this.groupId = groupId;
        this.memberUserId = memberUserId;
        this.role = role;
        this.joinedTime = TimeUtils.currentTime();
    }

}
