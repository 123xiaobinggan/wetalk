package com.wetalk.mapper;

import com.wetalk.model.*;
import java.util.*;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GroupMapper {

    Group[] fetchGroups(@Param("userId") Long userId);

    Group getGroup(@Param("groupId") Long groupId);

    GroupMember[] fetchGroupMembers(@Param("groupId") Long groupId);

    GroupMember getGroupMember(@Param("groupId") Long groupId, @Param("memberUserId") Long memberUserId);

    int buildGroup(@Param("group") Group group);

    int updateGroupSessionId(@Param("group") Group group);

    int updateGroupMemberCnt(@Param("groupId") Long groupId, @Param("cnt") int cnt);

    // 退出群聊
    int quitGroup(@Param("memberUserIds") Long[] memberUserIds, @Param("groupId") Long groupId);

    int addGroupMembers(@Param("groupMembers") GroupMember[] groupMembers);

    int updateGroupMembers(@Param("groupMembers") GroupMember[] groupMembers);

    int updateGroup(@Param("group") Group group);

}
