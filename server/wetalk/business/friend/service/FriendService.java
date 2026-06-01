package com.wetalk.business.friend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestPart;
import com.fasterxml.jackson.core.type.TypeReference;
import com.alibaba.fastjson2.JSON;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.*;

import com.wetalk.model.*;
import com.wetalk.utils.*;
import com.wetalk.mapper.*;
import com.wetalk.netty.session.SessionManager;
import com.wetalk.business.friend.dto.*;
import com.wetalk.vo.HttpResult;
import com.wetalk.business.friend.vo.*;
import com.wetalk.ws.Type.Friend.RequestAccept.*;
import com.wetalk.ws.Type.Friend.RequestSend.*;
import com.wetalk.ws.protocol.WsType;
import com.wetalk.ws.protocol.event.*;
import com.wetalk.ws.protocol.Friend.RequestStatus;
import com.wetalk.ws.Type.Chat.Content.*;
import com.wetalk.ws.protocol.WsResult;

import com.wetalk.mq.protocol.NotifyPush;
import com.wetalk.mq.protocol.NotifyPushUnit;
import com.wetalk.mq.producer.NotifyPushProducer;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class FriendService {
    private final NotifyPushProducer notifyPushProducer;
    private final FriendMapper friendMapper;
    private final UserMapper userMapper;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final SessionManager sessionManager;
    private final IdGenerator idGenerator;
    private final JwtUtil jwtUtil;

    // searchFriends
    public HttpResult<SearchFriendsVO> searchFriends(SearchFriendsDTO dto) {
        System.out.println("收到 searchFriends: " + dto);

        String searchQuery = dto.getSearchQuery();
        Long userId = dto.getUserId();

        try {
            User[] data = userMapper.searchFriends(searchQuery.trim(), userId);
            System.out.println("data " + java.util.Arrays.toString(data));
            return HttpResult.success(0, "查询成功", new SearchFriendsVO(data));
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.success(1, "查询失败", null);
        }
    }

    // fetchFriendRequests
    public HttpResult<FriendRequestVO> fetchFriendRequests(FriendRequestGetDTO dto) {
        System.out.println("fetchFriendRequests: " + dto);

        Long userId = dto.getUserId();
        try {
            FriendRequest[] friendRequestsSend = friendMapper.fetchFriendRequestsSend(userId);
            FriendRequest[] friendRequestsReceive = friendMapper.fetchFriendRequestsReceive(userId);
            List<FriendRequest> friendRequests = new ArrayList<>();
            friendRequests.addAll(Arrays.asList(friendRequestsSend));
            friendRequests.addAll(Arrays.asList(friendRequestsReceive));
            friendRequests.sort(Comparator.comparing(FriendRequest::getCreatedTime));
            return HttpResult.success(0, "查询成功", new FriendRequestVO(friendRequests.toArray(new FriendRequest[0])));
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "查询错误", null);
        }
    }

    // sendAddFriendRequest
    @Transactional(rollbackFor = Exception.class)
    public HttpResult<Object> sendFriendRequest(FriendRequestSendDTO dto) {
        System.out.println("sendFriendRequest: " + dto);
        Long requesterUserId = dto.getRequesterUserId();
        Long requesteeUserId = dto.getRequesteeUserId();
        String requestMsg = dto.getRequestMsg();
        String remark = dto.getRemark();
        Boolean hideMyMoments = dto.getHideMyMoments();
        Boolean hideFriendMoments = dto.getHideFriendMoments();
        try {
            FriendRequest existsFriendRequest = friendMapper.getFriendRequestByUserId(requesterUserId, requesteeUserId);
            FriendRequest existsPeerFriendRequest = friendMapper.getFriendRequestByUserId(requesteeUserId,
                    requesterUserId);
            Friendship requesterFriendship = friendMapper.getFriendship(requesterUserId, requesteeUserId);
            Friendship requesteeFriendship = friendMapper.getFriendship(requesteeUserId, requesterUserId);

            // 双方仍是好友
            if (requesterFriendship != null && requesteeFriendship != null
                    && Boolean.FALSE.equals(requesteeFriendship.getDeleted())
                    && Boolean.FALSE.equals(requesterFriendship.getDeleted())
                    && Boolean.FALSE.equals(requesterFriendship.getBlocked())
                    && Boolean.FALSE.equals(requesteeFriendship.getBlocked())) {
                return HttpResult.error(1, "对方已经是你的好友", null);
            }

            // 如果拉黑了对方, 则解除拉黑
            if (requesterFriendship != null && Boolean.TRUE.equals(requesterFriendship.getBlocked())) {
                friendMapper.unBlockFriend(requesterUserId, requesteeUserId);
                requesterFriendship.setBlocked(false);
            }

            // 已被对方拉黑
            if (requesteeFriendship != null && Boolean.TRUE.equals(requesteeFriendship.getBlocked())) {
                return HttpResult.error(1, "你已被对方拉黑", null);
            }

            // 已经发过好友申请
            if (existsFriendRequest != null) {
                return HttpResult.error(1, "已经发送过申请", null);
            }

            // 对方也已申请你为好友
            if (existsPeerFriendRequest != null) {
                acceptFriendRequest(
                        new FriendRequestAcceptDTO(
                                existsPeerFriendRequest.getFriendRequestId(),
                                existsPeerFriendRequest.getRemark(),
                                existsPeerFriendRequest.getHideMyMoments(),
                                existsPeerFriendRequest.getHideFriendMoments()));
                return HttpResult.success(0, "添加成功", null);
            }

            Integer status = RequestStatus.UNREAD;

            // 已删除对方但未被对方删除 -> 直接接受
            if (requesterFriendship != null && Boolean.FALSE.equals(requesteeFriendship.getDeleted())) {
                status = RequestStatus.ACCEPT;
            }

            FriendRequest friendRequest = new FriendRequest(
                    requesterUserId,
                    requesteeUserId,
                    requestMsg,
                    remark,
                    status,
                    hideMyMoments,
                    hideFriendMoments,
                    LocalDateTime.now());

            int rows = friendMapper.insertFriendRequest(friendRequest);
            if (rows <= 0) {
                return HttpResult.error(1, "申请发送失败", null);
            }

            Long friendRequestId = friendRequest.getFriendRequestId();

            // 已删除对方但未被对方删除
            if (requesterFriendship != null && Boolean.FALSE.equals(requesteeFriendship.getDeleted())) {
                // 重建关系
                rebuildFriendship(requesterUserId, requesteeUserId);
                requesterFriendship.setDeleted(false);
                // 找回conv
                Conversation requesterConv = conversationMapper.getConversationByUserIdAndPeerId(requesterUserId,
                        requesteeUserId);
                requesterConv.setDeleted(false);
                // 更新deleted = 0
                conversationMapper.updateConversation(requesterConv);
                friendMapper.acceptFriendRequest(friendRequestId);
                if (sessionManager.isOnline(requesterUserId)) {
                    List<NotifyPushUnit> notifyPushUnits = List.of(new NotifyPushUnit(requesterUserId,
                            new WsResult<>(WsType.FRIEND, FriendEvent.REQUEST_ACCEPT, 0, "添加成功", null,
                                    new RequestAcceptResp(requesterFriendship, requesterConv, friendRequestId))));
                    notifyPushProducer.pushNotification(new NotifyPush(notifyPushUnits));
                }
                return HttpResult.success(0, "申请发送成功", friendRequestId);
            }

            if (sessionManager.isOnline(requesteeUserId)) {
                Channel requesteeChannel = sessionManager.getChannel(requesteeUserId);
                if (requesteeChannel != null) {
                    User requester = userMapper.getUserByUserId(requesterUserId);
                    if (requester != null) {
                        requester.setPassword("");
                        List<NotifyPushUnit> notifyPushUnits = List.of(new NotifyPushUnit(requesteeUserId,
                                new WsResult<>(
                                        WsType.FRIEND, FriendEvent.REQUEST_SEND,
                                        0, "收到好友申请",
                                        null, new RequestSendResp(friendRequestId, requester.getUserId(),
                                                requester.getUserId(), requester.getAccountId(),
                                                requester.getUsername(), requester.getAvatar(),
                                                friendRequest.getRequestMsg(), friendRequest.getStatus()))));
                        notifyPushProducer.pushNotification(new NotifyPush(notifyPushUnits));
                    }
                }
            }

            return HttpResult.success(0, "申请发送成功", friendRequestId);

        } catch (Exception e) {
            throw new RuntimeException("好友添加申请发送失败", e);
        }
    }

    // 已读好友申请
    public HttpResult<Object> readFriendRequest(FriendRequestReadDTO dto) {
        System.out.println("receive readFriendRequest " + dto);
        Long friendRequestId = dto.getFriendRequestId();
        try {
            int rows = friendMapper.readFriendRequest(friendRequestId);
            if (rows > 0) {
                return HttpResult.success(0, "已读成功", null);
            } else {
                System.out.println("readFriendRequest: 数据更新失败");
                return HttpResult.error(1, "已读失败", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "已读失败", null);
        }
    }

    // 接收好友申请
    @Transactional
    public void acceptFriendRequest(FriendRequestAcceptDTO dto) {
        System.out.println("acceptFriendRequest: " + dto);
        Long friendRequestId = dto.getFriendRequestId();
        String requesteeRemark = dto.getRemark();
        Boolean requesteeHideMyMoments = dto.getHideMyMoments();
        Boolean requesteeHideFriendMoments = dto.getHideFriendMoments();
        Long requesterUserId = null, requesteeUserId = null;
        try {
            FriendRequest friendRequest = friendMapper.getFriendRequestById(friendRequestId);
            if (friendRequest == null) {
                if (sessionManager.isOnline(requesteeUserId)) {
                    List<NotifyPushUnit> notifyPushUnits = List.of(new NotifyPushUnit(requesteeUserId,
                            new WsResult(WsType.SYSTEM, SystemEvent.ERROR, 1, "好友申请不存在", null, null)));
                    notifyPushProducer.pushNotification(new NotifyPush(notifyPushUnits));
                }
                throw new RuntimeException("好友申请不存在");
            }
            // 更新friendRequest
            int rows = friendMapper.acceptFriendRequest(friendRequestId);
            requesterUserId = friendRequest.getRequesterUserId();
            requesteeUserId = friendRequest.getRequesteeUserId();
            String requesterRemark = friendRequest.getRemark();
            Boolean requesterHideMyMoments = friendRequest.getHideMyMoments();
            if (rows <= 0) {
                System.out.println("acceptFriendRequest: failed");
                if (sessionManager.isOnline(requesteeUserId)) {
                    List<NotifyPushUnit> notifyPushUnits = List.of(new NotifyPushUnit(requesteeUserId,
                            new WsResult(WsType.SYSTEM, SystemEvent.ERROR, 1, "数据更新失败", null, null)));
                    notifyPushProducer.pushNotification(new NotifyPush(notifyPushUnits));
                }
            }
            Boolean requesterHideFriendMoments = friendRequest.getHideFriendMoments();
            Friendship requesterFriendship = new Friendship(requesterUserId, requesteeUserId, requesterRemark,
                    requesterHideMyMoments, requesterHideFriendMoments, false, false,
                    LocalDateTime.now());
            // 添加requester->requestee的friendship
            rows = friendMapper.buildFriendship(requesterFriendship);
            if (rows <= 0) {
                System.out.println("acceptFriendRequest: failed");
                if (sessionManager.isOnline(requesteeUserId)) {
                    List<NotifyPushUnit> notifyPushUnits = List.of(new NotifyPushUnit(requesteeUserId,
                            new WsResult(WsType.SYSTEM, SystemEvent.ERROR, 1, "数据更新失败", null, null)));
                    notifyPushProducer.pushNotification(new NotifyPush(notifyPushUnits));
                }
            }
            // 处理requestee->requester的friendship
            Friendship requesteeFriendship = new Friendship(requesteeUserId, requesterUserId, requesteeRemark,
                    requesteeHideMyMoments, requesteeHideFriendMoments, false, false, LocalDateTime.now());
            rows = friendMapper.buildFriendship(requesteeFriendship);
            if (rows <= 0) {
                System.out.println("acceptFriendRequest: failed");
                if (sessionManager.isOnline(requesteeUserId)) {
                    List<NotifyPushUnit> notifyPushUnits = List.of(new NotifyPushUnit(requesteeUserId,
                            new WsResult(WsType.SYSTEM, SystemEvent.ERROR, 1, "数据更新失败", null, null)));
                    notifyPushProducer.pushNotification(new NotifyPush(notifyPushUnits));
                }
            }
            Long sessionId = idGenerator.nextId();
            // 插入打招呼消息
            Message message = new Message(false, sessionId, requesterUserId, requesteeUserId, 1,
                    JSON.toJSONString(new TextContent(friendRequest.getRequestMsg())), null, false);
            message.setCreatedTime(LocalDateTime.now());
            rows = messageMapper.insertMessage(message);
            if (rows <= 0) {
                System.out.println("acceptFriendRequest: failed");
                if (sessionManager.isOnline(requesteeUserId)) {
                    List<NotifyPushUnit> notifyPushUnits = List.of(new NotifyPushUnit(requesteeUserId,
                            new WsResult(WsType.SYSTEM, SystemEvent.ERROR, 1, "数据更新失败", null, null)));
                    notifyPushProducer.pushNotification(new NotifyPush(notifyPushUnits));
                }
            }
            // 创建会话requester -> requestee
            Conversation requesterConv = conversationMapper.getConversationByUserIdAndPeerId(requesterUserId,
                    requesteeUserId);
            if (requesterConv == null) {
                requesterConv = new Conversation(requesterUserId, sessionId, false, requesteeUserId, null,
                        null, 0);
                requesterConv.setLastMsgTime(message.getCreatedTime());
                requesterConv.setLastMsgId(message.getMsgId());
                requesterConv.setLastMsgBrief(friendRequest.getRequestMsg());
                requesterConv.setLastMsgSenderId(requesterUserId);
                requesterConv.setDeleted(false);
            } else {
                requesterConv.setDeleted(false);
                conversationMapper.updateConversation(requesterConv);
            }
            Conversation requesteeConv = conversationMapper.getConversationByUserIdAndPeerId(requesteeUserId,
                    requesterUserId);
            // 创建会话requestee -> requester
            if (requesteeConv == null) {
                requesteeConv = new Conversation(requesteeUserId, sessionId, false, requesterUserId, null,
                        null, 1);
                requesteeConv.setLastMsgTime(message.getCreatedTime());
                requesteeConv.setLastMsgId(message.getMsgId());
                requesteeConv.setLastMsgBrief(friendRequest.getRequestMsg());
                requesteeConv.setLastMsgSenderId(requesterUserId);
                requesteeConv.setDeleted(false);
            } else {
                requesteeConv.setDeleted(false);
                conversationMapper.updateConversation(requesteeConv);
            }

            if (requesterConv.getConvId() == null || requesteeConv.getConvId() == null) {
                conversationMapper.createNewConversations(new Conversation[] { requesterConv, requesteeConv });
            }

            // 发送requester: socket
            if (sessionManager.isOnline(requesterUserId)) {
                List<NotifyPushUnit> notifyPushUnits = List.of(new NotifyPushUnit(requesterUserId,
                        new WsResult<>(WsType.FRIEND, FriendEvent.REQUEST_ACCEPT, 0, "添加成功", null,
                                new RequestAcceptResp(requesterFriendship, requesterConv, friendRequestId))));
                notifyPushProducer.pushNotification(new NotifyPush(notifyPushUnits));
            }
            // requestee: socket
            if (sessionManager.isOnline(requesteeUserId)) {
                List<NotifyPushUnit> notifyPushUnits = List.of(new NotifyPushUnit(requesteeUserId,
                        new WsResult(WsType.FRIEND, FriendEvent.REQUEST_ACCEPT, 0, "添加成功", null,
                                new RequestAcceptResp(requesteeFriendship, requesteeConv, friendRequestId))));
                notifyPushProducer.pushNotification(new NotifyPush(notifyPushUnits));
            }

        } catch (Exception e) {
            System.out.println("acceptFriendRequest: failed " + e);
            if (requesteeUserId != null && sessionManager.isOnline(requesteeUserId)) {
                List<NotifyPushUnit> notifyPushUnits = List.of(new NotifyPushUnit(requesteeUserId,
                        new WsResult(WsType.SYSTEM, SystemEvent.ERROR, 1, "数据更新失败", null, null)));
                notifyPushProducer.pushNotification(new NotifyPush(notifyPushUnits));
            }
            throw new RuntimeException(e);
        }
    }

    // 拉黑
    public HttpResult<Object> blockFriend(FriendBlockDTO dto) {
        System.out.println("blockFriend ");
        Long userId = dto.getUserId();
        Long friendUserId = dto.getFriendUserId();
        try {
            int rows = friendMapper.blockFriend(userId, friendUserId);
            if (rows > 0) {
                return HttpResult.success(0, "已拉黑", null);
            } else {
                return HttpResult.success(1, "拉黑失败", null);
            }
        } catch (Exception e) {
            System.out.println("blockFriend: " + e);
            return HttpResult.error(1, "操作失败", null);
        }
    }

    // 移出黑名单
    public HttpResult<Object> unBlockFriend(FriendUnBlockDTO dto) {
        System.out.println("unBlockFriend: " + dto);
        Long userId = dto.getUserId();
        Long friendUserId = dto.getFriendUserId();
        try {
            friendMapper.unBlockFriend(userId, friendUserId);
            return HttpResult.success(0, "移出黑名单成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "移出黑名单失败", null);
        }
    }

    // 删除好友
    public HttpResult<Object> deleteFriend(FriendDeleteDTO dto) {
        Long userId = dto.getUserId();
        Long friendUserId = dto.getFriendUserId();
        System.out.println("deletedFriend: " + dto);
        try {
            deleteFriend(userId, friendUserId);
            return HttpResult.error(0, "删除好友成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "删除好友失败", null);
        }
    }

    // 批量获取好友关系
    public HttpResult<Object> fetchFriendships(FriendshipsFetchDTO dto) {
        System.out.println("fetchFriendships: " + dto);
        Long userId = dto.getUserId();
        try {
            Friendship[] friendships = friendMapper.fetchFriendships(userId);
            System.out.println("friendshipsFetch: " + friendships);
            return HttpResult.success(0, "获取好友关系成功", new FriendshipsFetchVO(friendships));
        } catch (Exception e) {
            System.out.println("fetchFriendships: " + e);
            return HttpResult.error(1, "获取好友关系失败", null);
        }
    }

    // 获取特定好友关系
    public HttpResult<Object> getFriendship(FriendshipGetDTO dto) {
        Long userId = dto.getUserId();
        Long friendUserId = dto.getFriendUserId();
        System.out.println("getFriendship: " + dto);
        try {
            Friendship friendship = friendMapper.getFriendship(userId, friendUserId);
            if (friendship != null) {
                return HttpResult.success(0, "获取好友信息成功", new FriendshipGetVO(friendship));
            } else {
                return HttpResult.error(1, "获取好友信息失败", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "获取好友信息失败", null);
        }
    }

    // 更新好友关系信息
    public HttpResult<Object> updateFriendship(FriendshipUpdateDTO dto) {
        Friendship friendship = new Friendship(dto.getUserId(), dto.getFriendUserId(), dto.getRemark(),
                dto.getHideMyMoments(), dto.getHideFriendMoments(), null, null, null);
        System.out.println("updateFriendship: " + dto);
        try {
            int rows = friendMapper.updateFriendship(friendship);
            if (rows > 0) {
                return HttpResult.success(0, "更新好友关系成功", null);
            }
            return HttpResult.error(1, "更新好友关系失败", null);
        } catch (Exception e) {
            e.printStackTrace();
            return HttpResult.error(1, "更新好友关系失败", null);
        }
    }

    /* 工具接口方法 */

    // 标记删除好友
    private void deleteFriend(Long userId, Long friendUserId) {
        try {
            Friendship friendship = new Friendship();
            friendship.setUserId(userId);
            friendship.setFriendUserId(friendUserId);
            friendship.setDeleted(true);
            friendMapper.updateFriendship(friendship);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 标记重建好友
    private void rebuildFriendship(Long userId, Long friendUserId) {
        try {
            Friendship friendship = new Friendship();
            friendship.setUserId(userId);
            friendship.setFriendUserId(friendUserId);
            friendship.setDeleted(false);
            friendMapper.updateFriendship(friendship);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
