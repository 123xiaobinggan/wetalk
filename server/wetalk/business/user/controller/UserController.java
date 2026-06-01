package com.wetalk.business.user.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.security.core.Authentication;

import com.wetalk.model.User;
import com.wetalk.business.user.service.UserService;

import com.wetalk.business.user.dto.*;
import com.wetalk.vo.HttpResult;
import com.wetalk.business.user.vo.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;

    @GetMapping("/autoEnter")
    public HttpResult<AutoEnterVO> autoEnter(Authentication authentication, HttpServletRequest request) {

        User user = (User) authentication.getPrincipal();

        user.setPassword("");

        String token = (String) request.getAttribute("token");

        System.out.println("token" + token);
        return HttpResult.success(0, "登录成功", new AutoEnterVO(token, user));
    }

    @PostMapping("/enter")
    public HttpResult<Object> enter(@RequestBody EnterDTO dto) {
        System.out.println(dto);
        return userService.enter(dto);
    }

    @PostMapping("/updateUserInfo")
    public HttpResult<Object> updateUserInfo(
            @RequestBody UpdateUserInfoDTO dto) {
        return userService.updateUserInfo(dto);
    }

    @PostMapping("/fetchUsers")
    public HttpResult<Object> fetchUsers(@RequestBody UsersFetchDTO dto) {
        return userService.fetchUsers(dto);
    }
}
