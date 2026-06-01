package com.wetalk.mapper;

import com.wetalk.model.*;

import java.util.*;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FriendMapper {
    FriendRequest getFriendRequestByUserId(@Param("requesterUserId") Long requestUserId,
            @Param("requesteeUserId") Long requesteeUserId);

    FriendRequest getFriendRequestById(@Param("friendRequestId") Long friendRequestId);

    FriendRequest[] fetchFriendRequestsReceive(@Param("requesteeUserId") Long requesteeUserId);

    FriendRequest[] fetchFriendRequestsSend(@Param("requesterUserId") Long requesterUserId);

    Friendship getFriendship(@Param("userId") Long userId, @Param("friendUserId") Long friendUserId);

    Friendship[] fetchFriendships(@Param("userId") Long userId);

    int insertFriendRequest(@Param("friendRequest") FriendRequest friendRequest);

    int readFriendRequest(@Param("friendRequestId") Long friendRequestId);

    int acceptFriendRequest(@Param("friendRequestId") Long friendRequstId);

    int buildFriendship(@Param("friendship") Friendship friendship);

    int blockFriend(@Param("userId") Long userId, @Param("friendUserId") Long friendUserId);

    int unBlockFriend(@Param("userId") Long userId, @Param("friendUserId") Long friendUserId);

    int updateFriendship(@Param("friendship") Friendship friendship);

}
