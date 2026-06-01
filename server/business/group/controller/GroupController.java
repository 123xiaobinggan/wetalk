package com.wetalk.business.group.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.security.core.Authentication;

import java.util.*;

import com.wetalk.model.*;
import com.wetalk.business.group.service.GroupService;
import com.wetalk.business.group.dto.*;
import com.wetalk.business.group.vo.*;
import com.wetalk.vo.HttpResult;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class GroupController {
    private final GroupService groupService;

    @PostMapping("/buildGroup")
    public HttpResult<Object> buildGroup(@RequestBody GroupBuildDTO dto) {
        return groupService.buildGroup(dto);
    }

    @PostMapping("/fetchGroups")
    public HttpResult<Object> fetchGroups(@RequestBody GroupsFetchDTO dto) {
        return groupService.fetchGroups(dto);
    }

    @PostMapping("/fetchGroupMembers")
    public HttpResult<Object> fetchGroupMembers(@RequestBody GroupMembersFetchDTO dto) {
        return groupService.fetchGroupMembers(dto);
    }

    @PostMapping("/joinGroup")
    public HttpResult<Object> joinGroup(@RequestBody GroupJoinDTO dto) {
        return groupService.joinGroup(dto);
    }

    @PostMapping("/quitGroup")
    public HttpResult<Object> quitGroup(@RequestBody GroupQuitDTO dto) {
        return groupService.quitGroup(dto);
    }

    @PostMapping("/disbandGroup")
    public HttpResult<Object> disbandGroup(@RequestBody GroupDisbandDTO dto) {
        return groupService.disbandGroup(dto);
    }

    @PostMapping("setGroupMembersRole")
    public HttpResult<Object> setGroupMembersRole(@RequestBody GroupMembersRoleSetDTO dto) {
        return groupService.setGroupMembersRole(dto);
    }

    @PostMapping("setGroupSilenceAll")
    public HttpResult<Object> setGroupSilenceAll(@RequestBody GroupUpdateDTO dto) {
        return groupService.setGroupSilenceAll(dto);
    }

    @PostMapping("setGroupUnSilenceAll")
    public HttpResult<Object> setGroupUnSilenceAll(@RequestBody GroupUpdateDTO dto) {
        return groupService.setGroupUnSilenceAll(dto);
    }

    @PostMapping("updateGroup")
    public HttpResult<Object> updateGroup(@RequestBody GroupUpdateDTO dto) {
        return groupService.updateGroup(dto);
    }

    @PostMapping("updateGroupMembers")
    public HttpResult<Object> updateGroupMembers(@RequestBody GroupMembersUpdateDTO dto) {
        return groupService.updateGroupMembers(dto);
    }

    @PostMapping("transferGroupOwner")
    public HttpResult<Object> transferGroupOwner(@RequestBody GroupOwnerTransferDTO dto) {
        return groupService.transferGroupOwner(dto);
    }

}
