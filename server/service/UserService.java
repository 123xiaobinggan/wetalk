package com.wetalk.service;

import com.wetalk.mapper.UserMapper;
import com.wetalk.mapper.FriendMapper;
import com.wetalk.model.*;
import com.wetalk.utils.*;
import com.wetalk.DTO.UserController.*;
import com.wetalk.VO.HttpResult;
import com.wetalk.VO.UserController.*;
import com.wetalk.ws.protocol.WsResult;
import com.wetalk.ws.protocol.WsType;
import com.wetalk.ws.Type.Friend.FriendUpdate.FriendUpdatePush;
import com.wetalk.ws.protocol.event.FriendEvent;
import com.wetalk.netty.session.SessionManager;

import io.netty.channel.Channel;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.*;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserMapper userMapper;
    private final FriendMapper friendMapper;
    private final FileService fileService;
    private final SessionManager sessionManager;
    private final JwtUtil jwtUtil;
    private final Cache cache;

    // enter:用于处理用户登录和注册逻辑
    public HttpResult<Object> enter(EnterDTO dto) {
        System.out.println("enter: " + dto);
        String accountId = dto.getAccountId();
        String password = dto.getPassword();
        Boolean isLogin = dto.getIsLogin();
        try {
            if (isLogin) {
                User user = userMapper.getUserByAccountId(accountId);
                if (user == null) {
                    return HttpResult.success(1, "用户不存在", null);
                } else {
                    if (!user.getPassword().equals(password)) {
                        return HttpResult.success(1, "密码错误", null);
                    } else {
                        user.setPassword("");

                        String token = jwtUtil.generateToken(
                                user.getUserId(),
                                user.getAccountId());

                        cache.setUser(user);
                        cache.setToken(user.getUserId(), token);

                        System.out.println("enter:token " + token);
                        return HttpResult.success(0, "登录成功", new EnterVO(token, user));
                    }
                }
            } else {
                User user = userMapper.getUserByAccountId(accountId);
                if (user != null) {
                    return HttpResult.success(1, "用户已存在", null);
                } else {
                    User newUser = new User(accountId, dto.getUsername(), password,
                            "http://120.48.156.237:90/static_resources/wetalk/avatar/spread_hands.jpg", dto.getSex(),
                            dto.getAreaName(), dto.getAreaCode());

                    int rows = userMapper.createNewUser(newUser);
                    if (rows <= 0) {
                        return HttpResult.error(1, "注册失败", null);
                    }
                    newUser.setPassword("");
                    String token = jwtUtil.generateToken(
                            newUser.getUserId(),
                            newUser.getAccountId());

                    cache.setUser(newUser);
                    cache.setToken(newUser.getUserId(), token);

                    return HttpResult.success(0, "注册成功", new EnterVO(token, newUser));

                }
            }
        } catch (Exception e) {
            System.out.println("service error: UserService");
            e.printStackTrace();
            return HttpResult.success(1, "系统错误", null);
        }

    }

    // updateUserInfo:用于处理用户信息更新逻辑
    public HttpResult<Object> updateUserInfo(UpdateUserInfoDTO dto) {
        System.out.println("updateUserInfo: " + dto);
        try {
            User user = userMapper.getUserByUserId(dto.getUserId());
            System.out.println("user" + user);
            if (user == null) {
                System.out.println("用户不存在" + dto.getOldAccountId());
                return HttpResult.error(1, "用户不存在", null);
            }
            // 如果改accountId, 保证新账户唯一
            if (!((dto.getAccountId()).equals(user.getAccountId()))) {
                User newUser = new User();
                newUser = userMapper.getUserByAccountId(dto.getAccountId());
                if (newUser != null) {
                    System.out.println("该用户已存在");
                    return HttpResult.error(1, "该账户已存在", null);
                }
                user.setAccountId(dto.getAccountId());
            }
            // 如果修改密码，验证原密码正确
            if (dto.getPassword() != null && !(dto.getPassword()).equals("")) {
                System.out.println("password" + (String) dto.getPassword());
                if (!user.getPassword().equals(dto.getPassword())) {
                    System.out.println("密码错误");
                    return HttpResult.error(1, "密码错误", null);
                }
                user.setPassword(dto.getNewPassword());
            }
            user.setAvatar(dto.getAvatar());
            user.setPersonalSignature(dto.getPersonalSignature());
            user.setUsername(dto.getUsername());
            user.setSex(dto.getSex());
            user.setAreaName(dto.getAreaName());
            user.setAreaCode(dto.getAreaCode());
            int rows = userMapper.updateUserInfo(user);
            if (rows > 0) {
                System.out.println("更新成功" + user);
                user.setPassword("");
                cache.setUser(user);
                Friendship[] friendships = friendMapper.fetchFriendships(user.getUserId());
                for (Friendship f : friendships) {
                    Channel channel = sessionManager.getChannel(f.getFriendUserId());
                    WsSender.send(channel,
                            new WsResult(WsType.FRIEND, FriendEvent.FRIEND_UPDATE, 0, "好友信息更新", null,
                                    new FriendUpdatePush(user)));

                }
                return HttpResult.success(0, "更新成功", new UpdateUserInfoVO(user));
            } else {
                System.out.println("更新失败");
                return HttpResult.success(1, "更新失败", null);
            }
        } catch (Exception e) {
            System.out.println("updateUserInfo: " + e);
            return HttpResult.success(1, "更新失败", null);
        }
    }

    // fetchUsers:获取用户
    public HttpResult<Object> fetchUsers(UsersFetchDTO dto) {
        System.out.println("fetchUsers " + dto);
        Long[] userIds = dto.getUserIds();
        try {
            User[] users = userMapper.fetchUsers(userIds);
            if (users == null) {
                return HttpResult.error(1, "没有用户", null);
            } else {
                return HttpResult.success(0, "获取用户信息成功", new UsersFetchVO(users));
            }
        } catch (Exception e) {
            System.out.println("fetchUsers: " + e);
            return HttpResult.error(1, "获取用户信息失败", null);
        }
    }

}
