package com.wetalk.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.security.core.Authentication;

import java.util.*;

import com.wetalk.model.*;
import com.wetalk.service.FriendService;
import com.wetalk.DTO.FriendController.*;
import com.wetalk.VO.FriendController.*;
import com.wetalk.VO.HttpResult;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.wetalk.VO.FriendController.SearchFriendsVO;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class FriendController {
    private final FriendService friendService;

    @PostMapping("/searchFriends")
    public HttpResult<SearchFriendsVO> searchFriends(@RequestBody SearchFriendsDTO dto) {
        return friendService.searchFriends(dto);
    }

    @PostMapping("/fetchFriendRequests")
    public HttpResult<FriendRequestVO> fetchFriendRequest(@RequestBody FriendRequestGetDTO dto) {
        return friendService.fetchFriendRequests(dto);
    }

    @PostMapping("/sendFriendRequest")
    public HttpResult<Object> sendFriendRequest(@RequestBody FriendRequestSendDTO dto) {
        return friendService.sendFriendRequest(dto);
    }

    @PostMapping("/readFriendRequest")
    public HttpResult<Object> readFriendRequest(@RequestBody FriendRequestReadDTO dto) {
        System.out.println("readFriendRequest: " + dto);
        return friendService.readFriendRequest(dto);
    }

    @PostMapping("/acceptFriendRequest")
    public void acceptFriendRequest(@RequestBody FriendRequestAcceptDTO dto) {
        friendService.acceptFriendRequest(dto);
    }

    @PostMapping("/fetchFriendships")
    public HttpResult<Object> fetchFriendships(@RequestBody FriendshipsFetchDTO dto) {
        return friendService.fetchFriendships(dto);
    }

    @PostMapping("/getFriendship")
    public HttpResult<Object> getFriendship(@RequestBody FriendshipGetDTO dto) {
        return friendService.getFriendship(dto);
    }

    @PostMapping("/updateFriendship")
    public HttpResult<Object> updateFriendship(@RequestBody FriendshipUpdateDTO dto) {
        return friendService.updateFriendship(dto);
    }

    @PostMapping("/blockFriend")
    public HttpResult<Object> blockFriend(@RequestBody FriendBlockDTO dto) {
        return friendService.blockFriend(dto);
    }

    @PostMapping("/unBlockFriend")
    public HttpResult<Object> unBlockFriend(@RequestBody FriendUnBlockDTO dto) {
        return friendService.unBlockFriend(dto);
    }

    @PostMapping("/deleteFriend")
    public HttpResult<Object> deleteFriend(@RequestBody FriendDeleteDTO dto) {
        return friendService.deleteFriend(dto);
    }
}
