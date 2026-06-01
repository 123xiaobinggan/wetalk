package com.wetalk.mapper;

import com.wetalk.model.User;
import java.util.*;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User getUserByAccountId(@Param("accountId") String accountId);

    User getUserByUserId(@Param("userId") Long userId);

    User[] fetchUsers(@Param("userIds") Long[] userIds);

    int createNewUser(@Param("user") User user);

    int updateUserInfo(@Param("user") User user);

    User[] searchFriends(@Param("searchQuery") String searchQuery, @Param("userId") Long userId);

}
