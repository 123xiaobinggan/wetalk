package com.wetalk.model;

import org.apache.ibatis.annotations.Mapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.*;
import lombok.Data;

import com.wetalk.utils.*;

@Data
public class Group {
    private Long groupId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;
    private String groupName;
    private String groupAvatar;
    private String announcement;
    private Long ownerUserId;
    private Integer memberCnt;
    private Integer status;
    private LocalDateTime createdTime;

    public Group() {
    }

    public Group(String groupName, String groupAvatar, Long ownerUserId,
            Integer memberCnt, Integer status,
            LocalDateTime createdTime) {
        this.groupName = groupName;
        this.groupAvatar = groupAvatar;
        this.ownerUserId = ownerUserId;
        this.memberCnt = memberCnt;
        this.status = status;
        this.createdTime = createdTime;
        if (this.groupAvatar == null) {
            this.groupAvatar = "http://120.48.156.237:90/static_resources/wetalk/avatar/spread_hands.jpg";
        }
        if (this.createdTime == null) {
            this.createdTime = TimeUtils.currentTime();
        }
    }

}
