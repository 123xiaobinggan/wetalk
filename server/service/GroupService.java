package com.wetalk.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestPart;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.alibaba.fastjson2.JSON;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import com.wetalk.ws.Type.Group.Disband.GroupDisbandPush;
import com.wetalk.ws.Type.Group.Join.MembersJoinPush;

import com.wetalk.DTO.GroupController.*;
import com.wetalk.netty.constant.ChannelAttrKey;
import com.wetalk.ws.Type.Group.Create.GroupCreatePush;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.*;

import com.wetalk.model.*;
import com.wetalk.utils.*;
import com.wetalk.mapper.*;
import com.wetalk.netty.session.SessionManager;
import com.wetalk.DTO.GroupController.*;
import com.wetalk.VO.HttpResult;
import com.wetalk.VO.GroupController.*;
import com.wetalk.VO.MessageController.*;
import com.wetalk.ws.Type.Chat.Content.*;
import com.wetalk.ws.Type.Chat.Content.SystemContentClass.*;
import com.wetalk.ws.Type.Chat.Send.SendPush;
import com.wetalk.ws.Type.Group.Create.*;
import com.wetalk.ws.Type.Group.Join.*;
import com.wetalk.ws.Type.Group.Quit.*;
import com.wetalk.ws.Type.Group.Disband.*;
import com.wetalk.ws.Type.Group.Group_Update.*;
import com.wetalk.ws.Type.Group.Members_Update.*;
import com.wetalk.ws.protocol.WsType;
import com.wetalk.ws.protocol.event.*;
import com.wetalk.ws.protocol.Chat.*;
import com.wetalk.ws.protocol.WsResult;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.wetalk.DTO.GroupController.GroupBuildDTO;
import com.wetalk.DTO.GroupController.GroupDisbandDTO;
import com.wetalk.DTO.GroupController.GroupJoinDTO;
import com.wetalk.DTO.GroupController.GroupOwnerTransferDTO;
import com.wetalk.DTO.GroupController.GroupsFetchDTO;

import com.wetalk.VO.GroupController.GroupMembersFetchVO;
import com.wetalk.VO.MessageController.MessageVO;

@RequiredArgsConstructor
@Service
public class GroupService {
    private final UserMapper userMapper;
    private final GroupMapper groupMapper;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ConversationService conversationService;
    private final SessionManager sessionManager;
    private final Cache cache;

    @Transactional
    public HttpResult<Object> buildGroup(GroupBuildDTO dto) {
        Long[] userIds = new HashSet<>(Arrays.asList(dto.getUserIds())).toArray(new Long[0]);
        String groupName = dto.getGroupName();
        Long ownerUserId = dto.getOwnerUserId();
        System.out.println("GroupBuildDTO: " + dto);
        if (userIds.length <= 2) {
            System.out.println("length <= 2");
            return HttpResult.error(1, "群聊人数至少三人", null);
        }
        try {
            Group group = new Group(groupName, null, ownerUserId, userIds.length,
                    0, null);
            // 创建群聊
            int rows = groupMapper.buildGroup(group);
            if (rows <= 0) {
                return HttpResult.error(1, "群聊创建失败", null);
            }
            group.setSessionId(group.getGroupId());
            // 更新sessionId
            rows = groupMapper.updateGroupSessionId(group);
            if (rows <= 0) {
                throw new RuntimeException("更新sessionId失败");
            }
            // 插入群聊创建的系统消息
            Message message = new Message(true, group.getSessionId(), null, null, 6,
                    JSON.toJSONString(
                            new SystemContent(SystemContentType.GROUP_CREATE, new GroupCreateContent(ownerUserId))),
                    null, false);
            messageMapper.insertMessage(message);
            GroupMember[] groupMembers = new GroupMember[userIds.length];
            Conversation[] conversations = new Conversation[userIds.length];
            for (int i = 0; i < userIds.length; i++) {
                final Long userId = userIds[i];
                User user = cache.getUserOrLoad(userId, () -> {
                    return userMapper.getUserByUserId(userId);
                });
                groupMembers[i] = new GroupMember(group.getGroupId(), userIds[i],
                        userIds[i].equals(ownerUserId) ? 2 : 0, null, false);
                conversations[i] = new Conversation(userIds[i], group.getSessionId(), true, null, group.getGroupId(),
                        null, 0);
                conversations[i].setLastMsgId(message.getMsgId());
                conversations[i].setLastMsgBrief("群聊创建成功");
                conversations[i].setLastMsgTime(message.getCreatedTime());
                conversations[i].setDeleted(false);
            }
            // 插入cache
            cache.setGroupMembers(group.getGroupId(), userIds);
            // 插入新成员
            rows = groupMapper.addGroupMembers(groupMembers);
            if (rows <= 0) {
                return HttpResult.error(1, "群聊创建失败", null);
            }
            // 创建会话
            rows = conversationMapper.createNewConversations(conversations);
            if (rows <= 0) {
                return HttpResult.error(1, "群聊创建失败", null);
            }
            for (int i = 0; i < userIds.length; i++) {
                if (sessionManager.isOnline(userIds[i])) {
                    Channel memberChannel = sessionManager.getChannel(userIds[i]);
                    WsSender.send(memberChannel, new WsResult(WsType.GROUP,
                            GroupEvent.CREATE, 0, "群聊创建成功", null,
                            new GroupCreatePush(conversations[i], group)));
                }
            }

            return HttpResult.success(0, "群聊创建成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("群聊创建失败", e);
        }
    }

    // 批量获取 groups
    public HttpResult<Object> fetchGroups(GroupsFetchDTO dto) {
        Long userId = dto.getUserId();
        System.out.println("fetchGroups: " + dto);
        try {
            Group[] groups = groupMapper.fetchGroups(userId);
            return HttpResult.success(0, "获取群聊成功", new GroupsFetchVO(groups));
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "获取群聊失败", null);
        }
    }

    // 获取一个group的所有members
    public HttpResult<Object> fetchGroupMembers(GroupMembersFetchDTO dto) {
        System.out.println("fetchGroupMembers: " + dto);
        Long groupId = dto.getGroupId();
        try {
            GroupMember[] groupMembers = groupMapper.fetchGroupMembers(groupId);
            return HttpResult.success(0, "获取群成员成功", new GroupMembersFetchVO(groupMembers));
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "获取群成员失败", null);
        }
    }

    @Transactional
    public HttpResult<Object> joinGroup(GroupJoinDTO dto) {
        Long inviterUserId = dto.getInviterUserId();
        Long[] inviteeUserIds = dto.getInviteeUserIds();
        Long groupId = dto.getGroupId();
        if (inviterUserId == null || inviteeUserIds == null || inviteeUserIds.length <= 0 || groupId == null) {
            return HttpResult.error(1, "字段不完整", null);
        }
        Integer len = inviteeUserIds.length;
        System.out.println("joinGroup: " + dto);
        try {
            Group group = groupMapper.getGroup(groupId);
            if (group == null || group.getStatus() == 1) {
                return HttpResult.error(1, "群聊不存在", null);
            }
            // 获取invitees的info
            User[] users = new User[len];
            GroupMember[] newGroupMembers = new GroupMember[len];
            for (int i = 0; i < len; i++) {
                Long inviteeUserId = inviteeUserIds[i];
                users[i] = cache.getUserOrLoad(inviteeUserIds[i], () -> {
                    return userMapper.getUserByUserId(inviteeUserId);
                });
                newGroupMembers[i] = new GroupMember(groupId, users[i].getUserId(), 0, users[i].getUsername(), false);
            }
            // 插入新成员
            int rows = groupMapper.addGroupMembers(newGroupMembers);
            if (rows <= 0) {
                throw new RuntimeException("更新群成员数失败");
            }
            // 更新群成员数量
            rows = groupMapper.updateGroupMemberCnt(groupId, len);
            if (rows <= 0) {
                throw new RuntimeException("更新群成员数失败");
            }
            group.setMemberCnt(group.getMemberCnt() + len);
            // 推送消息的内容
            SystemContent systemContent = new SystemContent(SystemContentType.MEMBERS_JOIN,
                    new MembersJoinContent(inviterUserId, inviteeUserIds));
            Message message = new Message(true, groupId, null, null,
                    6, JSON.toJSONString(systemContent),
                    null, false);
            Conversation[] conversations = new Conversation[len];

            for (int i = 0; i < len; i++) {
                conversations[i] = new Conversation(inviteeUserIds[i], groupId,
                        true, null,
                        groupId, null, 0);
            }

            // 插入系统消息
            messageMapper.insertMessage(message);
            // 更新cache
            cache.groupMembersJoin(groupId, inviteeUserIds);
            MessageVO messageVO = new MessageVO(message.getMsgId(), String.valueOf(groupId),
                    true, null, null, message.getMsgType(),
                    systemContent, null, false, message.getCreatedTime());

            // 为invitees创建新会话
            conversationMapper.createNewConversations(conversations);
            // 更新所有conv的相关信息
            conversations[0].setLastMsgId(message.getMsgId());
            conversations[0].setLastMsgBrief("新成员加入");
            conversations[0].setLastMsgTime(message.getCreatedTime());
            conversations[0].setLastMsgSenderId(null);

            Long[] groupMemberUserIds = cache.getGroupMembersOrLoad(groupId, () -> {
                GroupMember[] groupMembers = groupMapper.fetchGroupMembers(groupId);
                return Arrays.stream(groupMembers).map(GroupMember::getMemberUserId)
                        .toArray(Long[]::new);
            });

            // 更新所有会话
            conversationMapper.updateConversations(conversations[0], groupMemberUserIds);
            Conversation[] convs = conversationMapper.fetchConversationsByUserIdsAndSessionId(groupMemberUserIds,
                    groupId);

            for (Conversation c : convs) {
                if (sessionManager.isOnline(c.getUserId())) {
                    Channel channel = sessionManager.getChannel(c.getUserId());
                    // 推送新成员加入消息
                    WsSender.send(channel,
                            new WsResult(WsType.GROUP, GroupEvent.JOIN, 0,
                                    "新群友加入", null,
                                    new MembersJoinPush(group, newGroupMembers)));
                    // 推送群聊系统消息
                    WsSender.send(channel,
                            new WsResult(WsType.CHAT, ChatEvent.RECEIVE, 0, "新成员加入", null,
                                    new SendPush(messageVO, c)));
                }
            }

            return HttpResult.success(0, "邀请好友成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("邀请好友加入群聊失败", e);
        }

    }

    @Transactional
    public HttpResult<Object> quitGroup(GroupQuitDTO dto) {
        Long[] userIds = dto.getUserIds();
        Long groupId = dto.getGroupId();
        System.out.println("quitGroup: " + dto);
        if (groupId == null) {
            return HttpResult.error(1, "群聊不存在", null);
        }
        try {
            Group group = groupMapper.getGroup(groupId);
            if (group == null || group.getStatus() == 1) {
                return HttpResult.error(1, "群聊不存在", null);
            }
            // 删除groupMember
            int rows = groupMapper.quitGroup(userIds, groupId);
            if (rows <= 0) {
                throw new RuntimeException("退出群聊失败");
            }
            // 更新群成员数量
            rows = groupMapper.updateGroupMemberCnt(groupId, userIds.length);
            if (rows <= 0) {
                throw new RuntimeException("更新群成员数失败");
            }
            // 获取群成员
            GroupMember[] groupMembers = groupMapper.fetchGroupMembers(groupId);
            cache.groupMembersQuit(groupId, userIds);
            if (groupMembers.length == 0) {
                group.setMemberCnt(0);
                group.setStatus(1);
                groupMapper.updateGroup(group);
                cache.delete(groupId);
            } else {
                // 如果是群主退出，则转让群主位置
                if (Arrays.stream(userIds).anyMatch(id -> id.equals(group.getOwnerUserId()))) {
                    group.setOwnerUserId(groupMembers[0].getMemberUserId());
                    groupMapper.updateGroup(group);
                    groupMembers[0].setRole(2);
                    groupMapper.updateGroupMembers(new GroupMember[] { groupMembers[0] });
                }
                for (GroupMember member : groupMembers) {
                    if (sessionManager.isOnline(member.getMemberUserId())) {
                        WsSender.send(sessionManager.getChannel(member.getMemberUserId()),
                                new WsResult(WsType.GROUP,
                                        GroupEvent.QUIT, 0, "成员退出", null,
                                        new MembersQuitPush(groupId, userIds)));
                    }
                }
            }
            return HttpResult.success(0, "群聊退出成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("群聊退出失败", e);
        }
    }

    // 解散群聊
    public HttpResult<Object> disbandGroup(GroupDisbandDTO dto) {
        Long groupId = dto.getGroupId();
        Long ownerUserId = dto.getOwnerUserId();
        System.out.println("disbandGroup: " + dto);
        if (groupId == null) {
            return HttpResult.error(1, "群聊不存在", null);
        }
        try {
            Group group = groupMapper.getGroup(groupId);
            if (group == null || group.getStatus() == 1) {
                return HttpResult.error(1, "群聊不存在", null);
            }
            if (!(group.getOwnerUserId().equals(ownerUserId))) {
                return HttpResult.error(1, "只有群主有权限解散群聊", null);
            }
            group.setStatus(1);
            // 更新群聊解散状态
            groupMapper.updateGroup(group);
            Long[] groupMemberUserIds = cache.getGroupMembersOrLoad(groupId, () -> {
                GroupMember[] groupMembers = groupMapper.fetchGroupMembers(groupId);
                return Arrays.stream(groupMembers).map(GroupMember::getMemberUserId)
                        .toArray(Long[]::new);
            });
            SystemContent systemContent = new SystemContent(SystemContentType.GROUP_DISBAND, null);
            // 插入系统消息
            Message message = new Message(true, groupId, null, null, 6, JSON.toJSONString(systemContent), null, false);
            messageMapper.insertMessage(message);
            // 批量更新会话
            Conversation conv = new Conversation();
            conv.setSessionId(groupId);
            conv.setLastMsgBrief("群聊已解散");
            conv.setLastMsgId(message.getMsgId());
            conv.setLastMsgSenderId(null);
            conv.setLastMsgTime(message.getCreatedTime());
            conversationMapper.updateConversations(conv, groupMemberUserIds);
            Conversation[] convs = conversationMapper.fetchConversationsByUserIdsAndSessionId(groupMemberUserIds,
                    groupId);
            MessageVO messageVO = new MessageVO(message.getMsgId(), groupId, true, null, null,
                    message.getMsgType(), systemContent, null, false, message.getCreatedTime());
            for (Conversation c : convs) {
                if (sessionManager.isOnline(c.getUserId())) {
                    Channel channel = sessionManager.getChannel(c.getUserId());
                    // 推送群聊解散消息
                    WsSender.send(channel,
                            new WsResult(WsType.GROUP, GroupEvent.DISBAND, 0, "群聊已解散", null,
                                    new GroupDisbandPush(groupId)));
                    // 推送群聊系统消息
                    WsSender.send(channel,
                            new WsResult(WsType.CHAT, ChatEvent.RECEIVE, 0, "群聊已解散", null,
                                    new SendPush(messageVO, c)));
                }
            }
            cache.delete(groupId);
            return HttpResult.success(0, "解散群聊成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(2, "解散群聊失败", null);
        }
    }

    // 改变 member:role
    @Transactional
    public HttpResult<Object> setGroupMembersRole(GroupMembersRoleSetDTO dto) {
        Long ownerUserId = dto.getOwnerUserId();
        GroupMember[] groupMembers = dto.getGroupMembers();
        System.out.println("setGroupMembersRole: " + dto);
        try {
            if (groupMembers.length <= 0) {
                return HttpResult.success(0, "管理员更新成功", null);
            }
            if (groupMembers[0].getGroupId() == null) {
                return HttpResult.error(1, "群聊不存在", null);
            }
            Group group = groupMapper.getGroup(groupMembers[0].getGroupId());
            if (group == null || group.getStatus() == 1) {
                return HttpResult.error(1, "群聊不存在", null);
            }
            // 检查是否是群主操作
            if (group.getOwnerUserId() != ownerUserId) {
                return HttpResult.error(1, "只有群主有权限", null);
            }
            groupMapper.updateGroupMembers(groupMembers);

            Long[] groupMemberUserIds = cache.getGroupMembersOrLoad(group.getGroupId(), () -> {
                GroupMember[] gms = groupMapper.fetchGroupMembers(group.getGroupId());
                return Arrays.stream(gms).map(GroupMember::getMemberUserId)
                        .toArray(Long[]::new);
            });
            for (Long memberUserId : groupMemberUserIds) {
                if (sessionManager.isOnline(memberUserId)) {
                    Channel channel = sessionManager.getChannel(memberUserId);
                    WsSender.send(channel,
                            new WsResult(WsType.GROUP, GroupEvent.MEMBERS_UPDATE, 0, "管理员设置操作", null,
                                    new GroupMembersUpdatePush(groupMembers)));
                }
            }
            return HttpResult.success(0, "管理员更新成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "管理员更新失败", null);
        }
    }

    // 全员禁言
    @Transactional
    public HttpResult<Object> setGroupSilenceAll(GroupUpdateDTO dto) {
        System.out.println("setGRoupSilenceAll: " + dto);
        Long groupId = dto.getGroup().getGroupId();
        Long updaterUserId = dto.getUpdaterUserId();
        if (groupId == null) {
            return HttpResult.error(1, "群聊不存在", null);
        }

        try {
            Group group = groupMapper.getGroup(groupId);
            if (group == null || group.getStatus() == 1) {
                return HttpResult.error(1, "群聊不存在", null);
            }
            GroupMember groupMember = groupMapper.getGroupMember(groupId, updaterUserId);
            if (groupMember.getRole() == 0) {
                return HttpResult.error(1, "只有群主和管理员有权限禁言", null);
            }
            group.setGroupId(groupId);
            group.setStatus(2);
            groupMapper.updateGroup(group);
            group = groupMapper.getGroup(groupId);
            SystemContent systemContent = new SystemContent(SystemContentType.SILENCE_ALL,
                    new SilenceAllContent(updaterUserId));
            Message message = new Message(true, groupId, null,
                    null, 6, JSON.toJSONString(systemContent),
                    null, false);
            messageMapper.insertMessage(message);
            Conversation conv = new Conversation();
            conv.setSessionId(groupId);
            conv.setLastMsgId(message.getMsgId());
            conv.setLastMsgSenderId(null);
            conv.setLastMsgTime(message.getCreatedTime());
            conv.setLastMsgBrief("全员禁言");
            Long[] groupMemberUserIds = cache.getGroupMembersOrLoad(groupId, () -> {
                GroupMember[] groupMembers = groupMapper.fetchGroupMembers(groupId);
                return Arrays.stream(groupMembers).map(GroupMember::getMemberUserId)
                        .toArray(Long[]::new);
            });
            conversationMapper.updateConversations(conv, groupMemberUserIds);
            Conversation[] convs = conversationMapper.fetchConversationsByUserIdsAndSessionId(groupMemberUserIds,
                    groupId);
            MessageVO messageVO = new MessageVO(message.getMsgId(), groupId, true, null, null, 6, systemContent,
                    null, false, message.getCreatedTime());
            for (Conversation c : convs) {
                if (sessionManager.isOnline(c.getUserId())) {
                    Channel channel = sessionManager.getChannel(c.getUserId());
                    // 推送系统群聊全员禁言消息
                    WsSender.send(channel,
                            new WsResult(WsType.CHAT, ChatEvent.RECEIVE, 0, "全员禁言", null,
                                    new SendPush(messageVO, c)));
                    // 推送群聊更新事件
                    WsSender.send(channel,
                            new WsResult(WsType.GROUP, GroupEvent.GROUP_UPDATE, 0, "群聊更新", null,
                                    new GroupUpdatePush(group)));
                }
            }
            return HttpResult.success(0, "全员禁言成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "全员禁言失败", null);
        }
    }

    // 解除全员禁言
    public HttpResult<Object> setGroupUnSilenceAll(GroupUpdateDTO dto) {
        System.out.println("setGroupUnSilenceAll: " + dto);
        Long groupId = dto.getGroup().getGroupId();
        Long updaterUserId = dto.getUpdaterUserId();
        if (groupId == null) {
            return HttpResult.error(1, "群聊不存在", null);
        }

        try {
            Group group = groupMapper.getGroup(groupId);
            if (group == null || group.getStatus() == 1) {
                return HttpResult.error(1, "群聊不存在", null);
            }
            GroupMember groupMember = groupMapper.getGroupMember(groupId, updaterUserId);
            if (groupMember.getRole() == 0) {
                return HttpResult.error(1, "只有群主和管理员有权限解除全员禁言", null);
            }
            group.setGroupId(groupId);
            group.setStatus(0);
            groupMapper.updateGroup(group);
            group = groupMapper.getGroup(groupId);
            SystemContent systemContent = new SystemContent(SystemContentType.UNSILENCE_ALL,
                    new UnSilenceAllContent(updaterUserId));
            Message message = new Message(true, groupId, null,
                    null, 6, JSON.toJSONString(systemContent),
                    null, false);
            messageMapper.insertMessage(message);
            Conversation conv = new Conversation();
            conv.setSessionId(groupId);
            conv.setLastMsgId(message.getMsgId());
            conv.setLastMsgSenderId(null);
            conv.setLastMsgTime(message.getCreatedTime());
            conv.setLastMsgBrief("解除全员禁言");
            Long[] groupMemberUserIds = cache.getGroupMembersOrLoad(groupId, () -> {
                GroupMember[] groupMembers = groupMapper.fetchGroupMembers(groupId);
                return Arrays.stream(groupMembers).map(GroupMember::getMemberUserId)
                        .toArray(Long[]::new);
            });
            conversationMapper.updateConversations(conv, groupMemberUserIds);
            Conversation[] convs = conversationMapper.fetchConversationsByUserIdsAndSessionId(groupMemberUserIds,
                    groupId);
            MessageVO messageVO = new MessageVO(message.getMsgId(), groupId, true, null, null, 6, systemContent,
                    null, false, message.getCreatedTime());
            for (Conversation c : convs) {
                if (sessionManager.isOnline(c.getUserId())) {
                    Channel channel = sessionManager.getChannel(c.getUserId());
                    // 推送消息
                    WsSender.send(channel,
                            new WsResult(WsType.CHAT, ChatEvent.RECEIVE, 0, "解除全员禁言", null,
                                    new SendPush(messageVO, c)));
                    // 推送group更新事件
                    WsSender.send(channel,
                            new WsResult(WsType.GROUP, GroupEvent.GROUP_UPDATE, 0, "群聊更新", null,
                                    new GroupUpdatePush(group)));
                }
            }
            return HttpResult.success(0, "全员禁言解除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "解除全员禁言失败", null);
        }
    }

    // update: group
    @Transactional
    public HttpResult<Object> updateGroup(GroupUpdateDTO dto) {
        if (dto.getGroup() == null) {
            return HttpResult.error(1, "群聊不存在", null);
        }
        Long groupId = dto.getGroup().getGroupId();
        String groupName = dto.getGroup().getGroupName();
        String groupAvatar = dto.getGroup().getGroupAvatar();
        Long ownerUserId = dto.getGroup().getOwnerUserId();
        String announcement = dto.getGroup().getAnnouncement();
        Long updaterUserId = dto.getUpdaterUserId();
        System.out.println("updateGroup: " + dto);
        if (groupId == null) {
            return HttpResult.error(1, "群聊不存在", null);
        }
        try {
            Group group = groupMapper.getGroup(groupId);
            if (group == null || group.getStatus() == 1) {
                return HttpResult.error(1, "群聊不存在", null);
            }
            group.setGroupId(groupId);
            group.setGroupName(groupName);
            group.setGroupAvatar(groupAvatar);
            group.setOwnerUserId(ownerUserId);
            group.setAnnouncement(announcement);
            // 更新群聊信息
            int rows = groupMapper.updateGroup(group);
            if (rows <= 0) {
                return HttpResult.error(1, "群聊更新失败", null);
            }
            Long[] groupMemberUserIds = cache.getGroupMembersOrLoad(groupId, () -> {
                GroupMember[] groupMembers = groupMapper.fetchGroupMembers(groupId);
                return Arrays.stream(groupMembers).map(GroupMember::getMemberUserId)
                        .toArray(Long[]::new);
            });
            if (announcement != null) {
                SystemContent systemContent = new SystemContent(SystemContentType.ANNOUNCEMENT_POST,
                        new AnnouncementPostContent(updaterUserId, announcement));
                Message message = new Message(true, group.getGroupId(), null, null, 6,
                        JSON.toJSONString(systemContent), null, false);
                messageMapper.insertMessage(message);
                Conversation conv = new Conversation();
                conv.setSessionId(groupId);
                conv.setLastMsgId(message.getMsgId());
                conv.setLastMsgSenderId(null);
                conv.setLastMsgTime(message.getCreatedTime());
                conv.setLastMsgBrief("发布了新公告");
                conversationMapper.updateConversations(conv, groupMemberUserIds);
                Conversation[] convs = conversationMapper.fetchConversationsByUserIdsAndSessionId(groupMemberUserIds,
                        groupId);
                MessageVO messageVO = new MessageVO(message.getMsgId(), String.valueOf(groupId), true, null,
                        null, message.getMsgType(), systemContent,
                        null, false, message.getCreatedTime());
                for (Conversation c : convs) {
                    if (sessionManager.isOnline(c.getUserId())) {
                        Channel channel = sessionManager.getChannel(c.getUserId());
                        WsSender.send(channel, new WsResult(WsType.CHAT, ChatEvent.RECEIVE, 0, "新公告发布", null,
                                new SendPush(messageVO, c)));
                    }
                }
            }
            // 推送给群成员
            for (Long memberUserId : groupMemberUserIds) {
                if (sessionManager.isOnline(memberUserId)) {
                    Channel channel = sessionManager.getChannel(memberUserId);
                    WsSender.send(channel, new WsResult(
                            WsType.GROUP, GroupEvent.GROUP_UPDATE, 0, "群聊更新", null,
                            new GroupUpdatePush(group)));
                }
            }
            return HttpResult.success(0, "群聊更新成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "群聊更新失败", null);
        }
    }

    // 批量update :groupMembers
    @Transactional
    public HttpResult<Object> updateGroupMembers(GroupMembersUpdateDTO dto) {
        System.out.println("updateGroupMembers: " + dto);
        try {
            GroupMember[] groupMembers = dto.getGroupMembers();
            if (groupMembers.length == 0) {
                return HttpResult.error(1, "更新失败", null);
            }
            Long groupId = groupMembers[0].getGroupId();
            if (groupId == null) {
                return HttpResult.error(1, "群ID不能为空", null);
            }
            Group group = groupMapper.getGroup(groupId);
            if (group == null || group.getStatus() == 1) {
                return HttpResult.error(1, "群聊不存在", null);
            }
            int rows = groupMapper.updateGroupMembers(groupMembers);
            if (rows <= 0) {
                throw new RuntimeException("更新群成员失败, memberUserId=" + groupMembers);
            }

            Long[] groupMemberUserIds = cache.getGroupMembersOrLoad(groupId, () -> {
                GroupMember[] gms = groupMapper.fetchGroupMembers(groupId);
                return Arrays.stream(gms).map(GroupMember::getMemberUserId)
                        .toArray(Long[]::new);
            });

            GroupMembersUpdatePush push = new GroupMembersUpdatePush(groupMembers);

            for (Long memberUserId : groupMemberUserIds) {
                if (sessionManager.isOnline(memberUserId)) {
                    Channel channel = sessionManager.getChannel(memberUserId);
                    WsSender.send(channel,
                            new WsResult<>(WsType.GROUP, GroupEvent.MEMBERS_UPDATE,
                                    0, "群成员信息更新成功", null,
                                    push));

                }
            }

            return HttpResult.success(0, "信息更新成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("信息更新失败", e);
        }
    }

    // 转让群主
    @Transactional
    public HttpResult<Object> transferGroupOwner(GroupOwnerTransferDTO dto) {
        System.out.println("transferGroupOwner: " + dto);
        Long groupId = dto.getGroupId();
        Long ownerUserId = dto.getOwnerUserId();
        Long transfereeUserId = dto.getTransfereeUserId();
        if (groupId == null) {
            return HttpResult.error(1, "群聊不存在", null);
        }
        try {
            Group group = groupMapper.getGroup(groupId);
            if (group == null || group.getStatus() == 1) {
                return HttpResult.error(1, "群聊不存在", null);
            }
            if (!Objects.equals(group.getGroupId(), groupId)
                    || !Objects.equals(group.getOwnerUserId(), ownerUserId)) {
                return HttpResult.error(1, "转让失败", null);
            }
            Long[] groupMemberUserIds = cache.getGroupMembersOrLoad(groupId, () -> {
                GroupMember[] gms = groupMapper.fetchGroupMembers(groupId);
                return Arrays.stream(gms).map(GroupMember::getMemberUserId)
                        .toArray(Long[]::new);
            });
            boolean exists = Arrays.stream(groupMemberUserIds).anyMatch(gm -> gm.equals(transfereeUserId));
            if (Boolean.FALSE.equals(exists)) {
                return HttpResult.error(1, "被转让者不在群聊里", null);
            }
            GroupMember lastOwnerMember = new GroupMember(groupId, ownerUserId, 1);
            GroupMember newOwnerMember = new GroupMember(groupId, transfereeUserId, 2);
            groupMapper.updateGroupMembers(new GroupMember[] { lastOwnerMember });
            groupMapper.updateGroupMembers(new GroupMember[] { newOwnerMember });
            group.setOwnerUserId(transfereeUserId);
            groupMapper.updateGroup(group);
            GroupMembersUpdatePush membersUpdatePush = new GroupMembersUpdatePush(
                    new GroupMember[] { lastOwnerMember, newOwnerMember });
            GroupUpdatePush groupUpdatePush = new GroupUpdatePush(group);
            for (Long memberUserId : groupMemberUserIds) {
                Channel channel = sessionManager.getChannel(memberUserId);
                // 群成员更新推送
                WsSender.send(channel,
                        new WsResult(WsType.GROUP, GroupEvent.MEMBERS_UPDATE, 0, "群成员信息更新", null,
                                membersUpdatePush));
                // 群聊更新推送
                WsSender.send(channel,
                        new WsResult(WsType.GROUP, GroupEvent.GROUP_UPDATE, 0, "群成员信息更新", null,
                                groupUpdatePush));
            }
            return HttpResult.success(0, "转让成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("转让失败");
        }
    }

}
